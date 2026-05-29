package com.voxelcraft.render;

import com.voxelcraft.block.Block;
import com.voxelcraft.block.BlockType;
import com.voxelcraft.world.World;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL33;

import java.nio.*;
import java.util.*;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Builds greedy meshes or simple per-block meshes.
 * For simplicity, we build flat-shaded quads per visible face.
 */
public class VoxelMeshBuilder {

    public static ChunkMesh buildMesh(World world, int cx, int cz) {
        // Get all blocks in this chunk
        List<Block> blocks = new ArrayList<>();
        for (Block b : world.getAllBlocks()) {
            int bcx = Math.floorDiv(b.x(), ChunkGenerator.CHUNK_SIZE);
            int bcz = Math.floorDiv(b.z(), ChunkGenerator.CHUNK_SIZE);
            if (bcx == cx && bcz == cz) {
                blocks.add(b);
            }
        }

        // For each block, check which faces are visible
        // A face is visible if the adjacent block is air/water
        List<Float> verts = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int vertexCount = 0;

        for (Block block : blocks) {
            if (!TerrainGenerator.isSolidBlock(block.type.id)) continue;
            addBlockFaces(block, world, verts, indices, vertexCount);
            vertexCount += 24; // 4 verts per face * 6 faces = 24
        }

        if (verts.isEmpty()) {
            return new ChunkMesh(0, 0, 0, 0);
        }

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        float[] vertexArray = new float[verts.size()];
        for (int i = 0; i < verts.size(); i++) {
            vertexArray[i] = verts.get(i);
        }

        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }

        FloatBuffer vertexBuffer = memAllocFloat(vertexArray.length);
        vertexBuffer.put(vertexArray).flip();

        IntBuffer indexBuffer = memAllocInt(indexArray.length);
        indexBuffer.put(indexArray).flip();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        // Position (3 floats)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 24, 0);
        glEnableVertexAttribArray(0);

        // Normal (3 floats)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 24, 12);
        glEnableVertexAttribArray(1);

        // Color (3 floats)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 24, 20);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);

        memFree(vertexBuffer);
        memFree(indexBuffer);

        return new ChunkMesh(vao, vbo, ebo, indexArray.length);
    }

    private static void addBlockFaces(Block block, World world,
                                       List<Float> verts, List<Integer> indices,
                                       int baseVertex) {
        int bx = block.x(), by = block.y(), bz = block.z();
        float r = ((block.type.sideColor >> 16) & 0xFF) / 255f;
        float g = ((block.type.sideColor >> 8) & 0xFF) / 255f;
        float b = ((block.type.sideColor) & 0xFF) / 255f;

        // Top and bottom use topColor
        float tr = ((block.type.topColor >> 16) & 0xFF) / 255f;
        float tg = ((block.type.topColor >> 8) & 0xFF) / 255f;
        float tb = ((block.type.topColor) & 0xFF) / 255f;

        // Check each face
        // +Y (top)
        if (!world.collides(bx, by + 1, bz)) {
            addFace(verts, indices, baseVertex,
                bx, by + 1, bz, bx + 1, by + 1, bz,
                bx + 1, by + 1, bz + 1, bx, by + 1, bz + 1,
                0, 1, 0, tr, tg, tb);
            baseVertex += 4;
        }

        // -Y (bottom)
        if (!world.collides(bx, by - 1, bz)) {
            addFace(verts, indices, baseVertex,
                bx, by, bz, bx + 1, by, bz,
                bx + 1, by, bz + 1, bx, by, bz + 1,
                0, -1, 0, r, g, b);
            baseVertex += 4;
        }

        // +X
        if (!world.collides(bx + 1, by, bz)) {
            addFace(verts, indices, baseVertex,
                bx + 1, by, bz, bx + 1, by + 1, bz,
                bx + 1, by + 1, bz + 1, bx + 1, by, bz + 1,
                1, 0, 0, r, g, b);
            baseVertex += 4;
        }

        // -X
        if (!world.collides(bx - 1, by, bz)) {
            addFace(verts, indices, baseVertex,
                bx, by, bz, bx, by + 1, bz,
                bx, by + 1, bz + 1, bx, by, bz + 1,
                -1, 0, 0, r, g, b);
            baseVertex += 4;
        }

        // +Z
        if (!world.collides(bx, by, bz + 1)) {
            addFace(verts, indices, baseVertex,
                bx, by, bz + 1, bx + 1, by, bz + 1,
                bx + 1, by + 1, bz + 1, bx, by + 1, bz + 1,
                0, 0, 1, r, g, b);
            baseVertex += 4;
        }

        // -Z
        if (!world.collides(bx, by, bz - 1)) {
            addFace(verts, indices, baseVertex,
                bx, by, bz, bx + 1, by, bz,
                bx + 1, by + 1, bz, bx, by + 1, bz,
                0, 0, -1, r, g, b);
        }
    }

    private static void addFace(List<Float> verts, List<Integer> indices, int baseVertex,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float nx, float ny, float nz,
                                 float cr, float cg, float cb) {
        int start = baseVertex;

        // Vertex 1
        verts.add(x1); verts.add(y1); verts.add(z1);
        verts.add(nx); verts.add(ny); verts.add(nz);
        verts.add(cr); verts.add(cg); verts.add(cb);

        // Vertex 2
        verts.add(x2); verts.add(y2); verts.add(z2);
        verts.add(nx); verts.add(ny); verts.add(nz);
        verts.add(cr); verts.add(cg); verts.add(cb);

        // Vertex 3
        verts.add(x3); verts.add(y3); verts.add(z3);
        verts.add(nx); verts.add(ny); verts.add(nz);
        verts.add(cr); verts.add(cg); verts.add(cb);

        // Vertex 4
        verts.add(x4); verts.add(y4); verts.add(z4);
        verts.add(nx); verts.add(ny); verts.add(nz);
        verts.add(cr); verts.add(cg); verts.add(cb);

        // Two triangles: 0-1-2, 0-2-3
        indices.add(start);
        indices.add(start + 1);
        indices.add(start + 2);
        indices.add(start);
        indices.add(start + 2);
        indices.add(start + 3);
    }

    // Helper class (reuse constants from Java sources)
    static class ChunkGenerator {
        static final int CHUNK_SIZE = 16;
    }

    static class TerrainGenerator {
        static boolean isSolidBlock(String typeId) {
            return !"water".equals(typeId);
        }
    }

    public record ChunkMesh(int vao, int vbo, int ebo, int indexCount) {
        public boolean isValid() { return vao != 0 && indexCount > 0; }

        public void cleanup() {
            if (vao != 0) glDeleteVertexArrays(vao);
            if (vbo != 0) glDeleteBuffers(vbo);
            if (ebo != 0) glDeleteBuffers(ebo);
        }
    }
}
