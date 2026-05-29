package com.voxelcraft.block;

public class Block {
    public final BlockPos pos;
    public final BlockType type;

    public Block(BlockPos pos, BlockType type) {
        this.pos = pos;
        this.type = type;
    }

    public Block(int x, int y, int z, BlockType type) {
        this(new BlockPos(x, y, z), type);
    }

    public int x() { return pos.x; }
    public int y() { return pos.y; }
    public int z() { return pos.z; }
}
