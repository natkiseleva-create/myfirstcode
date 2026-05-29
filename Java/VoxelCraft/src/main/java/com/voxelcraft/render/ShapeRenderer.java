package com.voxelcraft.render;

import com.voxelcraft.entities.Mob;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.memFree;

public class ShapeRenderer {
    private static final String VERTEX_SHADER = """
        #version 330 core
        layout (location = 0) in vec3 aPos;
        layout (location = 1) in vec3 aColor;
        uniform mat4 uProjection;
        uniform mat4 uView;
        out vec3 vColor;
        void main() {
            vColor = aColor;
            gl_Position = uProjection * uView * vec4(aPos, 1.0);
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 330 core
        in vec3 vColor;
        out vec4 FragColor;
        void main() {
            FragColor = vec4(vColor, 1.0);
        }
        """;

    private final int program;
    private final int vao;
    private final int vbo;
    private final int uProjection;
    private final int uView;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public ShapeRenderer() {
        program = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        uProjection = glGetUniformLocation(program, "uProjection");
        uView = glGetUniformLocation(program, "uView");

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    public void begin(Matrix4f projection, Matrix4f view) {
        glUseProgram(program);
        projection.get(matrixBuffer);
        glUniformMatrix4fv(uProjection, false, matrixBuffer);
        view.get(matrixBuffer);
        glUniformMatrix4fv(uView, false, matrixBuffer);
        glBindVertexArray(vao);
    }

    public void end() {
        glBindVertexArray(0);
        glUseProgram(0);
    }

    public void drawWireBlock(int x, int y, int z) {
        glLineWidth(2f);
        float[] c = rgb(0x000000);
        float e = 0.002f;
        float x0 = x - e, y0 = y - e, z0 = z - e;
        float x1 = x + 1 + e, y1 = y + 1 + e, z1 = z + 1 + e;
        float[] lines = lineBoxVertices(x0, y0, z0, x1, y1, z1, c);
        uploadAndDraw(lines, GL_LINES);
    }

    public void drawMob(Mob mob) {
        float[] color = rgb(mob.kind.color);
        float bw = (float) mob.kind.width;
        float bh = (float) mob.kind.height;
        float bd = (float) mob.kind.depth;
        drawBox((float) (mob.x - bw / 2), (float) mob.y, (float) (mob.z - bd / 2),
            (float) (mob.x + bw / 2), (float) (mob.y + bh), (float) (mob.z + bd / 2), color);

        float hw = bw * 0.55f;
        float hh = bh * 0.55f;
        float hd = bd * 0.45f;
        double sin = Math.sin(mob.yaw);
        double cos = Math.cos(mob.yaw);
        float hx = (float) (mob.x + sin * bd * 0.45);
        float hz = (float) (mob.z + cos * bd * 0.45);
        drawBox(hx - hw / 2, (float) (mob.y + bh * 0.58), hz - hd / 2,
            hx + hw / 2, (float) (mob.y + bh * 0.58 + hh), hz + hd / 2, color);
    }

    private void drawBox(float x0, float y0, float z0, float x1, float y1, float z1, float[] c) {
        float[] v = {
            x0,y0,z1,c[0],c[1],c[2], x1,y0,z1,c[0],c[1],c[2], x1,y1,z1,c[0],c[1],c[2],
            x0,y0,z1,c[0],c[1],c[2], x1,y1,z1,c[0],c[1],c[2], x0,y1,z1,c[0],c[1],c[2],
            x1,y0,z0,c[0],c[1],c[2], x0,y0,z0,c[0],c[1],c[2], x0,y1,z0,c[0],c[1],c[2],
            x1,y0,z0,c[0],c[1],c[2], x0,y1,z0,c[0],c[1],c[2], x1,y1,z0,c[0],c[1],c[2],
            x0,y0,z0,c[0],c[1],c[2], x0,y0,z1,c[0],c[1],c[2], x0,y1,z1,c[0],c[1],c[2],
            x0,y0,z0,c[0],c[1],c[2], x0,y1,z1,c[0],c[1],c[2], x0,y1,z0,c[0],c[1],c[2],
            x1,y0,z1,c[0],c[1],c[2], x1,y0,z0,c[0],c[1],c[2], x1,y1,z0,c[0],c[1],c[2],
            x1,y0,z1,c[0],c[1],c[2], x1,y1,z0,c[0],c[1],c[2], x1,y1,z1,c[0],c[1],c[2],
            x0,y1,z1,c[0],c[1],c[2], x1,y1,z1,c[0],c[1],c[2], x1,y1,z0,c[0],c[1],c[2],
            x0,y1,z1,c[0],c[1],c[2], x1,y1,z0,c[0],c[1],c[2], x0,y1,z0,c[0],c[1],c[2],
            x0,y0,z0,c[0],c[1],c[2], x1,y0,z0,c[0],c[1],c[2], x1,y0,z1,c[0],c[1],c[2],
            x0,y0,z0,c[0],c[1],c[2], x1,y0,z1,c[0],c[1],c[2], x0,y0,z1,c[0],c[1],c[2],
        };
        uploadAndDraw(v, GL_TRIANGLES);
    }

    private float[] lineBoxVertices(float x0, float y0, float z0, float x1, float y1, float z1, float[] c) {
        float[][] p = {{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[] e = {0,1, 1,2, 2,3, 3,0, 4,5, 5,6, 6,7, 7,4, 0,4, 1,5, 2,6, 3,7};
        float[] out = new float[e.length * 6];
        int i = 0;
        for (int idx : e) {
            out[i++] = p[idx][0]; out[i++] = p[idx][1]; out[i++] = p[idx][2];
            out[i++] = c[0]; out[i++] = c[1]; out[i++] = c[2];
        }
        return out;
    }

    private void uploadAndDraw(float[] data, int mode) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(data.length);
        buffer.put(data).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        glDrawArrays(mode, 0, data.length / 6);
        memFree(buffer);
    }

    private float[] rgb(int color) {
        return new float[] { ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f };
    }

    private static int linkProgram(String vertexSource, String fragmentSource) {
        int vertex = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragment = compileShader(GL_FRAGMENT_SHADER, fragmentSource);
        int program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shape shader link error: " + glGetProgramInfoLog(program));
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
            throw new RuntimeException("Shape shader compile error: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteProgram(program);
    }
}
