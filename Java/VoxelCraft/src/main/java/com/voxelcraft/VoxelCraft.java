package com.voxelcraft;

import com.voxelcraft.block.BlockType;
import com.voxelcraft.game.GameSession;
import com.voxelcraft.mode.GameMode;
import com.voxelcraft.render.Camera;
import com.voxelcraft.render.ShapeRenderer;
import com.voxelcraft.render.ShaderProgram;
import com.voxelcraft.render.VoxelMeshBuilder.ChunkMesh;
import com.voxelcraft.render.Window;
import com.voxelcraft.ui.CraftingGrid;
import com.voxelcraft.ui.GameHud;
import com.voxelcraft.ui.LoadingScreen;
import com.voxelcraft.ui.ModeMenuScreen;
import com.voxelcraft.ui.OverlayScreen;
import com.voxelcraft.ui.UiRenderer;
import com.voxelcraft.world.BlockRaycaster;
import com.voxelcraft.world.BlockRaycaster.Hit;
import org.joml.Matrix4f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class VoxelCraft {
    private enum Screen { MODE_MENU, OVERLAY, PLAYING, INVENTORY, WIN }

    private Window window;
    private ShaderProgram worldShader;
    private ShapeRenderer shapeRenderer;
    private UiRenderer uiRenderer;
    private LoadingScreen loadingScreen;
    private ModeMenuScreen modeMenu;
    private OverlayScreen overlayScreen;
    private GameHud gameHud;
    private CraftingGrid craftingGrid;

    private Screen screen = Screen.MODE_MENU;
    private GameSession session;
    private Hit currentHit;
    private double lastMouseX;
    private double lastMouseY;

    public void run() {
        System.out.println("[VoxelCraft] Starting LWJGL application...");
        System.out.println("[VoxelCraft] Step 1/7: creating window...");
        window = new Window(1280, 720, "VoxelCraft");
        System.out.println("[VoxelCraft] Step 2/7: loading world shader...");
        worldShader = new ShaderProgram("shaders/vertex.glsl", "shaders/fragment.glsl");
        System.out.println("[VoxelCraft] Step 3/7: creating shape renderer...");
        shapeRenderer = new ShapeRenderer();
        System.out.println("[VoxelCraft] Step 4/7: creating UI renderer...");
        uiRenderer = new UiRenderer();
        System.out.println("[VoxelCraft] Step 5/7: creating screens...");
        loadingScreen = new LoadingScreen(uiRenderer);
        modeMenu = new ModeMenuScreen(uiRenderer);
        overlayScreen = new OverlayScreen(uiRenderer);
        gameHud = new GameHud(uiRenderer);
        craftingGrid = new CraftingGrid();

        System.out.println("[VoxelCraft] Step 6/7: drawing first loading frame...");
        renderLoading("Voxel Craft", "Подготовка меню...", 0.85f);
        System.out.println("[VoxelCraft] Step 7/7: installing input callbacks...");
        setupInput();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glClearColor(0.10f, 0.10f, 0.18f, 1f);
        System.out.println("[VoxelCraft] Ready: mode menu is visible. If you see blue, press Esc to return to menu.");

        double lastTime = glfwGetTime();
        while (!window.shouldClose()) {
            double now = glfwGetTime();
            double dt = Math.min(now - lastTime, 0.05);
            lastTime = now;

            update(dt);
            render();

            window.swapBuffers();
            window.pollEvents();
        }

        cleanup();
    }

    private void setupInput() {
        window.setInputCallbacks(
            (key, action) -> {
                if (action != GLFW_PRESS && action != GLFW_REPEAT) return;

                if (key == GLFW_KEY_ESCAPE) {
                    if (screen == Screen.INVENTORY) {
                        closeInventory();
                    } else if (screen == Screen.PLAYING || screen == Screen.OVERLAY || screen == Screen.WIN) {
                        showModeMenu();
                    }
                    return;
                }

                if (session == null || screen != Screen.PLAYING) return;

                boolean pressed = action == GLFW_PRESS || action == GLFW_REPEAT;
                switch (key) {
                    case GLFW_KEY_W -> session.controller.forward = pressed;
                    case GLFW_KEY_S -> session.controller.backward = pressed;
                    case GLFW_KEY_A -> session.controller.left = pressed;
                    case GLFW_KEY_D -> session.controller.right = pressed;
                    case GLFW_KEY_SPACE -> session.controller.jumping = pressed;
                    case GLFW_KEY_LEFT_SHIFT -> session.controller.sprinting = pressed;
                    case GLFW_KEY_E -> {
                        if (pressed) openInventory();
                    }
                    case GLFW_KEY_1 -> session.inventory.selectSlot(0);
                    case GLFW_KEY_2 -> session.inventory.selectSlot(1);
                    case GLFW_KEY_3 -> session.inventory.selectSlot(2);
                    case GLFW_KEY_4 -> session.inventory.selectSlot(3);
                    case GLFW_KEY_5 -> session.inventory.selectSlot(4);
                    case GLFW_KEY_6 -> session.inventory.selectSlot(5);
                    case GLFW_KEY_7 -> session.inventory.selectSlot(6);
                    case GLFW_KEY_8 -> session.inventory.selectSlot(7);
                    case GLFW_KEY_9 -> session.inventory.selectSlot(8);
                    default -> { }
                }
            },
            key -> {
                if (session == null || screen != Screen.PLAYING) return;
                switch (key) {
                    case GLFW_KEY_W -> session.controller.forward = false;
                    case GLFW_KEY_S -> session.controller.backward = false;
                    case GLFW_KEY_A -> session.controller.left = false;
                    case GLFW_KEY_D -> session.controller.right = false;
                    case GLFW_KEY_SPACE -> session.controller.jumping = false;
                    case GLFW_KEY_LEFT_SHIFT -> session.controller.sprinting = false;
                    default -> { }
                }
            }
        );

        window.setMouseButtonCallback((button, action) -> {
            if (action != GLFW_PRESS) return;

            if (screen == Screen.MODE_MENU && button == GLFW_MOUSE_BUTTON_1) {
                GameMode picked = modeMenu.pickMode(
                    window.getMouseX(), window.getMouseY(),
                    window.getWidth(), window.getHeight());
                if (picked != null) startMode(picked);
                return;
            }

            if (screen == Screen.OVERLAY && button == GLFW_MOUSE_BUTTON_1) {
                beginGameplay();
                return;
            }

            if (screen == Screen.WIN && button == GLFW_MOUSE_BUTTON_1) {
                handleWinClick();
                return;
            }

            if (screen == Screen.INVENTORY) {
                handleInventoryClick(button);
                return;
            }

            if (screen != Screen.PLAYING || !session.controller.isLocked) return;

            if (button == GLFW_MOUSE_BUTTON_1) {
                tryBreakBlock();
            } else if (button == GLFW_MOUSE_BUTTON_2) {
                tryPlaceBlock();
            }
        });

        window.setScrollCallback((dy) -> {
            if (screen == Screen.PLAYING && session.controller.isLocked) {
                session.inventory.cycleSelection(dy > 0 ? 1 : -1);
            }
        });
    }

    private void startMode(GameMode mode) {
        System.out.println("[VoxelCraft] Loading mode: " + mode.id);
        renderLoading("Загрузка мира", "Создание мира: " + mode.id, 0.20f);
        disposeSession();
        renderLoading("Загрузка мира", "Генерация чанков и spawn...", 0.45f);
        session = new GameSession(mode);
        craftingGrid.clearIntoInventory(session.inventory);
        renderLoading("Загрузка мира", "Построение mesh-геометрии...", 0.75f);
        session.updateMeshesIfNeeded();
        screen = Screen.OVERLAY;
        window.setCursorDisabled(false);
        glClearColor(0.53f, 0.81f, 0.92f, 1f);
        renderLoading("Загрузка мира", "Готово", 1.0f);
        System.out.println("[VoxelCraft] Mode ready: " + mode.id + ". Click the overlay to start.");
    }

    private void beginGameplay() {
        if (session == null) return;
        screen = Screen.PLAYING;
        window.setCursorDisabled(true);
        session.controller.isLocked = true;
        lastMouseX = window.getMouseX();
        lastMouseY = window.getMouseY();
    }

    private void openInventory() {
        if (session == null || screen != Screen.PLAYING) return;
        screen = Screen.INVENTORY;
        window.setCursorDisabled(false);
        session.controller.isLocked = false;
    }

    private void closeInventory() {
        if (session == null) return;
        screen = Screen.PLAYING;
        window.setCursorDisabled(true);
        session.controller.isLocked = true;
        lastMouseX = window.getMouseX();
        lastMouseY = window.getMouseY();
    }

    private void showModeMenu() {
        disposeSession();
        screen = Screen.MODE_MENU;
        window.setCursorDisabled(false);
        glClearColor(0.10f, 0.10f, 0.18f, 1f);
        System.out.println("[VoxelCraft] Returned to mode menu.");
    }

    private void disposeSession() {
        if (session != null) {
            craftingGrid.clearIntoInventory(session.inventory);
            session.dispose();
            session = null;
        }
    }

    private void update(double dt) {
        if (session == null) return;

        if (screen == Screen.PLAYING) {
            double mx = window.getMouseX();
            double my = window.getMouseY();
            double dx = mx - lastMouseX;
            double dy = my - lastMouseY;
            if (session.controller.isLocked && (dx != 0 || dy != 0)) {
                session.controller.addYaw(-dx);
                session.controller.addPitch(-dy);
            }
            lastMouseX = mx;
            lastMouseY = my;

            session.update(dt);
            session.updateMeshesIfNeeded();
            currentHit = BlockRaycaster.pick(session.controller, session.world);

            if (session.mode == GameMode.MAZE && session.checkWin()) {
                screen = Screen.WIN;
                window.setCursorDisabled(false);
                session.controller.isLocked = false;
            }
        }
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (session != null && screen != Screen.MODE_MENU) {
            renderWorld();
        }

        switch (screen) {
            case MODE_MENU -> modeMenu.render(window.getWidth(), window.getHeight());
            case OVERLAY -> overlayScreen.render(window.getWidth(), window.getHeight(), session.mode);
            case PLAYING -> gameHud.renderCrosshairAndHotbar(window.getWidth(), window.getHeight(), session.inventory);
            case INVENTORY -> gameHud.renderInventory(window.getWidth(), window.getHeight(), session.inventory, craftingGrid);
            case WIN -> renderWinScreen();
            default -> { }
        }
    }

    private void renderLoading(String title, String detail, float progress) {
        if (loadingScreen == null || window == null) return;
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        loadingScreen.render(window.getWidth(), window.getHeight(), title, detail, progress);
        window.swapBuffers();
        window.pollEvents();
        System.out.println("[VoxelCraft] " + title + " - " + detail + " (" + Math.round(progress * 100) + "%)");
    }

    private void renderWorld() {
        worldShader.use();
        Matrix4f projection = Camera.projection(window.getWidth(), window.getHeight());
        Matrix4f view = Camera.view(session.controller);
        worldShader.setProjection(projection);
        worldShader.setView(view);

        for (ChunkMesh mesh : session.chunkMeshes.values()) {
            if (!mesh.isValid()) continue;
            glBindVertexArray(mesh.vao());
            glDrawElements(GL_TRIANGLES, mesh.indexCount(), GL_UNSIGNED_INT, 0);
        }
        glBindVertexArray(0);

        shapeRenderer.begin(projection, view);
        if (session.mode == GameMode.FREE) {
            for (var mob : session.mobManager.mobs()) {
                shapeRenderer.drawMob(mob);
            }
        }
        if ((screen == Screen.PLAYING || screen == Screen.INVENTORY) && currentHit != null) {
            shapeRenderer.drawWireBlock(currentHit.x(), currentHit.y(), currentHit.z());
        }
        shapeRenderer.end();
    }

    private void renderWinScreen() {
        uiRenderer.begin(window.getWidth(), window.getHeight());
        uiRenderer.fillRect(0, 0, window.getWidth(), window.getHeight(), 0f, 0f, 0f, 0.55f);
        float x = window.getWidth() * 0.32f;
        float y = window.getHeight() * 0.32f;
        uiRenderer.drawText("win-title", "Лабиринт пройден!", x, y, 34, java.awt.Color.WHITE);
        uiRenderer.drawText("win-sub", "Вы нашли выход.", x, y + 48, 18, java.awt.Color.LIGHT_GRAY);
        uiRenderer.fillRect(x, y + 90, 150, 38, 0.20f, 0.42f, 0.18f, 0.95f);
        uiRenderer.drawText("win-retry", "Играть снова", x + 18, y + 100, 16, java.awt.Color.WHITE);
        uiRenderer.fillRect(x + 168, y + 90, 170, 38, 0.18f, 0.26f, 0.36f, 0.95f);
        uiRenderer.drawText("win-menu", "Меню режимов", x + 186, y + 100, 16, java.awt.Color.WHITE);
        uiRenderer.end();
    }

    private void tryBreakBlock() {
        Hit hit = currentHit != null ? currentHit : BlockRaycaster.pick(session.controller, session.world);
        if (hit == null) return;
        BlockType removed = session.world.removeBlock(hit.x(), hit.y(), hit.z());
        if (removed != null) {
            session.inventory.addItem(removed, 1);
            session.markMeshesDirty();
            currentHit = null;
        }
    }

    private void tryPlaceBlock() {
        if (!session.inventory.hasSelected()) return;
        Hit hit = currentHit != null ? currentHit : BlockRaycaster.pick(session.controller, session.world);
        if (hit == null) return;
        int x = hit.placeX();
        int y = hit.placeY();
        int z = hit.placeZ();
        if (blocksPlayerPlacement(x, y, z)) return;
        if (session.world.placeBlock(x, y, z, session.inventory.getSelectedItem().type)) {
            session.inventory.consumeSelected(1);
            session.markMeshesDirty();
            currentHit = null;
        }
    }

    private boolean blocksPlayerPlacement(int x, int y, int z) {
        double feetY = session.controller.getEyeY() - com.voxelcraft.control.FirstPersonController.PLAYER_HEIGHT;
        double headY = session.controller.getEyeY();
        double radius = com.voxelcraft.control.FirstPersonController.PLAYER_RADIUS;
        boolean overlaps = session.controller.getEyeX() + radius > x
            && session.controller.getEyeX() - radius < x + 1
            && headY > y
            && feetY < y + 1
            && session.controller.getEyeZ() + radius > z
            && session.controller.getEyeZ() - radius < z + 1;
        if (!overlaps) return false;
        return y + 1 > feetY + 0.2;
    }

    private void handleInventoryClick(int button) {
        int width = window.getWidth();
        int height = window.getHeight();
        float panelW = Math.min(860, width - 48);
        float panelH = Math.min(560, height - 48);
        float x = (width - panelW) / 2f;
        float y = (height - panelH) / 2f;
        float mx = (float) window.getMouseX();
        float my = (float) window.getMouseY();

        float buttonY = y + panelH - 58;
        if (button == GLFW_MOUSE_BUTTON_1 && UiRenderer.hitRect(mx, my, x + 24, buttonY, 220, 34)) {
            closeInventory();
            return;
        }
        if (button == GLFW_MOUSE_BUTTON_1 && UiRenderer.hitRect(mx, my, x + 260, buttonY, 160, 34)) {
            showModeMenu();
            return;
        }

        if (my >= y + 92 && my <= y + 122) {
            if (mx >= x + 20 && mx < x + 82) craftingGrid.setSize(2, session.inventory);
            if (mx >= x + 88 && mx < x + 150) craftingGrid.setSize(3, session.inventory);
        }

        float cell = 46;
        float gap = 6;
        float gridX = x + 24;
        float gridY = y + 132;
        for (int i = 0; i < craftingGrid.size() * craftingGrid.size(); i++) {
            float sx = gridX + (i % craftingGrid.size()) * (cell + gap);
            float sy = gridY + (i / craftingGrid.size()) * (cell + gap);
            if (UiRenderer.hitRect(mx, my, sx, sy, cell, cell)) {
                if (button == GLFW_MOUSE_BUTTON_1) craftingGrid.putFromHotbar(session.inventory);
                if (button == GLFW_MOUSE_BUTTON_2) craftingGrid.returnLastToInventory(session.inventory);
                return;
            }
        }
        if (UiRenderer.hitRect(mx, my, gridX + 200, gridY + 28, cell, cell)) {
            craftingGrid.craft(session.inventory);
        }
    }

    private void handleWinClick() {
        if (session == null) {
            showModeMenu();
            return;
        }
        float x = window.getWidth() * 0.32f;
        float y = window.getHeight() * 0.32f;
        float mx = (float) window.getMouseX();
        float my = (float) window.getMouseY();
        GameMode mode = session.mode;
        if (UiRenderer.hitRect(mx, my, x, y + 90, 150, 38)) {
            startMode(mode);
            return;
        }
        if (UiRenderer.hitRect(mx, my, x + 168, y + 90, 170, 38)) {
            showModeMenu();
        }
    }

    private void cleanup() {
        disposeSession();
        shapeRenderer.cleanup();
        uiRenderer.cleanup();
        worldShader.cleanup();
        window.cleanup();
    }

    public static void main(String[] args) {
        new VoxelCraft().run();
    }
}
