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

    private static final int FLOATS_PER_VERTEX = 10;
    private static final int STRIDE_BYTES = FLOATS_PER_VERTEX * Float.BYTES;

    /** CCW vertex order when viewed from outside along the face normal. */
    private enum Face {
        TOP(0, 1, 0,
            new int[][]{ {0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0} }),
        BOTTOM(0, -1, 0,
            new int[][]{ {0, 0, 1}, {1, 0, 1}, {1, 0, 0}, {0, 0, 0} }),
        EAST(1, 0, 0,
            new int[][]{ {1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1} }),
        WEST(-1, 0, 0,
            new int[][]{ {0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0} }),
        SOUTH(0, 0, 1,
            new int[][]{ {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1} }),
        NORTH(0, 0, -1,
            new int[][]{ {1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {0, 0, 0} });

        final int nx, ny, nz;
        final int[][] corners;

        Face(int nx, int ny, int nz, int[][] corners) {
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.corners = corners;
        }
    }

    public static ChunkMesh buildMesh(World world, int cx, int cz) {
        return uploadMesh(buildMeshData(world, cx, cz));
    }

    public static MeshData buildMeshData(World world, int cx, int cz) {
        List<Block> blocks = new ArrayList<>(world.getBlocksInChunk(cx, cz));

        List<Float> verts = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int nextVertex = 0;

        for (Block block : blocks) {
            nextVertex = addBlockFaces(block, world, verts, indices, nextVertex);
        }

        if (verts.isEmpty()) {
            return new MeshData(new float[0], new int[0]);
        }

        float[] vertexArray = new float[verts.size()];
        for (int i = 0; i < verts.size(); i++) {
            vertexArray[i] = verts.get(i);
        }

        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }

        return new MeshData(vertexArray, indexArray);
    }

    public static ChunkMesh uploadMesh(MeshData data) {
        if (data == null || data.isEmpty()) {
            return new ChunkMesh(0, 0, 0, 0);
        }

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(data.vertices.length);
        vertexBuffer.put(data.vertices).flip();

        IntBuffer indexBuffer = MemoryUtil.memAllocInt(data.indices.length);
        indexBuffer.put(data.indices).flip();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE_BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, STRIDE_BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 4, GL_FLOAT, false, STRIDE_BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);

        memFree(vertexBuffer);
        memFree(indexBuffer);

        return new ChunkMesh(vao, vbo, ebo, data.indices.length);
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
        float alpha = block.type.transparent ? 0.78f : 1f;

        for (Face face : Face.values()) {
            Block neighbor = world.getBlock(bx + face.nx, by + face.ny, bz + face.nz);
            if (neighbor != null && neighbor.type == block.type) {
                continue;
            }
            if (neighbor != null && !block.type.transparent && !neighbor.type.transparent) {
                continue;
            }
            if (neighbor != null && block.type.transparent && TerrainGenerator.isSolidBlock(neighbor.type.id)) {
                continue;
            }

            float cr, cg, cb;
            if (face == Face.TOP) {
                cr = tr;
                cg = tg;
                cb = tb;
            } else {
                cr = r;
                cg = g;
                cb = b;
            }

            int[][] corners = face.corners;
            addFace(verts, indices, baseVertex,
                bx + corners[0][0], by + corners[0][1], bz + corners[0][2],
                bx + corners[1][0], by + corners[1][1], bz + corners[1][2],
                bx + corners[2][0], by + corners[2][1], bz + corners[2][2],
                bx + corners[3][0], by + corners[3][1], bz + corners[3][2],
                face.nx, face.ny, face.nz, cr, cg, cb, alpha);
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
                                float cr, float cg, float cb, float alpha) {
        addVertex(verts, x1, y1, z1, nx, ny, nz, cr, cg, cb, alpha);
        addVertex(verts, x2, y2, z2, nx, ny, nz, cr, cg, cb, alpha);
        addVertex(verts, x3, y3, z3, nx, ny, nz, cr, cg, cb, alpha);
        addVertex(verts, x4, y4, z4, nx, ny, nz, cr, cg, cb, alpha);

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
                                    float r, float g, float b, float alpha) {
        verts.add(x);
        verts.add(y);
        verts.add(z);
        verts.add(nx);
        verts.add(ny);
        verts.add(nz);
        verts.add(r);
        verts.add(g);
        verts.add(b);
        verts.add(alpha);
    }

    public record MeshData(float[] vertices, int[] indices) {
        public boolean isEmpty() {
            return vertices.length == 0 || indices.length == 0;
        }
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
