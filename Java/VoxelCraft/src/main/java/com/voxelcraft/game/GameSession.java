package com.voxelcraft.game;

import com.voxelcraft.control.FirstPersonController;
import com.voxelcraft.entities.MobManager;
import com.voxelcraft.inventory.Inventory;
import com.voxelcraft.mode.GameMode;
import com.voxelcraft.render.VoxelMeshBuilder;
import com.voxelcraft.render.VoxelMeshBuilder.ChunkMesh;
import com.voxelcraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class GameSession {
    public final GameMode mode;
    public final World world;
    public final FirstPersonController controller;
    public final Inventory inventory;
    public final MobManager mobManager;
    public final Map<String, ChunkMesh> chunkMeshes = new HashMap<>();

    private int lastMeshCx = Integer.MIN_VALUE;
    private int lastMeshCz = Integer.MIN_VALUE;
    private boolean meshDirty = true;

    public GameSession(GameMode mode) {
        this.mode = mode;
        world = new World(mode);
        inventory = new Inventory();
        mobManager = new MobManager();

        double ground = world.getSupportHeight(world.getSpawnX(), world.getSpawnZ(), world.getSpawnGroundY());
        controller = new FirstPersonController(world, world.getSpawnX(), ground, world.getSpawnZ());
        controller.isLocked = false;
        syncChunkEvents();
    }

    public void markMeshesDirty() {
        meshDirty = true;
    }

    public void updateMeshesIfNeeded() {
        int cx = Math.floorDiv((int) controller.getEyeX(), 16);
        int cz = Math.floorDiv((int) controller.getEyeZ(), 16);
        syncChunkEvents();
        if (!meshDirty && cx == lastMeshCx && cz == lastMeshCz) {
            return;
        }
        rebuildMeshes(cx, cz);
        lastMeshCx = cx;
        lastMeshCz = cz;
        meshDirty = false;
    }

    public void update(double dt) {
        controller.update(dt);
        syncChunkEvents();
        if (mode == GameMode.FREE) {
            mobManager.update(dt, world);
        }
    }

    private void syncChunkEvents() {
        if (mode != GameMode.FREE) return;
        for (String key : world.drainLoadedChunks()) {
            int comma = key.indexOf(',');
            int cx = Integer.parseInt(key.substring(0, comma));
            int cz = Integer.parseInt(key.substring(comma + 1));
            mobManager.onChunkLoaded(cx, cz, world);
            meshDirty = true;
        }
        for (String key : world.drainUnloadedChunks()) {
            int comma = key.indexOf(',');
            int cx = Integer.parseInt(key.substring(0, comma));
            int cz = Integer.parseInt(key.substring(comma + 1));
            mobManager.onChunkUnloaded(cx, cz);
            meshDirty = true;
        }
    }

    private void rebuildMeshes(int pcx, int pcz) {
        for (ChunkMesh mesh : chunkMeshes.values()) {
            mesh.cleanup();
        }
        chunkMeshes.clear();

        for (int dx = -World.VIEW_DISTANCE; dx <= World.VIEW_DISTANCE; dx++) {
            for (int dz = -World.VIEW_DISTANCE; dz <= World.VIEW_DISTANCE; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                String key = cx + "," + cz;
                ChunkMesh mesh = VoxelMeshBuilder.buildMesh(world, cx, cz);
                if (mesh.isValid()) {
                    chunkMeshes.put(key, mesh);
                }
            }
        }
    }

    public void dispose() {
        for (ChunkMesh mesh : chunkMeshes.values()) {
            mesh.cleanup();
        }
        chunkMeshes.clear();
        mobManager.clear();
    }

    public boolean checkWin() {
        return world.checkMazeWin(controller.getEyeX(), controller.getEyeZ());
    }
}
