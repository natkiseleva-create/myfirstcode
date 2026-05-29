package com.voxelcraft.ui;

import com.voxelcraft.block.BlockType;
import com.voxelcraft.inventory.CraftingRecipes;
import com.voxelcraft.inventory.Inventory;
import com.voxelcraft.inventory.ItemStack;

public class CraftingGrid {
    private int size = 2;
    private ItemStack[] cells = emptyCells(size);
    private CraftingRecipes.RecipeResult result;

    public int size() {
        return size;
    }

    public ItemStack cell(int index) {
        return cells[index];
    }

    public CraftingRecipes.RecipeResult result() {
        return result;
    }

    public void setSize(int size, Inventory inventory) {
        if (this.size == size) return;
        clearIntoInventory(inventory);
        this.size = size;
        cells = emptyCells(size);
        updateResult();
    }

    public void putFromHotbar(Inventory inventory) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i].isEmpty()) {
                ItemStack one = inventory.takeOneFromSlot(inventory.getSelectedSlot());
                if (!one.isEmpty()) {
                    cells[i] = one;
                    updateResult();
                }
                return;
            }
        }
    }

    public void returnLastToInventory(Inventory inventory) {
        for (int i = cells.length - 1; i >= 0; i--) {
            if (!cells[i].isEmpty()) {
                inventory.addItem(cells[i].type, 1);
                cells[i] = new ItemStack(null, 0);
                updateResult();
                return;
            }
        }
    }

    public void craft(Inventory inventory) {
        if (result == null) return;
        if (inventory.addItem(result.outputType(), result.outputCount()) != result.outputCount()) return;

        for (int i = 0; i < cells.length; i++) {
            if (cells[i].isEmpty()) continue;
            cells[i].count--;
            if (cells[i].count <= 0) cells[i] = new ItemStack(null, 0);
        }
        updateResult();
    }

    public void clearIntoInventory(Inventory inventory) {
        for (int i = 0; i < cells.length; i++) {
            if (!cells[i].isEmpty()) {
                inventory.addItem(cells[i].type, cells[i].count);
                cells[i] = new ItemStack(null, 0);
            }
        }
        updateResult();
    }

    private void updateResult() {
        String[][] ids = new String[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                ItemStack stack = cells[y * size + x];
                BlockType type = stack.type;
                ids[y][x] = stack.count > 0 && type != null ? type.id : null;
            }
        }
        result = CraftingRecipes.match(ids, size);
    }

    private static ItemStack[] emptyCells(int size) {
        ItemStack[] result = new ItemStack[size * size];
        for (int i = 0; i < result.length; i++) {
            result[i] = new ItemStack(null, 0);
        }
        return result;
    }
}
