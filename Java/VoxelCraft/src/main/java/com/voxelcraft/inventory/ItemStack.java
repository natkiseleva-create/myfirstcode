package com.voxelcraft.inventory;

import com.voxelcraft.block.BlockType;

public class ItemStack {
    public final BlockType type;
    public int count;

    public ItemStack(BlockType type, int count) {
        this.type = type;
        this.count = count;
    }

    public boolean isEmpty() {
        return type == null || count <= 0;
    }

    public int maxStack() {
        return type != null ? type.stackSize : 0;
    }

    public ItemStack copy() {
        return new ItemStack(type, count);
    }
}
