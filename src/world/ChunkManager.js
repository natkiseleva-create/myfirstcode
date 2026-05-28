import { CHUNK_SIZE, generateChunkBlocks } from './chunkGenerator.js';
import { buildChunkMeshes, disposeChunkMeshes } from './chunkMeshBuilder.js';
import { isInfiniteStone } from './terrainRules.js';

const VIEW_DISTANCE = 3;
const UNLOAD_DISTANCE = 4;
const CHUNKS_PER_FRAME = 2;

const blockKey = (x, y, z) => `${x},${y},${z}`;
const chunkKey = (cx, cz) => `${cx},${cz}`;

export class ChunkManager {
  /** @param {THREE.Scene} scene */
  constructor(scene) {
    this.scene = scene;
    this.chunks = new Map();
    this.modifications = new Map();
    this.pickables = [];
    this.loadQueue = [];
    this.lastPlayerChunk = { cx: NaN, cz: NaN };
  }

  _parseKey(key) {
    const [x, y, z] = key.split(',').map(Number);
    return { x, y, z };
  }

  _chunkCoords(x, z) {
    return {
      cx: Math.floor(x / CHUNK_SIZE),
      cz: Math.floor(z / CHUNK_SIZE),
    };
  }

  _collectChunkBlocks(cx, cz) {
    const procedural = generateChunkBlocks(cx, cz);
    const blockMap = new Map();
    const seen = new Set();

    for (const block of procedural) {
      const key = blockKey(block.x, block.y, block.z);
      const mod = this.modifications.get(key);
      if (mod === null) continue;
      const type = mod ?? block.type;
      blockMap.set(key, { ...block, type });
      seen.add(key);
    }

    const minX = cx * CHUNK_SIZE;
    const maxX = minX + CHUNK_SIZE - 1;
    const minZ = cz * CHUNK_SIZE;
    const maxZ = minZ + CHUNK_SIZE - 1;

    for (const [key, type] of this.modifications) {
      if (type === null || seen.has(key)) continue;
      const { x, y, z } = this._parseKey(key);
      if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
        blockMap.set(key, { x, y, z, type });
        seen.add(key);
      }
    }

    return blockMap;
  }

  _rebuildChunkMeshes(chunk) {
    if (chunk.group) {
      for (const mesh of chunk.pickables) {
        const idx = this.pickables.indexOf(mesh);
        if (idx >= 0) this.pickables.splice(idx, 1);
      }
      this.scene.remove(chunk.group);
      disposeChunkMeshes(chunk.group);
    }

    const blocks = [...chunk.blocks.values()];
    const { group, pickables } = buildChunkMeshes(blocks);
    chunk.group = group;
    chunk.pickables = pickables;
    this.pickables.push(...pickables);
    this.scene.add(group);
  }

  _extendStoneDown(chunk, x, z, y) {
    if (y >= 0) return;
    const key = blockKey(x, y, z);
    if (this.modifications.get(key) === null) return;
    if (chunk.blocks.has(key)) return;
    chunk.blocks.set(key, { x, y, z, type: 'stone' });
  }

  _createChunk(cx, cz) {
    const blocks = this._collectChunkBlocks(cx, cz);
    const chunk = { cx, cz, blocks, group: null, pickables: [] };
    this._rebuildChunkMeshes(chunk);
    this.chunks.set(chunkKey(cx, cz), chunk);
    return chunk;
  }

  loadChunk(cx, cz) {
    const key = chunkKey(cx, cz);
    if (this.chunks.has(key)) return;
    this._createChunk(cx, cz);
  }

  _queueChunk(cx, cz) {
    const key = chunkKey(cx, cz);
    if (this.chunks.has(key)) return;
    if (this.loadQueue.some((c) => c.cx === cx && c.cz === cz)) return;
    this.loadQueue.push({ cx, cz });
  }

  _processLoadQueue() {
    let n = CHUNKS_PER_FRAME;
    while (n > 0 && this.loadQueue.length > 0) {
      const { cx, cz } = this.loadQueue.shift();
      this.loadChunk(cx, cz);
      n--;
    }
  }

  unloadChunk(cx, cz) {
    const key = chunkKey(cx, cz);
    const chunk = this.chunks.get(key);
    if (!chunk) return;

    for (const mesh of chunk.pickables) {
      const idx = this.pickables.indexOf(mesh);
      if (idx >= 0) this.pickables.splice(idx, 1);
    }

    if (chunk.group) {
      this.scene.remove(chunk.group);
      disposeChunkMeshes(chunk.group);
    }

    this.chunks.delete(key);
  }

  update(playerX, playerZ) {
    const pcx = Math.floor(playerX / CHUNK_SIZE);
    const pcz = Math.floor(playerZ / CHUNK_SIZE);

    const chunkChanged =
      pcx !== this.lastPlayerChunk.cx || pcz !== this.lastPlayerChunk.cz;

    if (chunkChanged) {
      this.lastPlayerChunk = { cx: pcx, cz: pcz };

      for (let dx = -VIEW_DISTANCE; dx <= VIEW_DISTANCE; dx++) {
        for (let dz = -VIEW_DISTANCE; dz <= VIEW_DISTANCE; dz++) {
          this._queueChunk(pcx + dx, pcz + dz);
        }
      }

      for (const chunk of this.chunks.values()) {
        const dist = Math.max(
          Math.abs(chunk.cx - pcx),
          Math.abs(chunk.cz - pcz)
        );
        if (dist > UNLOAD_DISTANCE) {
          this.unloadChunk(chunk.cx, chunk.cz);
        }
      }
    }

    this._processLoadQueue();
  }

  ensureLoaded(playerX, playerZ) {
    const pcx = Math.floor(playerX / CHUNK_SIZE);
    const pcz = Math.floor(playerZ / CHUNK_SIZE);

    for (let dx = -VIEW_DISTANCE; dx <= VIEW_DISTANCE; dx++) {
      for (let dz = -VIEW_DISTANCE; dz <= VIEW_DISTANCE; dz++) {
        this.loadChunk(pcx + dx, pcz + dz);
      }
    }

    this.loadQueue = [];
    this.lastPlayerChunk = { cx: pcx, cz: pcz };
  }

  _getChunkAtBlock(x, z) {
    const { cx, cz } = this._chunkCoords(x, z);
    const key = chunkKey(cx, cz);
    let chunk = this.chunks.get(key);
    if (!chunk) {
      this.loadChunk(cx, cz);
      chunk = this.chunks.get(key);
    }
    return chunk;
  }

  getColumnTop(x, z) {
    const bx = Math.floor(x);
    const bz = Math.floor(z);
    const chunk = this._getChunkAtBlock(bx, bz);
    if (!chunk) return 0;

    let maxY = -Infinity;
    for (const block of chunk.blocks.values()) {
      if (block.x === bx && block.z === bz && block.y > maxY) {
        maxY = block.y;
      }
    }

    if (maxY === -Infinity) return 0;
    return maxY + 1;
  }

  getSupportHeight(x, z, feetY, radius = 0.35) {
    const offsets = [
      [0, 0],
      [radius, 0],
      [-radius, 0],
      [0, radius],
      [0, -radius],
    ];

    let support = -Infinity;
    for (const [ox, oz] of offsets) {
      const top = this.getColumnTop(x + ox, z + oz);
      if (top <= feetY + 0.15) {
        support = Math.max(support, top);
      }
    }
    return support === -Infinity ? 0 : support;
  }

  collides(pos) {
    const bx = Math.floor(pos.x);
    const by = Math.floor(pos.y);
    const bz = Math.floor(pos.z);
    const key = blockKey(bx, by, bz);

    if (this._getChunkAtBlock(bx, bz)?.blocks.has(key)) {
      return true;
    }

    return isInfiniteStone(bx, by, bz, this.modifications);
  }

  getPickables() {
    return this.pickables;
  }

  removeBlock(x, y, z) {
    const key = blockKey(x, y, z);
    const chunk = this._getChunkAtBlock(x, z);
    if (!chunk) return null;

    const inMap = chunk.blocks.has(key);
    const procedural = isInfiniteStone(x, y, z, this.modifications);

    if (!inMap && !procedural) return null;

    let type = 'stone';
    if (inMap) {
      type = chunk.blocks.get(key).type;
      chunk.blocks.delete(key);
    }

    this.modifications.set(key, null);

    if (y < 0 && isInfiniteStone(x, y - 1, z, this.modifications)) {
      this._extendStoneDown(chunk, x, z, y - 1);
    }

    this._rebuildChunkMeshes(chunk);
    return type;
  }

  placeBlock(x, y, z, typeId) {
    const key = blockKey(x, y, z);
    const chunk = this._getChunkAtBlock(x, z);
    if (!chunk) return false;

    if (
      chunk.blocks.has(key) ||
      isInfiniteStone(x, y, z, this.modifications)
    ) {
      return false;
    }

    chunk.blocks.set(key, { x, y, z, type: typeId });
    this.modifications.set(key, typeId);
    this._rebuildChunkMeshes(chunk);
    return true;
  }
}
