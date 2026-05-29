package com.voxelcraft.inventory;

import com.voxelcraft.block.BlockType;

import java.util.*;

public class Recipe {
    public final String id;
    public final int gridSize;
    public final String[][] pattern; // null = empty cell
    public final Map<String, Integer> shapeless; // type id -> count
    public final BlockType outputType;
    public final int outputCount;

    public Recipe(String id, int gridSize, String[][] pattern, BlockType outputType, int outputCount) {
        this.id = id;
        this.gridSize = gridSize;
        this.pattern = pattern;
        this.shapeless = null;
        this.outputType = outputType;
        this.outputCount = outputCount;
    }

    public Recipe(String id, int gridSize, Map<String, Integer> shapeless, BlockType outputType, int outputCount) {
        this.id = id;
        this.gridSize = gridSize;
        this.pattern = null;
        this.shapeless = shapeless;
        this.outputType = outputType;
        this.outputCount = outputCount;
    }
}
