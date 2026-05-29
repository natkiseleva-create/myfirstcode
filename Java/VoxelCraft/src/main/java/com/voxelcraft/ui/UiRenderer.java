package com.voxelcraft.ui;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.memFree;

public class UiRenderer {
    private static final boolean AWT_TEXT_ENABLED =
        !"false".equalsIgnoreCase(System.getProperty("voxelcraft.awtText"));
    private static final String VERTEX_SHADER = """
        #version 330 core
        layout (location = 0) in vec2 aPos;
        layout (location = 1) in vec2 aUv;
        uniform mat4 uProjection;
        out vec2 vUv;
        void main() {
            vUv = aUv;
            gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 330 core
        in vec2 vUv;
        uniform vec4 uColor;
        uniform sampler2D uTexture;
        uniform int uUseTexture;
        out vec4 FragColor;
        void main() {
            vec4 color = uUseTexture == 1 ? texture(uTexture, vUv) : uColor;
            FragColor = vec4(color.rgb, color.a * uColor.a);
        }
        """;

    private final int program;
    private final int uProjection;
    private final int uColor;
    private final int uTexture;
    private final int uUseTexture;
    private final int quadVao;
    private final int quadVbo;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private final Map<String, Integer> textTextures = new LinkedHashMap<>();

    public UiRenderer() {
        program = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        uProjection = glGetUniformLocation(program, "uProjection");
        uColor = glGetUniformLocation(program, "uColor");
        uTexture = glGetUniformLocation(program, "uTexture");
        uUseTexture = glGetUniformLocation(program, "uUseTexture");

        float[] quad = {
            0f, 0f, 0f, 0f,
            1f, 0f, 1f, 0f,
            1f, 1f, 1f, 1f,
            0f, 0f, 0f, 0f,
            1f, 1f, 1f, 1f,
            0f, 1f, 0f, 1f,
        };

        quadVao = glGenVertexArrays();
        quadVbo = glGenBuffers();
        FloatBuffer buffer = MemoryUtil.memAllocFloat(quad.length);
        buffer.put(quad).flip();

        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
        memFree(buffer);
    }

    public void begin(int width, int height) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Matrix4f projection = new Matrix4f().ortho2D(0, width, height, 0);
        glUseProgram(program);
        projection.get(matrixBuffer);
        glUniformMatrix4fv(uProjection, false, matrixBuffer);
        glBindVertexArray(quadVao);
    }

    public void end() {
        glBindVertexArray(0);
        glUseProgram(0);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public void fillRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        glUniform1i(uUseTexture, 0);
        glUniform4f(uColor, r, g, b, a);
        drawQuad(x, y, w, h);
    }

    public void drawText(String key, String text, float x, float y, int size, Color color) {
        if (!AWT_TEXT_ENABLED) {
            return;
        }
        String textureKey = key + "|" + text + "|" + size + "|" + color.getRGB();
        int texture = textTextures.computeIfAbsent(textureKey, k -> createTextTexture(text, size, color));
        glUniform1i(uUseTexture, 1);
        glUniform4f(uColor, 1f, 1f, 1f, 1f);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glUniform1i(uTexture, 0);

        int[] dims = textDimensions(text, size);
        drawQuad(x, y, dims[0], dims[1]);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static boolean hitRect(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void drawQuad(float x, float y, float w, float h) {
        float x2 = x + w;
        float y2 = y + h;
        float[] quad = {
            x, y, 0f, 0f,
            x2, y, 1f, 0f,
            x2, y2, 1f, 1f,
            x, y, 0f, 0f,
            x2, y2, 1f, 1f,
            x, y2, 0f, 1f,
        };
        FloatBuffer buffer = MemoryUtil.memAllocFloat(quad.length);
        buffer.put(quad).flip();
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        memFree(buffer);
    }

    private int createTextTexture(String text, int size, Color color) {
        int[] dims = textDimensions(text, size);
        BufferedImage image = new BufferedImage(dims[0], dims[1], BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(resolveFont(size));
        g.setColor(color);
        g.drawString(text, 0, size);
        g.dispose();

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        int[] pixels = new int[dims[0] * dims[1]];
        image.getRGB(0, 0, dims[0], dims[1], pixels, 0, dims[0]);
        ByteBuffer buffer = MemoryUtil.memAlloc(dims[0] * dims[1] * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, dims[0], dims[1], 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        memFree(buffer);
        glBindTexture(GL_TEXTURE_2D, 0);
        return texture;
    }

    private int[] textDimensions(String text, int size) {
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scratch.createGraphics();
        g.setFont(resolveFont(size));
        FontMetrics metrics = g.getFontMetrics();
        g.dispose();
        return new int[]{Math.max(1, metrics.stringWidth(text)), Math.max(1, metrics.getHeight())};
    }

    private static Font resolveFont(int size) {
        for (String family : new String[]{"Helvetica Neue", "Arial", "DejaVu Sans", Font.SANS_SERIF}) {
            Font font = new Font(family, Font.PLAIN, size);
            if (font.canDisplayUpTo("Выберите режим игры") == -1) {
                return font;
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, size);
    }

    private static int linkProgram(String vertexSource, String fragmentSource) {
        int vertex = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragment = compileShader(GL_FRAGMENT_SHADER, fragmentSource);
        int program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("UI shader link error: " + glGetProgramInfoLog(program));
        }
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        return program;
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("UI shader compile error: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public void cleanup() {
        for (int texture : textTextures.values()) {
            glDeleteTextures(texture);
        }
        textTextures.clear();
        glDeleteVertexArrays(quadVao);
        glDeleteBuffers(quadVbo);
        glDeleteProgram(program);
    }
}
