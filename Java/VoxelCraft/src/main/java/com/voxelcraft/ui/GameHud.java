package com.voxelcraft.ui;

import com.voxelcraft.block.BlockType;
import com.voxelcraft.inventory.Inventory;
import com.voxelcraft.inventory.ItemStack;

import java.awt.Color;

public class GameHud {
    private final UiRenderer ui;

    public GameHud(UiRenderer ui) {
        this.ui = ui;
    }

    public void renderCrosshairAndHotbar(int width, int height, Inventory inventory) {
        ui.begin(width, height);
        float cx = width / 2f;
        float cy = height / 2f;
        ui.fillRect(cx - 1, cy - 8, 2, 16, 1, 1, 1, 0.9f);
        ui.fillRect(cx - 8, cy - 1, 16, 2, 1, 1, 1, 0.9f);
        renderHotbar(width, height, inventory, height - 68);
        ui.end();
    }

    public void renderInventory(int width, int height, Inventory inventory, CraftingGrid crafting) {
        ui.begin(width, height);
        ui.fillRect(0, 0, width, height, 0, 0, 0, 0.55f);
        float panelW = Math.min(860, width - 48);
        float panelH = Math.min(560, height - 48);
        float x = (width - panelW) / 2f;
        float y = (height - panelH) / 2f;
        ui.fillRect(x, y, panelW, panelH, 0.09f, 0.09f, 0.12f, 0.96f);

        ui.drawText("inv-title", "Инвентарь  E / У / Esc", x + 22, y + 18, 26, Color.WHITE);
        ui.drawText("craft-title", "Создание", x + 22, y + 70, 20, Color.WHITE);
        ui.drawText("craft-tabs", crafting.size() == 2 ? "[2x2]  3x3" : "2x2  [3x3]", x + 22, y + 100, 17, Color.LIGHT_GRAY);

        float cell = 46;
        float gap = 6;
        float gridX = x + 24;
        float gridY = y + 132;
        for (int i = 0; i < crafting.size() * crafting.size(); i++) {
            drawSlot(gridX + (i % crafting.size()) * (cell + gap), gridY + (i / crafting.size()) * (cell + gap),
                cell, crafting.cell(i), false);
        }
        ui.drawText("craft-arrow", "->", gridX + 150, gridY + 42, 24, Color.WHITE);
        ItemStack result = crafting.result() == null
            ? new ItemStack(null, 0)
            : new ItemStack(crafting.result().outputType(), crafting.result().outputCount());
        drawSlot(gridX + 200, gridY + 28, cell, result, false);

        ui.drawText("craft-help", "ЛКМ: положить 1 из хотбара   ПКМ: вернуть   Клик по результату: скрафтить", x + 22, y + 270, 14, Color.LIGHT_GRAY);
        ui.drawText("recipes-help", "Дерево -> доски · 4 доски -> верстак", x + 22, y + 294, 14, Color.LIGHT_GRAY);

        float storageX = x + panelW - (9 * (cell + gap)) - 22;
        float storageY = y + 86;
        for (int i = 0; i < Inventory.STORAGE_SIZE; i++) {
            drawSlot(storageX + (i % 9) * (cell + gap), storageY + (i / 9) * (cell + gap), cell,
                inventory.getSlot(Inventory.HOTBAR_SIZE + i), false);
        }
        renderHotbarAt(storageX, storageY + 3 * (cell + gap) + 18, inventory);

        float buttonY = y + panelH - 58;
        ui.fillRect(x + 24, buttonY, 220, 34, 0.18f, 0.32f, 0.20f, 0.95f);
        ui.drawText("inv-close", "Закрыть и продолжить", x + 38, buttonY + 8, 15, Color.WHITE);
        ui.fillRect(x + 260, buttonY, 160, 34, 0.24f, 0.20f, 0.20f, 0.95f);
        ui.drawText("inv-menu", "Меню режимов", x + 278, buttonY + 8, 15, Color.WHITE);
        ui.end();
    }

    public void renderHotbar(int width, int height, Inventory inventory, float y) {
        float slot = 46;
        float gap = 5;
        float total = Inventory.HOTBAR_SIZE * slot + (Inventory.HOTBAR_SIZE - 1) * gap + 8;
        float x = (width - total) / 2f + 4;
        renderHotbarAt(x, y, inventory);
    }

    private void renderHotbarAt(float x, float y, Inventory inventory) {
        float slot = 46;
        float gap = 5;
        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            drawSlot(x + i * (slot + gap), y, slot, inventory.getSlot(i), inventory.getSelectedSlot() == i);
        }
    }

    private void drawSlot(float x, float y, float size, ItemStack stack, boolean selected) {
        ui.fillRect(x - 2, y - 2, size + 4, size + 4, selected ? 1f : 0.25f, selected ? 1f : 0.25f, selected ? 1f : 0.25f, selected ? 0.95f : 0.5f);
        ui.fillRect(x, y, size, size, 0.10f, 0.10f, 0.12f, 0.9f);
        if (stack == null || stack.isEmpty()) return;
        float[] top = color(stack.type.topColor);
        float[] side = color(stack.type.sideColor);
        ui.fillRect(x + 7, y + 7, size - 14, size - 20, top[0], top[1], top[2], 1f);
        ui.fillRect(x + 7, y + size - 15, size - 14, 8, side[0], side[1], side[2], 1f);
        ui.drawText("slot-" + x + "-" + y + "-" + stack.count, String.valueOf(stack.count), x + size - 18, y + size - 18, 13, Color.WHITE);
    }

    private float[] color(int color) {
        return new float[] {
            ((color >> 16) & 0xFF) / 255f,
            ((color >> 8) & 0xFF) / 255f,
            (color & 0xFF) / 255f
        };
    }
}
