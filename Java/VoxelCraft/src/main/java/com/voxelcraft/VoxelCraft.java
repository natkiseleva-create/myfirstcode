package com.voxelcraft;

import com.voxelcraft.block.BlockType;
import com.voxelcraft.control.FirstPersonController;
import com.voxelcraft.inventory.CraftingRecipes;
import com.voxelcraft.inventory.Inventory;
import com.voxelcraft.math.FastNoise;
import com.voxelcraft.render.ShaderProgram;
import com.voxelcraft.render.VoxelMeshBuilder;
import com.voxelcraft.render.VoxelMeshBuilder.ChunkMesh;
import com.voxelcraft.render.Window;
import com.voxelcraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class VoxelCraft {
    private Window window;
    private ShaderProgram shader;
    private FirstPersonController controller;
    private World world;
    private Inventory inventory;
    private GameState state;
    private Map<String, ChunkMesh> chunkMeshes;
    private double lastMouseX, lastMouseY;
    private boolean cursorGrabbed = false;

    enum GameState { PLAYING, MENU }

    public void run() {
        window = new Window(1280, 720, "VoxelCraft");
        shader = new ShaderProgram("shaders/vertex.glsl", "shaders/fragment.glsl");

        world = new World();
        world.ensureLoaded(0, 0);

        inventory = new Inventory();
        state = GameState.PLAYING;
        chunkMeshes = new HashMap<>();

        // Use spawn from original JS: x=8.5, z=8.5, ground ~ 8
        controller = new FirstPersonController(world, 8.5, 8, 8.5);
        controller.isLocked = true;

        lastMouseX = window.getMouseX();
        lastMouseY = window.getMouseY();

        setupInput();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f); // Sky blue

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
        window.setKeyCallback((key, action) -> {
            boolean pressed = action == GLFW_PRESS || action == GLFW_REPEAT;

            switch (key) {
                case GLFW_KEY_W -> controller.forward = pressed;
                case GLFW_KEY_S -> controller.backward = pressed;
                case GLFW_KEY_A -> controller.left = pressed;
                case GLFW_KEY_D -> controller.right = pressed;
                case GLFW_KEY_SPACE -> controller.jumping = pressed;
                case GLFW_KEY_LEFT_SHIFT -> controller.sprinting = pressed;
                case GLFW_KEY_ESCAPE -> {
                    if (pressed) toggleMenu();
                }
                case GLFW_KEY_E -> {
                    if (pressed) toggleInventory();
                }
                case GLFW_KEY_1 -> inventory.selectSlot(0);
                case GLFW_KEY_2 -> inventory.selectSlot(1);
                case GLFW_KEY_3 -> inventory.selectSlot(2);
                case GLFW_KEY_4 -> inventory.selectSlot(3);
                case GLFW_KEY_5 -> inventory.selectSlot(4);
                case GLFW_KEY_6 -> inventory.selectSlot(5);
                case GLFW_KEY_7 -> inventory.selectSlot(6);
                case GLFW_KEY_8 -> inventory.selectSlot(7);
                case GLFW_KEY_9 -> inventory.selectSlot(8);
            }
        });

        window.setMouseButtonCallback((button, action) -> {
            if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
                // Break block
                int bx = (int) Math.floor(controller.getEyeX());
                int by = (int) Math.floor(controller.getEyeY());
                int bz = (int) Math.floor(controller.getEyeZ());
                // Simplified: raycast forward 5 blocks
                for (int i = 0; i < 5; i++) {
                    double dx = Math.sin(controller.yaw);
                    double dz = Math.cos(controller.yaw);
                    bx = (int) Math.floor(controller.getEyeX() + dx * (i + 1));
                    by = (int) Math.floor(controller.getEyeY() - 0.3 + Math.sin(controller.pitch) * (i + 1));
                    bz = (int) Math.floor(controller.getEyeZ() + dz * (i + 1));
                    BlockType removed = world.removeBlock(bx, by, bz);
                    if (removed != null) {
                        inventory.addItem(removed, 1);
                        rebuildMeshes();
                        break;
                    }
                }
            } else if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_2) {
                // Place block
                if (!inventory.hasSelected()) return;
                int bx = (int) Math.floor(controller.getEyeX());
                int by = (int) Math.floor(controller.getEyeY());
                int bz = (int) Math.floor(controller.getEyeZ());
                for (int i = 0; i < 5; i++) {
                    double dx = Math.sin(controller.yaw);
                    double dz = Math.cos(controller.yaw);
                    bx = (int) Math.floor(controller.getEyeX() + dx * (i + 1));
                    by = (int) Math.floor(controller.getEyeY() - 0.3 + Math.sin(controller.pitch) * (i + 1));
                    bz = (int) Math.floor(controller.getEyeZ() + dz * (i + 1));
                    if (world.collides(bx, by, bz)) {
                        // The block before this one in line of sight
                        double prevDx = Math.sin(controller.yaw) * i;
                        double prevDz = Math.cos(controller.yaw) * i;
                        int placeX = (int) Math.floor(controller.getEyeX() + prevDx);
                        int placeY = (int) Math.floor(controller.getEyeY() - 0.3 + Math.sin(controller.pitch) * i);
                        int placeZ = (int) Math.floor(controller.getEyeZ() + prevDz);
                        if (world.placeBlock(placeX, placeY, placeZ, inventory.getSelectedItem().type)) {
                            inventory.consumeSelected(1);
                            rebuildMeshes();
                        }
                        break;
                    }
                }
            }
        });

        window.setScrollCallback((dy) -> {
            if (state == GameState.PLAYING) {
                inventory.cycleSelection(dy > 0 ? 1 : -1);
            }
        });
    }

    private void toggleMenu() {
        if (state == GameState.MENU) {
            state = GameState.PLAYING;
            window.setCursorDisabled(true);
            controller.isLocked = true;
        } else {
            state = GameState.MENU;
            window.setCursorDisabled(false);
            controller.isLocked = false;
        }
    }

    private void toggleInventory() {
        System.out.println("Inventory toggle (keyboard E)");
        // Inventory UI would need a full GUI system - for v1, log to console
    }

    private void update(double dt) {
        if (state == GameState.PLAYING) {
            // Mouse look
            double mx = window.getMouseX();
            double my = window.getMouseY();
            double dx = mx - lastMouseX;
            double dy = my - lastMouseY;

            if (dx != 0 || dy != 0) {
                controller.addYaw(dx);
                controller.addPitch(-dy);
            }

            lastMouseX = mx;
            lastMouseY = my;

            controller.update(dt);
            rebuildMeshes();
        }
    }

    private void rebuildMeshes() {
        // Clean old meshes
        for (ChunkMesh mesh : chunkMeshes.values()) {
            mesh.cleanup();
        }
        chunkMeshes.clear();

        // Rebuild all loaded chunks
        int pcx = Math.floorDiv((int) controller.getEyeX(), 16);
        int pcz = Math.floorDiv((int) controller.getEyeZ(), 16);

        for (int dx = -World.VIEW_DISTANCE; dx <= World.VIEW_DISTANCE; dx++) {
            for (int dz = -World.VIEW_DISTANCE; dz <= World.VIEW_DISTANCE; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                String key = cx + "," + cz;
                ChunkMesh mesh = VoxelMeshBuilder.buildMesh(world, cx, cz);
                if (mesh.isValid()) {
                    chunkMeshes.put(key, mesh);
                }
            }
        }
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.use();

        // Projection
        float aspect = (float) window.getWidth() / window.getHeight();
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(75), aspect, 0.1f, 250.0f);
        shader.setProjection(projection);

        // View
        Matrix4f view = new Matrix4f();
        view.rotate((float) controller.pitch, new Vector3f(1, 0, 0));
        view.rotate((float) controller.yaw, new Vector3f(0, 1, 0));
        view.translate(new Vector3f(
            (float) -controller.getEyeX(),
            (float) -controller.getEyeY(),
            (float) -controller.getEyeZ()));
        shader.setView(view);

        // Draw all chunk meshes
        for (ChunkMesh mesh : chunkMeshes.values()) {
            if (!mesh.isValid()) continue;
            glBindVertexArray(mesh.vao());
            glDrawElements(GL_TRIANGLES, mesh.indexCount(), GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }
    }

    public static void main(String[] args) {
        new VoxelCraft().run();
    }

    private void cleanup() {
        for (ChunkMesh mesh : chunkMeshes.values()) {
            mesh.cleanup();
        }
        shader.cleanup();
        window.cleanup();
    }
}
