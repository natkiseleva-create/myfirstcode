package com.voxelcraft.world;

import com.voxelcraft.block.Block;
import com.voxelcraft.block.BlockType;
import com.voxelcraft.math.FastNoise;

import java.util.*;

public class MazeGenerator {

    public static MazeData generateMaze(int cells) {
        int w = cells * 2 + 1;
        int h = cells * 2 + 1;
        int[][] grid = new int[h][w];
        for (int y = 0; y < h; y++) Arrays.fill(grid[y], 1);

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{1, 1});
        grid[1][1] = 0;

        int[][] dirs = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};

        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            int cx = cur[0], cz = cur[1];

            // Shuffle directions deterministically
            List<int[]> shuffled = new ArrayList<>();
            for (int i = 0; i < dirs.length; i++) {
                shuffled.add(dirs[i]);
            }
            shuffled.sort(Comparator.comparingDouble(a ->
                FastNoise.randomAt(cx + 10 * a[0], cz + 10 * a[1])));

            boolean carved = false;
            for (int[] d : shuffled) {
                int nx = cx + d[0];
                int nz = cz + d[1];
                if (nx > 0 && nx < w - 1 && nz > 0 && nz < h - 1 && grid[nz][nx] == 1) {
                    grid[cz + d[1] / 2][cx + d[0] / 2] = 0;
                    grid[nz][nx] = 0;
                    stack.push(new int[]{nx, nz});
                    carved = true;
                    break;
                }
            }
            if (!carved) stack.pop();
        }

        grid[1][1] = 0;
        grid[h - 2][w - 2] = 0;

        return new MazeData(w, h, grid, new int[]{1, 1}, new int[]{w - 2, h - 2});
    }

    public static List<Block> mazeToBlocks(MazeData maze) {
        List<Block> blocks = new ArrayList<>();
        int wallHeight = 3;
        int exitX = maze.exit[0], exitZ = maze.exit[1];

        for (int z = 0; z < maze.height; z++) {
            for (int x = 0; x < maze.width; x++) {
                if (maze.grid[z][x] == 1) {
                    for (int y = 0; y < wallHeight; y++) {
                        blocks.add(new Block(x, y, z, BlockType.STONE));
                    }
                } else {
                    // Stone layers below ground
                    for (int y = -TerrainGenerator.STONE_DEPTH; y < 0; y++) {
                        blocks.add(new Block(x, y, z, BlockType.STONE));
                    }
                    blocks.add(new Block(x, 0, z, BlockType.GRASS));
                    if (x == exitX && z == exitZ) {
                        blocks.add(new Block(x, 1, z, BlockType.GOLD));
                    }
                }
            }
        }

        return blocks;
    }

    public record MazeData(int width, int height, int[][] grid, int[] spawn, int[] exit) {}
}
