package com.voxelcraft.game;

import com.voxelcraft.control.FirstPersonController;
import com.voxelcraft.entities.MobManager;
import com.voxelcraft.inventory.Inventory;
import com.voxelcraft.mode.GameMode;
import com.voxelcraft.net.NetworkMessage;
import com.voxelcraft.net.VoxelClient;
import com.voxelcraft.net.VoxelServer;
import com.voxelcraft.render.VoxelMeshBuilder;
import com.voxelcraft.render.VoxelMeshBuilder.ChunkMesh;
import com.voxelcraft.render.VoxelMeshBuilder.MeshData;
import com.voxelcraft.save.WorldSave;
import com.voxelcraft.save.WorldStorage;
import com.voxelcraft.world.ChunkGenerator;
import com.voxelcraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameSession {
    public final GameMode mode;
    public final World world;
    public final FirstPersonController controller;
    public final Inventory inventory;
    public final MobManager mobManager;
    public final Map<String, ChunkMesh> chunkMeshes = new HashMap<>();
    public final String worldName;
    private VoxelServer lanServer;
    private VoxelClient lanClient;
    private final ExecutorService meshExecutor = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    private final Map<String, CompletableFuture<MeshBuildResult>> pendingMeshes = new ConcurrentHashMap<>();
    private final Map<String, Integer> meshVersions = new HashMap<>();
    private final Set<String> dirtyChunks = new HashSet<>();

    private int lastMeshCx = Integer.MIN_VALUE;
    private int lastMeshCz = Integer.MIN_VALUE;
    private double autosaveTimer = 0;

    public GameSession(GameMode mode) {
        this(defaultWorldName(mode), mode);
    }

    public GameSession(String worldName, GameMode mode) {
        WorldSave save = WorldStorage.load(worldName);
        this.worldName = worldName;
        this.mode = mode;
        world = save != null ? new World(save) : new World(mode);
        inventory = new Inventory();
        mobManager = new MobManager();

        double ground = world.getSupportHeight(world.getSpawnX(), world.getSpawnZ(), world.getSpawnGroundY());
        controller = new FirstPersonController(world, world.getSpawnX(), ground, world.getSpawnZ());
        if (save != null) {
            inventory.restore(save.inventory, save.selectedSlot);
            controller.x = save.playerX;
            controller.y = save.playerY;
            controller.z = save.playerZ;
            controller.yaw = save.yaw;
            controller.pitch = save.pitch;
        }
        controller.isLocked = false;
        syncChunkEvents();
    }

    private GameSession(String worldName, WorldSave save, VoxelClient client, VoxelServer server) {
        this.worldName = worldName;
        this.mode = save.mode;
        this.lanClient = client;
        this.lanServer = server;
        world = new World(save);
        inventory = new Inventory();
        mobManager = new MobManager();
        double ground = world.getSupportHeight(world.getSpawnX(), world.getSpawnZ(), world.getSpawnGroundY());
        controller = new FirstPersonController(world, world.getSpawnX(), ground, world.getSpawnZ());
        controller.isLocked = false;
        syncChunkEvents();
    }

    public static GameSession hostLan() {
        VoxelServer server = new VoxelServer(VoxelServer.DEFAULT_PORT, "lan");
        server.start();
        VoxelClient client = new VoxelClient("127.0.0.1", VoxelServer.DEFAULT_PORT);
        return new GameSession("lan", client.initialSave(), client, server);
    }

    public static GameSession joinLan(String host) {
        VoxelClient client = new VoxelClient(host, VoxelServer.DEFAULT_PORT);
        return new GameSession("lan-client", client.initialSave(), client, null);
    }

    /** Marks only the affected chunk meshes for rebuild, not the whole world. */
    public void markBlockChanged(int x, int y, int z) {
        Set<String> affected = new HashSet<>();
        collectAffectedChunkKeys(x, z, affected);
        for (String key : affected) {
            if (chunkMeshes.containsKey(key)) {
                rebuildChunkSync(key);
            } else {
                dirtyChunks.add(key);
            }
        }
    }

    private void collectAffectedChunkKeys(int x, int z, Set<String> affected) {
        affected.add(chunkKey(chunkCoord(x), chunkCoord(z)));

        int localX = Math.floorMod(x, ChunkGenerator.CHUNK_SIZE);
        int localZ = Math.floorMod(z, ChunkGenerator.CHUNK_SIZE);
        int cx = chunkCoord(x);
        int cz = chunkCoord(z);
        if (localX == 0) {
            affected.add(chunkKey(cx - 1, cz));
        }
        if (localX == ChunkGenerator.CHUNK_SIZE - 1) {
            affected.add(chunkKey(cx + 1, cz));
        }
        if (localZ == 0) {
            affected.add(chunkKey(cx, cz - 1));
        }
        if (localZ == ChunkGenerator.CHUNK_SIZE - 1) {
            affected.add(chunkKey(cx, cz + 1));
        }
    }

    public void updateMeshesIfNeeded() {
        int cx = chunkCoord((int) controller.getEyeX());
        int cz = chunkCoord((int) controller.getEyeZ());
        syncChunkEvents();

        boolean playerMoved = cx != lastMeshCx || cz != lastMeshCz;
        if (playerMoved || !dirtyChunks.isEmpty()) {
            scheduleVisibleMeshes(cx, cz);
            lastMeshCx = cx;
            lastMeshCz = cz;
        }

        uploadReadyMeshes();
    }

    public void update(double dt) {
        controller.update(dt);
        syncChunkEvents();
        if (mode == GameMode.FREE) {
            mobManager.update(dt, world);
        }
        drainNetwork();
        autosaveTimer += dt;
        if (autosaveTimer >= 10) {
            autosaveTimer = 0;
            save();
        }
    }

    private void syncChunkEvents() {
        if (mode != GameMode.FREE) return;
        for (String key : world.drainLoadedChunks()) {
            int comma = key.indexOf(',');
            int cx = Integer.parseInt(key.substring(0, comma));
            int cz = Integer.parseInt(key.substring(comma + 1));
            mobManager.onChunkLoaded(cx, cz, world);
            markChunkDirty(cx, cz);
        }
        for (String key : world.drainUnloadedChunks()) {
            int comma = key.indexOf(',');
            int cx = Integer.parseInt(key.substring(0, comma));
            int cz = Integer.parseInt(key.substring(comma + 1));
            mobManager.onChunkUnloaded(cx, cz);
            removeChunkMesh(key);
        }
    }

    private void scheduleVisibleMeshes(int pcx, int pcz) {
        Set<String> wanted = new HashSet<>();
        for (int dx = -World.VIEW_DISTANCE; dx <= World.VIEW_DISTANCE; dx++) {
            for (int dz = -World.VIEW_DISTANCE; dz <= World.VIEW_DISTANCE; dz++) {
                wanted.add(chunkKey(pcx + dx, pcz + dz));
            }
        }

        for (String key : new HashSet<>(chunkMeshes.keySet())) {
            if (!wanted.contains(key)) {
                removeChunkMesh(key);
            }
        }
        pendingMeshes.keySet().removeIf(key -> !wanted.contains(key));

        for (String key : wanted) {
            boolean missing = !chunkMeshes.containsKey(key);
            boolean dirty = dirtyChunks.contains(key);
            if (!missing && !dirty) {
                continue;
            }
            scheduleChunkRebuild(key);
        }
    }

    private void scheduleChunkRebuild(String key) {
        dirtyChunks.remove(key);
        pendingMeshes.remove(key);

        int comma = key.indexOf(',');
        int cx = Integer.parseInt(key.substring(0, comma));
        int cz = Integer.parseInt(key.substring(comma + 1));
        int version = meshVersions.getOrDefault(key, 0) + 1;
        meshVersions.put(key, version);

        pendingMeshes.put(key, CompletableFuture.supplyAsync(() ->
            new MeshBuildResult(key, VoxelMeshBuilder.buildMeshData(world, cx, cz), version), meshExecutor));
    }

    private void rebuildChunkSync(String key) {
        dirtyChunks.remove(key);
        pendingMeshes.remove(key);

        int comma = key.indexOf(',');
        int cx = Integer.parseInt(key.substring(0, comma));
        int cz = Integer.parseInt(key.substring(comma + 1));
        int version = meshVersions.getOrDefault(key, 0) + 1;
        meshVersions.put(key, version);

        MeshData data = VoxelMeshBuilder.buildMeshData(world, cx, cz);
        applyMeshData(key, data);
    }

    private void applyMeshData(String key, MeshData data) {
        if (data.isEmpty()) {
            ChunkMesh removed = chunkMeshes.remove(key);
            if (removed != null) {
                removed.cleanup();
            }
            return;
        }

        ChunkMesh old = chunkMeshes.remove(key);
        ChunkMesh mesh = VoxelMeshBuilder.uploadMesh(data);
        if (mesh.isValid()) {
            chunkMeshes.put(key, mesh);
        }
        if (old != null) {
            old.cleanup();
        }
    }

    private void uploadReadyMeshes() {
        for (var entry : new HashSet<>(pendingMeshes.entrySet())) {
            CompletableFuture<MeshBuildResult> future = entry.getValue();
            if (!future.isDone()) {
                continue;
            }

            pendingMeshes.remove(entry.getKey());
            MeshBuildResult result = future.join();
            int currentVersion = meshVersions.getOrDefault(result.key(), 0);
            if (result.version() != currentVersion) {
                continue;
            }

            MeshData data = result.data();
            applyMeshData(result.key(), data);
        }
    }

    private void markChunkDirty(int cx, int cz) {
        dirtyChunks.add(chunkKey(cx, cz));
    }

    private void removeChunkMesh(String key) {
        ChunkMesh mesh = chunkMeshes.remove(key);
        if (mesh != null) {
            mesh.cleanup();
        }
        pendingMeshes.remove(key);
        dirtyChunks.remove(key);
    }

    private static int chunkCoord(int worldCoord) {
        return Math.floorDiv(worldCoord, ChunkGenerator.CHUNK_SIZE);
    }

    private static String chunkKey(int cx, int cz) {
        return cx + "," + cz;
    }

    public void dispose() {
        save();
        meshExecutor.shutdownNow();
        for (ChunkMesh mesh : chunkMeshes.values()) {
            mesh.cleanup();
        }
        chunkMeshes.clear();
        mobManager.clear();
        if (lanClient != null) lanClient.close();
        if (lanServer != null) lanServer.close();
    }

    public boolean checkWin() {
        return world.checkMazeWin(controller.getEyeX(), controller.getEyeZ());
    }

    public void save() {
        if (lanServer == null && lanClient != null) {
            return;
        }
        WorldSave save = new WorldSave(worldName, mode, world.getSeed());
        save.playerX = controller.x;
        save.playerY = controller.y;
        save.playerZ = controller.z;
        save.yaw = controller.yaw;
        save.pitch = controller.pitch;
        save.selectedSlot = inventory.getSelectedSlot();
        save.inventory = inventory.snapshot();
        save.modifications.putAll(world.getModificationsSnapshot());
        WorldStorage.save(save);
    }

    public void sendBlockChange(String blockKey, String typeId) {
        if (lanClient != null) {
            lanClient.sendBlockChange(blockKey, typeId);
        }
    }

    private void drainNetwork() {
        if (lanClient == null) return;
        NetworkMessage message;
        while ((message = lanClient.poll()) != null) {
            if (NetworkMessage.BLOCK.equals(message.type())) {
                int split = message.payload().indexOf('=');
                if (split > 0) {
                    String blockKey = message.payload().substring(0, split);
                    world.applyBlockChange(blockKey, message.payload().substring(split + 1));
                    String[] parts = blockKey.split(",");
                    if (parts.length == 3) {
                        markBlockChanged(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                    }
                }
            }
        }
    }

    private static String defaultWorldName(GameMode mode) {
        return mode == GameMode.MAZE ? "maze" : "world";
    }

    private record MeshBuildResult(String key, MeshData data, int version) {}
}
