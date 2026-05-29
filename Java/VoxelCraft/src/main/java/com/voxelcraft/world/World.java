package com.voxelcraft.world;

import com.voxelcraft.block.Block;
import com.voxelcraft.block.BlockPos;
import com.voxelcraft.block.BlockType;
import com.voxelcraft.mode.GameMode;

import java.util.*;

public class World {
    public static final int VIEW_DISTANCE = 2;
    public static final int UNLOAD_DISTANCE = 3;
    private static final double PLAYER_RADIUS = 0.35;
    private static final double SNAP = 0.12;

    private final GameMode gameMode;
    private final Map<String, Chunk> chunks = new HashMap<>();
    private final Map<String, String> modifications = new HashMap<>();
    private final Set<String> newlyLoadedChunks = new LinkedHashSet<>();
    private final Set<String> newlyUnloadedChunks = new LinkedHashSet<>();
    private final double spawnX;
    private final double spawnZ;
    private final double spawnGroundY;
    private MazeGenerator.MazeData mazeData;

    public World(GameMode mode) {
        this.gameMode = mode;
        if (mode == GameMode.MAZE) {
            mazeData = MazeGenerator.generateMaze(15);
            spawnX = mazeData.spawn()[0] + 0.5;
            spawnZ = mazeData.spawn()[1] + 0.5;
            spawnGroundY = 1.0;
            loadMazeBlocks();
        } else {
            TerrainGenerator.SpawnPoint spawn = TerrainGenerator.findLandSpawn(0, 0, 96);
            spawnX = spawn.x();
            spawnZ = spawn.z();
            spawnGroundY = spawn.groundY();
            ensureLoaded((int) spawnX, (int) spawnZ);
        }
    }

    private void loadMazeBlocks() {
        for (Block block : MazeGenerator.mazeToBlocks(mazeData)) {
            putGeneratedBlock(block);
        }
    }

    private void putGeneratedBlock(Block block) {
        String ck = chunkCoords(block.x(), block.z());
        Chunk chunk = chunks.computeIfAbsent(ck, key -> {
            String[] parts = key.split(",");
            return new Chunk(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), new HashMap<>());
        });
        chunk.blocks.put(block.pos.key(), block);
        if (TerrainGenerator.isSolidBlock(block.type.id)) {
            String columnKey = block.x() + "," + block.z();
            chunk.columnTops.merge(columnKey, block.y() + 1, Math::max);
        }
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public double getSpawnX() {
        return spawnX;
    }

    public double getSpawnZ() {
        return spawnZ;
    }

    public double getSpawnGroundY() {
        return spawnGroundY;
    }

    public boolean checkMazeWin(double x, double z) {
        if (gameMode != GameMode.MAZE || mazeData == null) return false;
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        return bx == mazeData.exit()[0] && bz == mazeData.exit()[1];
    }

    public void loadChunk(int cx, int cz) {
        if (gameMode == GameMode.MAZE) return;

        String key = cx + "," + cz;
        if (chunks.containsKey(key)) return;

        ChunkGenerator.ChunkBlocks gen = ChunkGenerator.generateChunk(cx, cz);
        Map<String, Block> blockMap = new HashMap<>();

        for (Block b : gen.blocks()) {
            String bk = b.pos.key();
            String mod = modifications.get(bk);
            if (mod == null) {
                // keep generated block
            } else if ("null".equals(mod)) {
                continue;
            } else {
                BlockType mt = BlockType.fromId(mod);
                if (mt != null) {
                    blockMap.put(bk, new Block(b.pos, mt));
                    continue;
                }
            }
            blockMap.put(bk, b);
        }

        for (var entry : modifications.entrySet()) {
            if ("null".equals(entry.getValue())) continue;
            BlockType mt = BlockType.fromId(entry.getValue());
            if (mt == null) continue;
            String[] parts = entry.getKey().split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            BlockPos pos = new BlockPos(x, y, z);
            String ck = chunkCoords(x, z);
            if (ck.equals(key) && !blockMap.containsKey(pos.key())) {
                blockMap.put(pos.key(), new Block(pos, mt));
            }
        }

        Chunk chunk = new Chunk(cx, cz, blockMap);
        rebuildColumnTops(chunk);
        chunks.put(key, chunk);
        newlyLoadedChunks.add(key);
    }

    public void unloadChunk(int cx, int cz) {
        if (gameMode == GameMode.MAZE) return;
        chunks.remove(cx + "," + cz);
    }

    public void update(int playerX, int playerZ) {
        if (gameMode == GameMode.MAZE) return;

        int pcx = Math.floorDiv(playerX, ChunkGenerator.CHUNK_SIZE);
        int pcz = Math.floorDiv(playerZ, ChunkGenerator.CHUNK_SIZE);

        for (int dx = -VIEW_DISTANCE; dx <= VIEW_DISTANCE; dx++) {
            for (int dz = -VIEW_DISTANCE; dz <= VIEW_DISTANCE; dz++) {
                loadChunk(pcx + dx, pcz + dz);
            }
        }

        for (var it = chunks.entrySet().iterator(); it.hasNext(); ) {
            Chunk c = it.next().getValue();
            int dist = Math.max(Math.abs(c.cx - pcx), Math.abs(c.cz - pcz));
            if (dist > UNLOAD_DISTANCE) {
                newlyUnloadedChunks.add(c.cx + "," + c.cz);
                it.remove();
            }
        }
    }

    public void ensureLoaded(int playerX, int playerZ) {
        int pcx = Math.floorDiv(playerX, ChunkGenerator.CHUNK_SIZE);
        int pcz = Math.floorDiv(playerZ, ChunkGenerator.CHUNK_SIZE);

        for (int dx = -VIEW_DISTANCE; dx <= VIEW_DISTANCE; dx++) {
            for (int dz = -VIEW_DISTANCE; dz <= VIEW_DISTANCE; dz++) {
                loadChunk(pcx + dx, pcz + dz);
            }
        }
    }

    public double getSupportHeight(double x, double z, double feetY) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        double maxSurfaceY = feetY + SNAP + 0.05;

        double[][] offsets = {{0, 0}, {PLAYER_RADIUS, 0}, {-PLAYER_RADIUS, 0},
            {0, PLAYER_RADIUS}, {0, -PLAYER_RADIUS}};

        double best = Double.NEGATIVE_INFINITY;
        for (double[] off : offsets) {
            Double s = getColumnSupportAt((int) Math.floor(x + off[0]),
                (int) Math.floor(z + off[1]), maxSurfaceY);
            if (s != null && s > best) best = s;
        }
        return best == Double.NEGATIVE_INFINITY ? spawnGroundY : best;
    }

    private Double getColumnSupportAt(int bx, int bz, double maxSurfaceY) {
        String ck = chunkCoords(bx, bz);
        Chunk chunk = chunks.get(ck);
        if (chunk == null) return null;

        double best = Double.NEGATIVE_INFINITY;
        for (Block b : chunk.blocks.values()) {
            if (b.x() != bx || b.z() != bz) continue;
            if (!TerrainGenerator.isSolidBlock(b.type.id)) continue;
            double surface = b.y() + 1;
            if (surface <= maxSurfaceY && surface > best) best = surface;
        }
        return best == Double.NEGATIVE_INFINITY ? null : best;
    }

    public boolean collides(double x, double y, double z) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        String ck = chunkCoords(bx, bz);
        Chunk chunk = chunks.get(ck);
        if (chunk == null) return false;
        Block b = chunk.blocks.get(new BlockPos(bx, by, bz).key());
        return b != null && TerrainGenerator.isSolidBlock(b.type.id);
    }

    public Block getBlock(int x, int y, int z) {
        Chunk chunk = chunks.get(chunkCoords(x, z));
        if (chunk == null) return null;
        return chunk.blocks.get(new BlockPos(x, y, z).key());
    }

    public boolean isWaterAt(double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        Chunk chunk = chunks.get(chunkCoords(bx, bz));
        if (chunk == null) return false;
        for (Block b : chunk.blocks.values()) {
            if (b.x() == bx && b.z() == bz && b.type == BlockType.WATER) return true;
        }
        return false;
    }

    public Set<String> drainLoadedChunks() {
        Set<String> drained = new LinkedHashSet<>(newlyLoadedChunks);
        newlyLoadedChunks.clear();
        return drained;
    }

    public Set<String> drainUnloadedChunks() {
        Set<String> drained = new LinkedHashSet<>(newlyUnloadedChunks);
        newlyUnloadedChunks.clear();
        return drained;
    }

    public BlockType removeBlock(int x, int y, int z) {
        String key = new BlockPos(x, y, z).key();
        String ck = chunkCoords(x, z);
        Chunk chunk = chunks.get(ck);
        if (chunk == null) return null;
        Block b = chunk.blocks.remove(key);
        if (b == null) return null;
        modifications.put(key, "null");
        rebuildColumnTops(chunk);
        return b.type;
    }

    public boolean placeBlock(int x, int y, int z, BlockType type) {
        String key = new BlockPos(x, y, z).key();
        String ck = chunkCoords(x, z);
        Chunk chunk = chunks.get(ck);
        if (chunk == null) return false;
        if (chunk.blocks.containsKey(key)) return false;
        chunk.blocks.put(key, new Block(x, y, z, type));
        modifications.put(key, type.id);
        rebuildColumnTops(chunk);
        return true;
    }

    public Collection<Block> getAllBlocks() {
        List<Block> all = new ArrayList<>();
        for (Chunk c : chunks.values()) {
            all.addAll(c.blocks.values());
        }
        return all;
    }

    private String chunkCoords(int x, int z) {
        int cx = Math.floorDiv(x, ChunkGenerator.CHUNK_SIZE);
        int cz = Math.floorDiv(z, ChunkGenerator.CHUNK_SIZE);
        return cx + "," + cz;
    }

    private void rebuildColumnTops(Chunk chunk) {
        chunk.columnTops.clear();
        for (Block b : chunk.blocks.values()) {
            if (!TerrainGenerator.isSolidBlock(b.type.id)) continue;
            String ck = b.x() + "," + b.z();
            chunk.columnTops.merge(ck, b.y() + 1, Math::max);
        }
    }

    public static class Chunk {
        public final int cx, cz;
        public final Map<String, Block> blocks;
        public final Map<String, Integer> columnTops = new HashMap<>();

        Chunk(int cx, int cz, Map<String, Block> blocks) {
            this.cx = cx;
            this.cz = cz;
            this.blocks = blocks;
        }
    }
}
