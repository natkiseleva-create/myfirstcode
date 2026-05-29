package com.voxelcraft.inventory;

import com.voxelcraft.block.BlockType;

import java.util.function.Consumer;

public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int STORAGE_SIZE = 27;
    public static final int TOTAL_SLOTS = HOTBAR_SIZE + STORAGE_SIZE;

    private final ItemStack[] slots = new ItemStack[TOTAL_SLOTS];
    private int selectedSlot = 0;
    private Consumer<Inventory> onChange;

    public Inventory() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slots[i] = new ItemStack(null, 0);
        }
        // Give starter blocks
        addItem(BlockType.DIRT, 4);
        addItem(BlockType.WOOD, 4);
        addItem(BlockType.PLANKS, 8);
        addItem(BlockType.STONE, 4);
        addItem(BlockType.CRAFTING_TABLE, 1);
    }

    public void setOnChange(Consumer<Inventory> listener) {
        this.onChange = listener;
    }

    private void notifyChange() {
        if (onChange != null) onChange.accept(this);
    }

    public ItemStack getSlot(int index) {
        return slots[index];
    }

    public ItemStack getSelectedItem() {
        return slots[selectedSlot];
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void selectSlot(int index) {
        if (index >= 0 && index < HOTBAR_SIZE) {
            selectedSlot = index;
            notifyChange();
        }
    }

    public boolean hasSelected() {
        return slots[selectedSlot].type != null && slots[selectedSlot].count > 0;
    }

    /**
     * Add items. Returns how many were added.
     */
    public int addItem(BlockType type, int count) {
        int remaining = count;

        // First try to stack onto existing
        for (int i = 0; i < TOTAL_SLOTS && remaining > 0; i++) {
            ItemStack slot = slots[i];
            if (slot.type == type && slot.count < slot.maxStack()) {
                int canAdd = Math.min(remaining, slot.maxStack() - slot.count);
                slot.count += canAdd;
                remaining -= canAdd;
            }
        }

        // Then try empty slots
        for (int i = 0; i < TOTAL_SLOTS && remaining > 0; i++) {
            ItemStack slot = slots[i];
            if (slot.type == null || slot.count <= 0) {
                int add = Math.min(remaining, type.stackSize);
                slots[i] = new ItemStack(type, add);
                remaining -= add;
            }
        }

        if (remaining < count) notifyChange();
        return count - remaining;
    }

    public boolean consumeSelected(int count) {
        ItemStack slot = slots[selectedSlot];
        if (slot.type == null || slot.count < count) return false;
        slot.count -= count;
        if (slot.count <= 0) {
            slots[selectedSlot] = new ItemStack(null, 0);
        }
        notifyChange();
        return true;
    }

    public void cycleSelection(int delta) {
        int newSlot = selectedSlot + delta;
        if (newSlot < 0) newSlot += HOTBAR_SIZE;
        if (newSlot >= HOTBAR_SIZE) newSlot -= HOTBAR_SIZE;
        selectSlot(newSlot);
    }

    public int countItem(BlockType type) {
        int total = 0;
        for (ItemStack s : slots) {
            if (s.type == type) total += s.count;
        }
        return total;
    }
}
