package com.voxelcraft.render;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.io.*;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;

public class ShaderProgram {
    private final int programId;
    private final int uProjection;
    private final int uView;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vertex = compileShader(GL_VERTEX_SHADER, loadResource(vertexPath));
        int fragment = compileShader(GL_FRAGMENT_SHADER, loadResource(fragmentPath));

        programId = glCreateProgram();
        glAttachShader(programId, vertex);
        glAttachShader(programId, fragment);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader link error: " + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vertex);
        glDeleteShader(fragment);

        uProjection = glGetUniformLocation(programId, "uProjection");
        uView = glGetUniformLocation(programId, "uView");
    }

    public void use() {
        glUseProgram(programId);
    }

    public void setProjection(Matrix4f projection) {
        projection.get(matrixBuffer);
        glUniformMatrix4fv(uProjection, false, matrixBuffer);
    }

    public void setView(Matrix4f view) {
        view.get(matrixBuffer);
        glUniformMatrix4fv(uView, false, matrixBuffer);
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }

    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Resource not found: " + path);
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }
}
