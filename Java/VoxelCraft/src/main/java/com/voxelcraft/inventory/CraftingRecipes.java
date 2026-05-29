package com.voxelcraft.inventory;

import com.voxelcraft.block.BlockType;

import java.util.*;

public class CraftingRecipes {
    public static final List<Recipe> ALL = List.of(
        new Recipe("planks", 2,
            Map.of("wood", 1), BlockType.PLANKS, 4),
        new Recipe("crafting_table", 2,
            new String[][]{
                {"planks", "planks"},
                {"planks", "planks"}
            }, BlockType.CRAFTING_TABLE, 1),
        new Recipe("planks_3", 3,
            Map.of("wood", 1), BlockType.PLANKS, 4),
        new Recipe("crafting_table_3", 3,
            new String[][]{
                {"planks", "planks", null},
                {"planks", "planks", null},
                {null, null, null}
            }, BlockType.CRAFTING_TABLE, 1)
    );

    public static RecipeResult match(String[][] grid, int gridSize) {
        String[][] norm = normalizePattern(grid, gridSize);

        for (Recipe r : ALL) {
            if (r.gridSize != gridSize) continue;

            if (r.shapeless != null) {
                if (shapelessMatches(norm, r.shapeless)) {
                    return new RecipeResult(r, r.outputType, r.outputCount);
                }
                continue;
            }

            if (r.pattern != null && patternMatches(norm, normalizePattern(r.pattern, gridSize))) {
                return new RecipeResult(r, r.outputType, r.outputCount);
            }
        }

        return null;
    }

    private static boolean patternMatches(String[][] grid, String[][] pattern) {
        int size = grid.length;
        int patH = pattern.length;
        int patW = pattern[0].length;

        for (int offY = 0; offY <= size - patH; offY++) {
            for (int offX = 0; offX <= size - patW; offX++) {
                boolean ok = true;
                outer:
                for (int y = 0; y < size && ok; y++) {
                    for (int x = 0; x < size; x++) {
                        int py = y - offY;
                        int px = x - offX;
                        String expected = null;
                        if (py >= 0 && py < patH && px >= 0 && px < patW) {
                            expected = pattern[py][px];
                        }
                        String got = grid[y][x];
                        if (!Objects.equals(expected, got)) {
                            ok = false;
                            break outer;
                        }
                    }
                }
                if (ok) return true;
            }
        }
        return false;
    }

    private static boolean shapelessMatches(String[][] grid, Map<String, Integer> ingredients) {
        if (ingredients.isEmpty()) return false;
        List<String> flat = new ArrayList<>();
        for (String[] row : grid) {
            for (String cell : row) {
                if (cell != null) flat.add(cell);
            }
        }
        if (flat.isEmpty()) return false;

        Map<String, Integer> needed = new HashMap<>(ingredients);
        for (String cell : flat) {
            Integer cnt = needed.get(cell);
            if (cnt == null || cnt <= 0) return false;
            needed.put(cell, cnt - 1);
        }

        return needed.values().stream().allMatch(c -> c <= 0)
            && flat.size() == ingredients.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static String[][] normalizePattern(String[][] pattern, int gridSize) {
        String[][] result = new String[gridSize][gridSize];
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                result[y][x] = (y < pattern.length && x < pattern[y].length) ? pattern[y][x] : null;
            }
        }
        return result;
    }

    public record RecipeResult(Recipe recipe, BlockType outputType, int outputCount) {}
}
