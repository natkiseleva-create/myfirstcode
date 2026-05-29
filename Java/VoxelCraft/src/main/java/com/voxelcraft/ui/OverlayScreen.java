package com.voxelcraft.ui;

import com.voxelcraft.mode.GameMode;

import java.awt.*;

public class OverlayScreen {
    private final UiRenderer ui;

    public OverlayScreen(UiRenderer ui) {
        this.ui = ui;
    }

    public void render(int width, int height, GameMode mode) {
        ui.begin(width, height);
        ui.fillRect(0, 0, width, height, 0f, 0f, 0f, 0.45f);

        float panelW = Math.min(560, width - 48);
        float panelH = 320;
        float panelX = (width - panelW) / 2f;
        float panelY = (height - panelH) / 2f;
        ui.fillRect(panelX, panelY, panelW, panelH, 0.08f, 0.08f, 0.12f, 0.92f);

        String hint = mode == GameMode.MAZE
            ? "Найдите золотой столб — это выход из лабиринта. Esc — в меню."
            : "Холмы, реки, озёра. Исследуйте мир. Esc — в меню.";

        ui.drawText("overlay-title", "Кликните, чтобы начать", panelX + 24, panelY + 28, 28, Color.WHITE);
        ui.drawText("overlay-hint", hint, panelX + 24, panelY + 72, 16, new Color(210, 210, 220));
        ui.drawText("overlay-controls", "WASD — движение · Пробел — прыжок · Shift — бег", panelX + 24, panelY + 110, 15, new Color(190, 190, 200));
        ui.drawText("overlay-controls2", "Мышь — обзор · ЛКМ/ПКМ — сломать/поставить · 1-9 — хотбар", panelX + 24, panelY + 132, 15, new Color(190, 190, 200));
        ui.drawText("overlay-controls3", "E — инвентарь · Esc — меню режимов", panelX + 24, panelY + 154, 15, new Color(190, 190, 200));
        ui.end();
    }
}
