package com.voxelcraft.save;

public record ChunkDelta(String blockKey, String typeId) {
    public boolean removed() {
        return "null".equals(typeId);
    }
}
