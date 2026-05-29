package com.voxelcraft.block;

public enum BlockType {
    GRASS("grass", "Трава", 0x6ecf4a, 0x5cb848, 64, false),
    DIRT("dirt", "Земля", 0x6b4423, 0x5a3820, 64, false),
    STONE("stone", "Камень", 0x888888, 0x777777, 64, false),
    WOOD("wood", "Дерево", 0x8b6914, 0x5c3d1e, 64, false),
    PLANKS("planks", "Доски", 0xc9a86c, 0xa08050, 64, false),
    CRAFTING_TABLE("crafting_table", "Верстак", 0xb8885a, 0x8b5a2b, 64, false),
    LEAVES("leaves", "Листва", 0x3d8b37, 0x2d6b28, 64, false),
    WATER("water", "Вода", 0x3a8fd4, 0x2a6fa8, 64, true),
    SAND("sand", "Песок", 0xd4c48a, 0xc4b47a, 64, false),
    GOLD("gold", "Выход", 0xffd54f, 0xffb300, 64, false);

    public final String id;
    public final String label;
    public final int topColor;
    public final int sideColor;
    public final int stackSize;
    public final boolean transparent;

    BlockType(String id, String label, int topColor, int sideColor, int stackSize, boolean transparent) {
        this.id = id;
        this.label = label;
        this.topColor = topColor;
        this.sideColor = sideColor;
        this.stackSize = stackSize;
        this.transparent = transparent;
    }

    public static BlockType fromId(String id) {
        for (BlockType t : values()) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }
}
