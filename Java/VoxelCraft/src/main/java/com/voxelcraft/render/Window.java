package com.voxelcraft.render;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.*;

public class Window {
    private final long handle;
    private int width, height;

    public interface KeyCallback {
        void onKey(int key, int action);
    }

    public interface MouseButtonCallback {
        void onMouseButton(int button, int action);
    }

    public interface MouseMoveCallback {
        void onMouseMove(double dx, double dy);
    }

    public interface ScrollCallback {
        void onScroll(double dy);
    }

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;

        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 0);

        handle = glfwCreateWindow(width, height, title, 0, 0);
        if (handle == 0) {
            glfwTerminate();
            throw new RuntimeException("Failed to create GLFW window");
        }

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();

        glfwSwapInterval(1); // VSync
        glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetInputMode(handle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);

        glfwSetFramebufferSizeCallback(handle, (w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
        });
    }

    public void setKeyCallback(KeyCallback cb) {
        glfwSetKeyCallback(handle, (window, key, scancode, action, mods) -> {
            cb.onKey(key, action);
        });
    }

    public void setMouseButtonCallback(MouseButtonCallback cb) {
        glfwSetMouseButtonCallback(handle, (window, button, action, mods) -> {
            cb.onMouseButton(button, action);
        });
    }

    public void setMouseMoveCallback(MouseMoveCallback cb) {
        glfwSetCursorPosCallback(handle, (window, xpos, ypos) -> {
            // Use delta mode via glfwGetCursorPos
        });
    }

    public void setScrollCallback(ScrollCallback cb) {
        glfwSetScrollCallback(handle, (window, xoffset, yoffset) -> {
            cb.onScroll(yoffset);
        });
    }

    public double getMouseX() {
        double[] x = new double[1];
        glfwGetCursorPos(handle, x, null);
        return x[0];
    }

    public double getMouseY() {
        double[] y = new double[1];
        glfwGetCursorPos(handle, null, y);
        return y[0];
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getHandle() { return handle; }

    public void setCursorDisabled(boolean disabled) {
        glfwSetInputMode(handle, GLFW_CURSOR,
            disabled ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    public void cleanup() {
        glfwDestroyWindow(handle);
        glfwTerminate();
    }
}
