package com.voxelcraft.world;

import com.voxelcraft.math.FastNoise;

public class TreeGenerator {
    private static final int CROWN_RADIUS = 2;
    private static final int MIN_CROWN_LEAVES = 12;
    static final int TREE_CELL = 6;

    public static boolean isTreeSpawnColumn(int wx, int wz) {
        int cell = TREE_CELL;
        int gx = Math.floorDiv(wx, cell);
        int gz = Math.floorDiv(wz, cell);
        int tx = gx * cell + 2 + (int)(FastNoise.randomAt(gx, gz, 311) * (cell - 4));
        int tz = gz * cell + 2 + (int)(FastNoise.randomAt(gz, gx, 317) * (cell - 4));
        return wx == tx && wz == tz;
    }

    public static int placeTree(int x, int z, TreeBlockCallback addBlock, int surfaceY) {
        int trunkBase = surfaceY;
        int trunkHeight = 4 + (int)(FastNoise.randomAt(x * 13 + 7, z * 11 + 3) * 3);
        int leavesPlaced = 0;
        int crownBase = trunkBase + trunkHeight - 2;

        for (int dx = -CROWN_RADIUS; dx <= CROWN_RADIUS; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -CROWN_RADIUS; dz <= CROWN_RADIUS; dz++) {
                    if (dx == 0 && dz == 0 && dy < trunkHeight - 1) continue;
                    int dist = Math.abs(dx) + Math.abs(dy - 1) + Math.abs(dz);
                    if (dist > 4) continue;
                    if (addBlock.add(x + dx, crownBase + dy, z + dz, "leaves")) {
                        leavesPlaced++;
                    }
                }
            }
        }

        if (leavesPlaced < MIN_CROWN_LEAVES) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (addBlock.add(x + dx, crownBase + 2, z + dz, "leaves")) {
                        leavesPlaced++;
                    }
                }
            }
            if (addBlock.add(x, crownBase + 3, z, "leaves")) leavesPlaced++;
        }

        if (leavesPlaced < MIN_CROWN_LEAVES) return 0;

        for (int y = trunkBase; y < trunkBase + trunkHeight; y++) {
            addBlock.add(x, y, z, "wood");
        }

        return trunkHeight;
    }

    @FunctionalInterface
    public interface TreeBlockCallback {
        boolean add(int x, int y, int z, String type);
    }
}
