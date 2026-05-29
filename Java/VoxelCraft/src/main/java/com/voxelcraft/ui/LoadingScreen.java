package com.voxelcraft.ui;

public class LoadingScreen {
    private final UiRenderer ui;

    public LoadingScreen(UiRenderer ui) {
        this.ui = ui;
    }

    public void render(int width, int height, String title, String detail, float progress) {
        ui.begin(width, height);
        ui.fillRect(0, 0, width, height, 0.08f, 0.10f, 0.16f, 1f);

        float panelW = Math.min(560, width - 48);
        float panelX = (width - panelW) / 2f;
        float panelY = height * 0.38f;
        float barW = panelW;
        float barH = 18;

        // Text rendering is optional because AWT can conflict with LWJGL on macOS.
        // The progress bar is always drawn with pure OpenGL rectangles.
        ui.fillRect(panelX, panelY - 58, panelW * 0.46f, 24, 0.30f, 0.62f, 0.95f, 1f);
        ui.fillRect(panelX, panelY - 26, panelW * 0.72f, 12, 0.55f, 0.62f, 0.72f, 1f);
        ui.fillRect(panelX, panelY + 16, barW, barH, 0.18f, 0.20f, 0.26f, 1f);
        ui.fillRect(panelX + 2, panelY + 18, Math.max(0, (barW - 4) * clamp(progress)), barH - 4, 0.30f, 0.62f, 0.95f, 1f);
        ui.end();
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
