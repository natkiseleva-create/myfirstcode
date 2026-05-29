package com.voxelcraft.ui;

import com.voxelcraft.mode.GameMode;

import java.awt.*;

public class ModeMenuScreen {
    private final UiRenderer ui;

    public ModeMenuScreen(UiRenderer ui) {
        this.ui = ui;
    }

    public void render(int width, int height) {
        ui.begin(width, height);
        ui.fillRect(0, 0, width, height, 0.10f, 0.10f, 0.18f, 1f);

        float panelW = Math.min(520, width - 40);
        float panelX = (width - panelW) / 2f;
        float y = height * 0.18f;

        ui.drawText("title", "Voxel Craft", panelX, y, 42, Color.WHITE);
        ui.drawText("subtitle", "Выберите режим игры", panelX, y + 52, 20, new Color(210, 210, 220));
        ui.fillRect(panelX, y, 220, 10, 0.30f, 0.62f, 0.95f, 1f);
        ui.fillRect(panelX, y + 22, 320, 8, 0.82f, 0.86f, 0.92f, 1f);

        float btnH = 72;
        float btnY = y + 110;
        drawButton(panelX, btnY, panelW, btnH, "Свободный мир",
            "Бесконечная генерация, деревья, строительство", 0.18f, 0.45f, 0.78f);

        btnY += btnH + 16;
        drawButton(panelX, btnY, panelW, btnH, "Лабиринт",
            "Найдите золотой выход из каменного лабиринта", 0.78f, 0.55f, 0.18f);
        ui.drawText("lan-help", "LAN: H — создать сервер, J — подключиться к -Dvoxelcraft.joinHost=IP",
            panelX, btnY + btnH + 28, 15, new Color(210, 210, 220));
        ui.end();
    }

    private void drawButton(float x, float y, float w, float h, String title, String desc,
                            float r, float g, float b) {
        ui.fillRect(x, y, w, h, r * 0.35f, g * 0.35f, b * 0.35f, 0.95f);
        ui.fillRect(x + 2, y + 2, w - 4, h - 4, r * 0.55f, g * 0.55f, b * 0.55f, 0.35f);
        ui.fillRect(x + 18, y + 20, w * 0.38f, 12, r, g, b, 1f);
        ui.fillRect(x + 18, y + 46, w * 0.72f, 8, 0.82f, 0.86f, 0.92f, 0.9f);
        ui.drawText("btn-" + title, title, x + 18, y + 18, 24, Color.WHITE);
        ui.drawText("desc-" + title, desc, x + 18, y + 46, 16, new Color(230, 230, 235));
    }

    public GameMode pickMode(double mouseX, double mouseY, int width, int height) {
        float panelW = Math.min(520, width - 40);
        float panelX = (width - panelW) / 2f;
        float y = height * 0.18f + 110;
        float btnH = 72;

        if (UiRenderer.hitRect((float) mouseX, (float) mouseY, panelX, y, panelW, btnH)) {
            return GameMode.FREE;
        }
        y += btnH + 16;
        if (UiRenderer.hitRect((float) mouseX, (float) mouseY, panelX, y, panelW, btnH)) {
            return GameMode.MAZE;
        }
        return null;
    }
}
