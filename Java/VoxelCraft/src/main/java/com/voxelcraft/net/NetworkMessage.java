package com.voxelcraft.net;

public record NetworkMessage(String type, String payload) {
    public static final String INIT = "INIT";
    public static final String BLOCK = "BLOCK";
    public static final String PLAYER = "PLAYER";

    public String encode() {
        return type + "|" + payload.replace("\n", "");
    }

    public static NetworkMessage decode(String line) {
        int split = line.indexOf('|');
        if (split < 0) return new NetworkMessage(line, "");
        return new NetworkMessage(line.substring(0, split), line.substring(split + 1));
    }
}
