package com.voxelcraft.mode;

public enum GameMode {
    FREE("free"),
    MAZE("maze");

    public final String id;
    GameMode(String id) { this.id = id; }

    public static GameMode fromId(String id) {
        for (GameMode m : values()) {
            if (m.id.equals(id)) return m;
        }
        return FREE;
    }
}
