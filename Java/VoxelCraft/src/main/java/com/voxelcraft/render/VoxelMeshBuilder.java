package com.voxelcraft.render;

import com.voxelcraft.block.Block;
import com.voxelcraft.world.ChunkGenerator;
import com.voxelcraft.world.TerrainGenerator;
import com.voxelcraft.world.World;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Builds flat-shaded quads for each visible block face.
 */
public class VoxelMeshBuilder {

    private static final int FLOATS_PER_VERTEX = 9;
    private static final int STRIDE_BYTES = FLOATS_PER_VERTEX * Float.BYTES;

    public static ChunkMesh buildMesh(World world, int cx, int cz) {
        List<Block> blocks = new ArrayList<>();
        for (Block b : world.getAllBlocks()) {
            int bcx = Math.floorDiv(b.x(), ChunkGenerator.CHUNK_SIZE);
            int bcz = Math.floorDiv(b.z(), ChunkGenerator.CHUNK_SIZE);
            if (bcx == cx && bcz == cz) {
                blocks.add(b);
            }
        }

        List<Float> verts = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int nextVertex = 0;

        for (Block block : blocks) {
            if (!TerrainGenerator.isSolidBlock(block.type.id)) continue;
            nextVertex = addBlockFaces(block, world, verts, indices, nextVertex);
        }

        if (verts.isEmpty()) {
            return new ChunkMesh(0, 0, 0, 0);
        }

        float[] vertexArray = new float[verts.size()];
        for (int i = 0; i < verts.size(); i++) {
            vertexArray[i] = verts.get(i);
        }

        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertexArray.length);
        vertexBuffer.put(vertexArray).flip();

        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indexArray.length);
        indexBuffer.put(indexArray).flip();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE_BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, STRIDE_BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 3, GL_FLOAT, false, STRIDE_BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);

        memFree(vertexBuffer);
        memFree(indexBuffer);

        return new ChunkMesh(vao, vbo, ebo, indexArray.length);
    }

    private static int addBlockFaces(Block block, World world,
                                     List<Float> verts, List<Integer> indices,
                                     int baseVertex) {
        int bx = block.x(), by = block.y(), bz = block.z();
        float r = ((block.type.sideColor >> 16) & 0xFF) / 255f;
        float g = ((block.type.sideColor >> 8) & 0xFF) / 255f;
        float b = (block.type.sideColor & 0xFF) / 255f;

        float tr = ((block.type.topColor >> 16) & 0xFF) / 255f;
        float tg = ((block.type.topColor >> 8) & 0xFF) / 255f;
        float tb = (block.type.topColor & 0xFF) / 255f;

        if (!world.collides(bx, by + 1, bz)) {
            addFace(verts, indices, baseVertex,
                bx, by + 1, bz, bx + 1, by + 1, bz,
                bx + 1, by + 1, bz + 1, bx, by + 1, bz + 1,
                0, 1, 0, tr, tg, tb);
            baseVertex += 4;
        }

        if (!world.collides(bx, by - 1, bz)) {
            addFace(verts, indices, baseVertex,
                bx, by, bz, bx + 1, by, bz,
                bx + 1, by, bz + 1, bx, by, bz + 1,
                0, -1, 0, r, g, b);
            baseVertex += 4;
        }

        if (!world.collides(bx + 1, by, bz)) {
            addFace(verts, indices, baseVertex,
                bx + 1, by, bz, bx + 1, by + 1, bz,
                bx + 1, by + 1, bz + 1, bx + 1, by, bz + 1,
                1, 0, 0, r, g, b);
            baseVertex += 4;
        }

        if (!world.collides(bx - 1, by, bz)) {
            addFace(verts, indices, baseVertex,
                bx, by, bz, bx, by + 1, bz,
                bx, by + 1, bz + 1, bx, by, bz + 1,
                -1, 0, 0, r, g, b);
            baseVertex += 4;
        }

        if (!world.collides(bx, by, bz + 1)) {
            addFace(verts, indices, baseVertex,
                bx, by, bz + 1, bx + 1, by, bz + 1,
                bx + 1, by + 1, bz + 1, bx, by + 1, bz + 1,
                0, 0, 1, r, g, b);
            baseVertex += 4;
        }

        if (!world.collides(bx, by, bz - 1)) {
            addFace(verts, indices, baseVertex,
                bx, by, bz, bx + 1, by, bz,
                bx + 1, by + 1, bz, bx, by + 1, bz,
                0, 0, -1, r, g, b);
            baseVertex += 4;
        }

        return baseVertex;
    }

    private static void addFace(List<Float> verts, List<Integer> indices, int baseVertex,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float nx, float ny, float nz,
                                float cr, float cg, float cb) {
        addVertex(verts, x1, y1, z1, nx, ny, nz, cr, cg, cb);
        addVertex(verts, x2, y2, z2, nx, ny, nz, cr, cg, cb);
        addVertex(verts, x3, y3, z3, nx, ny, nz, cr, cg, cb);
        addVertex(verts, x4, y4, z4, nx, ny, nz, cr, cg, cb);

        indices.add(baseVertex);
        indices.add(baseVertex + 1);
        indices.add(baseVertex + 2);
        indices.add(baseVertex);
        indices.add(baseVertex + 2);
        indices.add(baseVertex + 3);
    }

    private static void addVertex(List<Float> verts,
                                    float x, float y, float z,
                                    float nx, float ny, float nz,
                                    float r, float g, float b) {
        verts.add(x);
        verts.add(y);
        verts.add(z);
        verts.add(nx);
        verts.add(ny);
        verts.add(nz);
        verts.add(r);
        verts.add(g);
        verts.add(b);
    }

    public record ChunkMesh(int vao, int vbo, int ebo, int indexCount) {
        public boolean isValid() {
            return vao != 0 && indexCount > 0;
        }

        public void cleanup() {
            if (vao != 0) glDeleteVertexArrays(vao);
            if (vbo != 0) glDeleteBuffers(vbo);
            if (ebo != 0) glDeleteBuffers(ebo);
        }
    }
}
