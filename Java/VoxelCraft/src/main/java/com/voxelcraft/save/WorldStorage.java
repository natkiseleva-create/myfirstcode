package com.voxelcraft.save;

import com.voxelcraft.block.BlockType;
import com.voxelcraft.inventory.Inventory;
import com.voxelcraft.inventory.ItemStack;
import com.voxelcraft.mode.GameMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class WorldStorage {
    private static final Path SAVE_ROOT = Path.of("saves");
    private static final String FILE_NAME = "world.properties";

    private WorldStorage() {
    }

    public static Path worldPath(String worldName) {
        return SAVE_ROOT.resolve(safeName(worldName)).resolve(FILE_NAME);
    }

    public static boolean exists(String worldName) {
        return Files.isRegularFile(worldPath(worldName));
    }

    public static WorldSave load(String worldName) {
        Path path = worldPath(worldName);
        if (!Files.isRegularFile(path)) {
            return null;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load world save: " + path, e);
        }

        WorldSave save = new WorldSave();
        save.worldName = props.getProperty("worldName", worldName);
        save.version = parseInt(props.getProperty("version"), WorldSave.FORMAT_VERSION);
        save.mode = GameMode.fromId(props.getProperty("mode", GameMode.FREE.id));
        save.seed = parseInt(props.getProperty("seed"), 12345);
        save.playerX = parseDouble(props.getProperty("player.x"), 0);
        save.playerY = parseDouble(props.getProperty("player.y"), 0);
        save.playerZ = parseDouble(props.getProperty("player.z"), 0);
        save.yaw = parseDouble(props.getProperty("player.yaw"), 0);
        save.pitch = parseDouble(props.getProperty("player.pitch"), 0);
        save.selectedSlot = parseInt(props.getProperty("inventory.selected"), 0);
        save.inventory = readInventory(props);

        int modCount = parseInt(props.getProperty("mod.count"), 0);
        for (int i = 0; i < modCount; i++) {
            String key = props.getProperty("mod." + i + ".key");
            String type = props.getProperty("mod." + i + ".type");
            if (key != null && type != null) {
                save.modifications.put(key, type);
            }
        }
        return save;
    }

    public static void save(WorldSave save) {
        Path path = worldPath(save.worldName);
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create save directory: " + path.getParent(), e);
        }

        Properties props = new Properties();
        props.setProperty("version", Integer.toString(WorldSave.FORMAT_VERSION));
        props.setProperty("worldName", save.worldName);
        props.setProperty("mode", save.mode.id);
        props.setProperty("seed", Integer.toString(save.seed));
        props.setProperty("player.x", Double.toString(save.playerX));
        props.setProperty("player.y", Double.toString(save.playerY));
        props.setProperty("player.z", Double.toString(save.playerZ));
        props.setProperty("player.yaw", Double.toString(save.yaw));
        props.setProperty("player.pitch", Double.toString(save.pitch));
        props.setProperty("inventory.selected", Integer.toString(save.selectedSlot));
        writeInventory(props, save.inventory);

        int index = 0;
        for (Map.Entry<String, String> entry : save.modifications.entrySet()) {
            props.setProperty("mod." + index + ".key", entry.getKey());
            props.setProperty("mod." + index + ".type", entry.getValue());
            index++;
        }
        props.setProperty("mod.count", Integer.toString(index));

        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, "VoxelCraft world save");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save world: " + path, e);
        }
    }

    private static ItemStack[] readInventory(Properties props) {
        ItemStack[] slots = new ItemStack[Inventory.TOTAL_SLOTS];
        for (int i = 0; i < slots.length; i++) {
            String typeId = props.getProperty("inventory." + i + ".type", "");
            int count = parseInt(props.getProperty("inventory." + i + ".count"), 0);
            BlockType type = typeId.isBlank() ? null : BlockType.fromId(typeId);
            slots[i] = new ItemStack(type, type == null ? 0 : count);
        }
        return slots;
    }

    private static void writeInventory(Properties props, ItemStack[] slots) {
        if (slots == null) return;
        for (int i = 0; i < Math.min(slots.length, Inventory.TOTAL_SLOTS); i++) {
            ItemStack stack = slots[i];
            if (stack == null || stack.isEmpty()) {
                props.setProperty("inventory." + i + ".type", "");
                props.setProperty("inventory." + i + ".count", "0");
            } else {
                props.setProperty("inventory." + i + ".type", stack.type.id);
                props.setProperty("inventory." + i + ".count", Integer.toString(stack.count));
            }
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return value == null ? fallback : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String safeName(String name) {
        return name == null || name.isBlank()
            ? "default"
            : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
