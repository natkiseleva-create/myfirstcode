package com.voxelcraft.world;

import com.voxelcraft.math.FastNoise;

public class TerrainGenerator {
    public static final int SEA_LEVEL = 4;
    public static final int MIN_HEIGHT = 2;
    public static final int MAX_HEIGHT = 14;
    public static final int STONE_DEPTH = 20;

    public static double getTerrainHeight(int wx, int wz) {
        double hills = FastNoise.fbm(wx * 0.012, wz * 0.012, 5, 11);
        double detail = FastNoise.fbm(wx * 0.045, wz * 0.045, 3, 29) * 0.35;
        double height = Math.floor(SEA_LEVEL + 2 + hills * 9 + detail * 4);

        double riverValley = Math.abs(FastNoise.fbm(wx * 0.028 + 40, wz * 0.028 - 20, 4, 53) - 0.5);
        if (riverValley < 0.045) {
            height = Math.min(height, SEA_LEVEL);
        }

        double lakeBasin = FastNoise.fbm(wx * 0.018 + 120, wz * 0.018 + 80, 4, 71);
        if (lakeBasin < 0.2) {
            height = Math.min(height, SEA_LEVEL - 1 + Math.floor(lakeBasin * 5));
        }

        return Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height));
    }

    public static boolean isWaterColumn(int wx, int wz) {
        return getTerrainHeight(wx, wz) < SEA_LEVEL;
    }

    public static boolean isLandColumn(int wx, int wz) {
        if (isWaterColumn(wx, wz)) return false;
        return getTerrainHeight(wx, wz) >= SEA_LEVEL;
    }

    public static boolean isSolidBlock(String typeId) {
        return !"water".equals(typeId);
    }

    public static String getSurfaceBlock(int wx, int wz, int surfaceY) {
        if (isWaterColumn(wx, wz)) return "sand";
        if (surfaceY <= SEA_LEVEL + 1 && hasWaterNear(wx, wz, 2)) return "sand";
        if (surfaceY > SEA_LEVEL + 7 && FastNoise.randomAt(wx + 3, wz + 9, 17) < 0.08) return "stone";
        return "grass";
    }

    public static boolean canPlaceTree(int wx, int wz) {
        if (isWaterColumn(wx, wz)) return false;
        if (hasWaterNear(wx, wz, 1)) return false;
        double h = getTerrainHeight(wx, wz);
        return h >= SEA_LEVEL + 2 && h <= MAX_HEIGHT - 3;
    }

    private static boolean hasWaterNear(int wx, int wz, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (isWaterColumn(wx + dx, wz + dz)) return true;
            }
        }
        return false;
    }

    public record SpawnPoint(double x, double z, double groundY) {}

    public static SpawnPoint findLandSpawn(int startX, int startZ, int maxRadius) {
        SpawnPoint center = trySpawnPoint(startX, startZ);
        if (center != null) return center;

        for (int r = 1; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    SpawnPoint point = trySpawnPoint(startX + dx, startZ + dz);
                    if (point != null) return point;
                }
            }
        }

        return new SpawnPoint(startX + 0.5, startZ + 0.5, SEA_LEVEL + 1);
    }

    private static SpawnPoint trySpawnPoint(int wx, int wz) {
        if (!isLandColumn(wx, wz)) return null;
        return new SpawnPoint(wx + 0.5, wz + 0.5, getTerrainHeight(wx, wz));
    }
}
