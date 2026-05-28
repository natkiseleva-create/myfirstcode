import { Mob, randomMobKind } from './Mob.js';
import { randomAt } from '../world/noise.js';
import { canPlaceTree } from '../world/terrainGenerator.js';
import { CHUNK_SIZE } from '../world/chunkGenerator.js';

const MOBS_PER_CHUNK = 2;
const MAX_MOBS = 48;

export class MobManager {
  /**
   * @param {THREE.Scene} scene
   * @param {object} worldApi
   */
  constructor(scene, worldApi) {
    this.scene = scene;
    this.world = worldApi;
    this.mobs = [];
    this.spawnedChunks = new Set();
  }

  _chunkKey(cx, cz) {
    return `${cx},${cz}`;
  }

  onChunkLoaded(cx, cz) {
    const key = this._chunkKey(cx, cz);
    if (this.spawnedChunks.has(key)) return;
    this.spawnedChunks.add(key);

    if (this.mobs.length >= MAX_MOBS) return;

    const baseX = cx * CHUNK_SIZE;
    const baseZ = cz * CHUNK_SIZE;

    for (let i = 0; i < MOBS_PER_CHUNK; i++) {
      if (this.mobs.length >= MAX_MOBS) break;

      const lx = 2 + Math.floor(randomAt(cx * 3 + i, cz * 5 + i, 44) * (CHUNK_SIZE - 4));
      const lz = 2 + Math.floor(randomAt(cx * 7 + i, cz * 2 + i, 55) * (CHUNK_SIZE - 4));
      const wx = baseX + lx;
      const wz = baseZ + lz;

      if (!canPlaceTree(wx, wz)) continue;
      if (randomAt(wx, wz, 66) > 0.55) continue;

      const kind = randomMobKind(wx, wz);
      const mob = new Mob(kind, wx + 0.5, wz + 0.5, this.world);
      this.mobs.push(mob);
      this.scene.add(mob.group);
    }
  }

  onChunkUnloaded(cx, cz) {
    const key = this._chunkKey(cx, cz);
    this.spawnedChunks.delete(key);

    const minX = cx * CHUNK_SIZE - 2;
    const maxX = (cx + 1) * CHUNK_SIZE + 2;
    const minZ = cz * CHUNK_SIZE - 2;
    const maxZ = (cz + 1) * CHUNK_SIZE + 2;

    this.mobs = this.mobs.filter((mob) => {
      const { x, z } = mob.group.position;
      if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
        mob.dispose();
        return false;
      }
      return true;
    });
  }

  update(dt) {
    for (const mob of this.mobs) {
      mob.update(dt);
    }
  }

  dispose() {
    for (const mob of this.mobs) {
      mob.dispose();
    }
    this.mobs = [];
    this.spawnedChunks.clear();
  }
}
