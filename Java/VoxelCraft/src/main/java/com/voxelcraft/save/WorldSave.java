package com.voxelcraft.save;

import com.voxelcraft.inventory.ItemStack;
import com.voxelcraft.mode.GameMode;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorldSave {
    public static final int FORMAT_VERSION = 1;

    public int version = FORMAT_VERSION;
    public String worldName = "default";
    public GameMode mode = GameMode.FREE;
    public int seed = 12345;
    public double playerX;
    public double playerY;
    public double playerZ;
    public double yaw;
    public double pitch;
    public int selectedSlot;
    public ItemStack[] inventory;
    public final Map<String, String> modifications = new LinkedHashMap<>();

    public WorldSave() {
    }

    public WorldSave(String worldName, GameMode mode, int seed) {
        this.worldName = worldName;
        this.mode = mode;
        this.seed = seed;
    }
}
