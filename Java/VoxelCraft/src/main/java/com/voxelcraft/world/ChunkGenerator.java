package com.voxelcraft.world;

import com.voxelcraft.block.Block;
import com.voxelcraft.math.FastNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class ChunkGenerator {
    public static final int CHUNK_SIZE = 16;
    private static final double TREE_SPAWN_THRESHOLD = 0.82;

    public static ChunkBlocks generateChunk(int cx, int cz) {
        List<Block> blocks = new ArrayList<>();
        int baseX = cx * CHUNK_SIZE;
        int baseZ = cz * CHUNK_SIZE;

        // Terrain column filling
        for (int lx = 0; lx < CHUNK_SIZE; lx++) {
            for (int lz = 0; lz < CHUNK_SIZE; lz++) {
                int wx = baseX + lx;
                int wz = baseZ + lz;
                int surfaceY = (int) TerrainGenerator.getTerrainHeight(wx, wz);
                fillTerrainColumn(wx, wz, surfaceY, blocks);
            }
        }

        // Trees
        for (int lx = 3; lx < CHUNK_SIZE - 3; lx++) {
            for (int lz = 3; lz < CHUNK_SIZE - 3; lz++) {
                int wx = baseX + lx;
                int wz = baseZ + lz;

                if (wx * wx + wz * wz < 36) continue;
                if (!TerrainGenerator.canPlaceTree(wx, wz)) continue;
                if (!TreeGenerator.isTreeSpawnColumn(wx, wz)) continue;
                if (FastNoise.randomAt(wx * 3 + 17, wz * 7 + 31) > TREE_SPAWN_THRESHOLD) continue;
                if (hasTreeNearby(blocks, wx, wz)) continue;

                int surfaceY = (int) TerrainGenerator.getTerrainHeight(wx, wz);
                if (surfaceY < 5) continue;

                TreeGenerator.placeTree(wx, wz, (x, y, z, type) -> {
                    blocks.add(new Block(x, y, z, com.voxelcraft.block.BlockType.fromId(type)));
                    return true;
                }, surfaceY);
            }
        }

        // Prune orphan wood (keep blocks with leaves nearby)
        List<Block> filtered = pruneOrphanWood(blocks);
        return new ChunkBlocks(cx, cz, filtered);
    }

    private static boolean hasTreeNearby(List<Block> blocks, int wx, int wz) {
        int radius = TreeGenerator.TREE_CELL - 2;
        int r2 = radius * radius;
        for (Block b : blocks) {
            if (b.type != com.voxelcraft.block.BlockType.WOOD && b.type != com.voxelcraft.block.BlockType.LEAVES) continue;
            int dx = b.x() - wx;
            int dz = b.z() - wz;
            if (dx * dx + dz * dz < r2) return true;
        }
        return false;
    }

    public static List<Block> pruneOrphanWood(List<Block> blocks) {
        java.util.Map<String, Integer> leafTopByColumn = new java.util.HashMap<>();

        for (Block b : blocks) {
            if (b.type != com.voxelcraft.block.BlockType.LEAVES) continue;
            String key = b.x() + "," + b.z();
            int prev = leafTopByColumn.getOrDefault(key, Integer.MIN_VALUE);
            if (b.y() > prev) leafTopByColumn.put(key, b.y());
        }

        List<Block> result = new ArrayList<>();
        for (Block b : blocks) {
            if (b.type != com.voxelcraft.block.BlockType.WOOD) {
                result.add(b);
                continue;
            }
            boolean hasCrown = false;
            for (int dx = -2; dx <= 2 && !hasCrown; dx++) {
                for (int dz = -2; dz <= 2 && !hasCrown; dz++) {
                    Integer top = leafTopByColumn.get((b.x() + dx) + "," + (b.z() + dz));
                    if (top != null && top >= b.y()) hasCrown = true;
                }
            }
            if (hasCrown) result.add(b);
        }
        return result;
    }

    private static void fillTerrainColumn(int wx, int wz, int surfaceY, List<Block> blocks) {
        for (int y = -TerrainGenerator.STONE_DEPTH; y < surfaceY; y++) {
            String type = (y < surfaceY - 3 || y < 0) ? "stone" : "dirt";
            blocks.add(new Block(wx, y, wz, com.voxelcraft.block.BlockType.fromId(type)));
        }

        String surfaceBlock = TerrainGenerator.isWaterColumn(wx, wz) ? "sand"
                : TerrainGenerator.getSurfaceBlock(wx, wz, surfaceY);
        blocks.add(new Block(wx, surfaceY, wz, com.voxelcraft.block.BlockType.fromId(surfaceBlock)));

        if (surfaceY < TerrainGenerator.SEA_LEVEL) {
            for (int y = surfaceY + 1; y <= TerrainGenerator.SEA_LEVEL; y++) {
                blocks.add(new Block(wx, y, wz, com.voxelcraft.block.BlockType.WATER));
            }
        }
    }

    public record ChunkBlocks(int cx, int cz, List<Block> blocks) {}
}
