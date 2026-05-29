package com.voxelcraft.entities;

import com.voxelcraft.math.FastNoise;
import com.voxelcraft.world.ChunkGenerator;
import com.voxelcraft.world.TerrainGenerator;
import com.voxelcraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MobManager {
    private static final int MOBS_PER_CHUNK = 1;
    private static final int MAX_MOBS = 14;
    private static final double MOB_SPAWN_CHANCE = 0.22;

    private final List<Mob> mobs = new ArrayList<>();
    private final Set<String> spawnedChunks = new HashSet<>();

    public List<Mob> mobs() {
        return mobs;
    }

    public void onChunkLoaded(int cx, int cz, World world) {
        String key = cx + "," + cz;
        if (!spawnedChunks.add(key)) return;
        if (mobs.size() >= MAX_MOBS) return;

        int baseX = cx * ChunkGenerator.CHUNK_SIZE;
        int baseZ = cz * ChunkGenerator.CHUNK_SIZE;

        for (int i = 0; i < MOBS_PER_CHUNK && mobs.size() < MAX_MOBS; i++) {
            int lx = 2 + (int) Math.floor(FastNoise.randomAt(cx * 3 + i, cz * 5 + i, 44) * (ChunkGenerator.CHUNK_SIZE - 4));
            int lz = 2 + (int) Math.floor(FastNoise.randomAt(cx * 7 + i, cz * 2 + i, 55) * (ChunkGenerator.CHUNK_SIZE - 4));
            int wx = baseX + lx;
            int wz = baseZ + lz;

            if (!TerrainGenerator.canPlaceTree(wx, wz)) continue;
            if (FastNoise.randomAt(wx, wz, 66) > MOB_SPAWN_CHANCE) continue;

            mobs.add(new Mob(Mob.randomKind(wx, wz), wx + 0.5, wz + 0.5, world));
        }
    }

    public void onChunkUnloaded(int cx, int cz) {
        spawnedChunks.remove(cx + "," + cz);
        int minX = cx * ChunkGenerator.CHUNK_SIZE - 2;
        int maxX = (cx + 1) * ChunkGenerator.CHUNK_SIZE + 2;
        int minZ = cz * ChunkGenerator.CHUNK_SIZE - 2;
        int maxZ = (cz + 1) * ChunkGenerator.CHUNK_SIZE + 2;

        for (Iterator<Mob> it = mobs.iterator(); it.hasNext(); ) {
            Mob mob = it.next();
            if (mob.x >= minX && mob.x <= maxX && mob.z >= minZ && mob.z <= maxZ) {
                it.remove();
            }
        }
    }

    public void update(double dt, World world) {
        for (Mob mob : mobs) {
            mob.update(dt, world);
        }
    }

    public void clear() {
        mobs.clear();
        spawnedChunks.clear();
    }
}
