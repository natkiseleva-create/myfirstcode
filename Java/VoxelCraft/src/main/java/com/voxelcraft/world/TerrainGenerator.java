package com.voxelcraft.world;

import com.voxelcraft.math.FastNoise;

public class TerrainGenerator {
    public static final int SEA_LEVEL = 4;
    public static final int MIN_HEIGHT = 2;
    public static final int MAX_HEIGHT = 14;
    public static final int STONE_DEPTH = 20;

    public record TerrainColumn(int height, int waterLevel, String biome) {
        public boolean hasWater() {
            return waterLevel > height;
        }
    }

    public static double getTerrainHeight(int wx, int wz) {
        return getTerrainHeight(wx, wz, 12345);
    }

    public static double getTerrainHeight(int wx, int wz, int seed) {
        return sampleColumn(wx, wz, seed).height();
    }

    public static TerrainColumn sampleColumn(int wx, int wz, int seed) {
        double warpX = (FastNoise.fbm(wx * 0.01 + 90, wz * 0.01 - 15, 3, seed + 401) - 0.5) * 42.0;
        double warpZ = (FastNoise.fbm(wx * 0.01 - 60, wz * 0.01 + 75, 3, seed + 409) - 0.5) * 42.0;

        double continental = FastNoise.fbm((wx + warpX) * 0.009, (wz + warpZ) * 0.009, 5, seed + 11);
        double hills = FastNoise.fbm((wx + warpX) * 0.026, (wz + warpZ) * 0.026, 4, seed + 29);
        double detail = FastNoise.fbm(wx * 0.075, wz * 0.075, 2, seed + 37);
        double height = SEA_LEVEL + 2.0 + (continental - 0.45) * 10.0 + (hills - 0.5) * 4.0 + (detail - 0.5) * 1.5;

        double riverNoise = FastNoise.fbm((wx + warpX * 0.6) * 0.018 + 40, (wz + warpZ * 0.6) * 0.018 - 20, 4, seed + 53);
        double river = Math.abs(riverNoise - 0.5);
        double riverWidth = 0.035 + FastNoise.fbm(wx * 0.006, wz * 0.006, 2, seed + 61) * 0.018;
        boolean riverColumn = river < riverWidth;
        if (riverColumn) {
            double center = 1.0 - river / Math.max(0.0001, riverWidth);
            height = Math.min(height, SEA_LEVEL - 1.0 - center * 1.8);
        }

        double lakeNoise = FastNoise.fbm((wx + warpX) * 0.014 + 120, (wz + warpZ) * 0.014 + 80, 4, seed + 71);
        boolean lakeColumn = lakeNoise < 0.24;
        if (lakeColumn) {
            double basin = (0.24 - lakeNoise) / 0.24;
            height = Math.min(height, SEA_LEVEL - 0.5 - basin * 2.5);
        }

        int waterLevel = (riverColumn || lakeColumn || height < SEA_LEVEL) ? SEA_LEVEL : Integer.MIN_VALUE;
        int blockHeight = (int) Math.floor(Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height)));
        String biome = waterLevel > blockHeight ? "shore" : (continental > 0.62 ? "forest" : "plains");
        return new TerrainColumn(blockHeight, waterLevel, biome);
    }

    public static boolean isWaterColumn(int wx, int wz) {
        return isWaterColumn(wx, wz, 12345);
    }

    public static boolean isWaterColumn(int wx, int wz, int seed) {
        return sampleColumn(wx, wz, seed).hasWater();
    }

    public static int getWaterLevel(int wx, int wz, int seed) {
        return sampleColumn(wx, wz, seed).waterLevel();
    }

    public static boolean isLandColumn(int wx, int wz) {
        return isLandColumn(wx, wz, 12345);
    }

    public static boolean isLandColumn(int wx, int wz, int seed) {
        if (isWaterColumn(wx, wz, seed)) return false;
        return getTerrainHeight(wx, wz, seed) >= SEA_LEVEL;
    }

    public static boolean isSolidBlock(String typeId) {
        return !"water".equals(typeId);
    }

    public static String getSurfaceBlock(int wx, int wz, int surfaceY) {
        return getSurfaceBlock(wx, wz, surfaceY, 12345);
    }

    public static String getSurfaceBlock(int wx, int wz, int surfaceY, int seed) {
        if (isWaterColumn(wx, wz, seed)) return "sand";
        if (surfaceY <= SEA_LEVEL + 1 && hasWaterNear(wx, wz, 2, seed)) return "sand";
        return "grass";
    }

    public static boolean canPlaceTree(int wx, int wz) {
        return canPlaceTree(wx, wz, 12345);
    }

    public static boolean canPlaceTree(int wx, int wz, int seed) {
        if (isWaterColumn(wx, wz, seed)) return false;
        if (hasWaterNear(wx, wz, 1, seed)) return false;
        double h = getTerrainHeight(wx, wz, seed);
        return h >= SEA_LEVEL + 2 && h <= MAX_HEIGHT - 3;
    }

    private static boolean hasWaterNear(int wx, int wz, int radius) {
        return hasWaterNear(wx, wz, radius, 12345);
    }

    private static boolean hasWaterNear(int wx, int wz, int radius, int seed) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (isWaterColumn(wx + dx, wz + dz, seed)) return true;
            }
        }
        return false;
    }

    public record SpawnPoint(double x, double z, double groundY) {}

    public static SpawnPoint findLandSpawn(int startX, int startZ, int maxRadius) {
        return findLandSpawn(startX, startZ, maxRadius, 12345);
    }

    public static SpawnPoint findLandSpawn(int startX, int startZ, int maxRadius, int seed) {
        SpawnPoint center = trySpawnPoint(startX, startZ, seed);
        if (center != null) return center;

        for (int r = 1; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    SpawnPoint point = trySpawnPoint(startX + dx, startZ + dz, seed);
                    if (point != null) return point;
                }
            }
        }

        return new SpawnPoint(startX + 0.5, startZ + 0.5, SEA_LEVEL + 1);
    }

    private static SpawnPoint trySpawnPoint(int wx, int wz) {
        return trySpawnPoint(wx, wz, 12345);
    }

    private static SpawnPoint trySpawnPoint(int wx, int wz, int seed) {
        if (!isLandColumn(wx, wz, seed)) return null;
        return new SpawnPoint(wx + 0.5, wz + 0.5, getTerrainHeight(wx, wz, seed));
    }
}
