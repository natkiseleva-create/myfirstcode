import { CHUNK_SIZE, generateChunkBlocks } from './chunkGenerator.js';
import { isSolidBlock } from './terrainRules.js';
import {
  buildChunkMeshes,
  disposeChunkMeshes,
  removeBlockInstance,
  blockRefKey as meshBlockKey,
} from './chunkMeshBuilder.js';

const VIEW_DISTANCE = 2;
const UNLOAD_DISTANCE = 3;
const CHUNKS_PER_FRAME = 1;
const GROUND_EPSILON = 0.08;

const blockKey = (x, y, z) => `${x},${y},${z}`;
const chunkKey = (cx, cz) => `${cx},${cz}`;
const columnKey = (x, z) => `${x},${z}`;

export class ChunkManager {
  constructor(scene, options = {}) {
    this.scene = scene;
    this.mobManager = options.mobManager ?? null;
    this.chunks = new Map();
    this.modifications = new Map();
    this.modsByChunk = new Map();
    this.pickables = [];
    this.pickableSet = new Set();
    this.loadQueue = [];
    this.rebuildQueue = [];
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

  _modChunkIndex(cx, cz) {
    return chunkKey(cx, cz);
  }

  _setModification(key, value, cx, cz) {
    if (value === undefined) {
      this.modifications.delete(key);
    } else {
      this.modifications.set(key, value);
    }

    const ck = this._modChunkIndex(cx, cz);
    if (!this.modsByChunk.has(ck)) this.modsByChunk.set(ck, new Map());
    const bucket = this.modsByChunk.get(ck);
    if (value === undefined) bucket.delete(key);
    else bucket.set(key, value);
  }

  _updateColumnTop(chunk, x, z) {
    let maxY = -Infinity;
    for (const block of chunk.blocks.values()) {
      if (block.x === x && block.z === z && block.y > maxY && isSolidBlock(block.type)) {
        maxY = block.y;
      }
    }
    if (maxY === -Infinity) {
      chunk.columnTops.delete(columnKey(x, z));
    } else {
      chunk.columnTops.set(columnKey(x, z), maxY + 1);
    }
  }

  _rebuildColumnTops(chunk) {
    chunk.columnTops.clear();
    for (const block of chunk.blocks.values()) {
      if (!isSolidBlock(block.type)) continue;
      const ck = columnKey(block.x, block.z);
      const top = chunk.columnTops.get(ck) ?? -Infinity;
      const surface = block.y + 1;
      if (surface > top) chunk.columnTops.set(ck, surface);
    }
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

    const bucket = this.modsByChunk.get(chunkKey(cx, cz));
    if (bucket) {
      for (const [key, type] of bucket) {
        if (type === null || seen.has(key)) continue;
        const { x, y, z } = this._parseKey(key);
        blockMap.set(key, { x, y, z, type });
        seen.add(key);
      }
    }

    return blockMap;
  }

  _attachChunkMeshes(chunk, built) {
    chunk.group = built.group;
    chunk.pickables = built.pickables;
    chunk.refs = built.refs;
    chunk.lists = built.lists;

    for (const mesh of built.pickables) {
      if (!this.pickableSet.has(mesh)) {
        this.pickableSet.add(mesh);
        this.pickables.push(mesh);
      }
    }
    this.scene.add(built.group);
  }

  _detachChunkMeshes(chunk) {
    for (const mesh of chunk.pickables) {
      this.pickableSet.delete(mesh);
      const idx = this.pickables.indexOf(mesh);
      if (idx >= 0) this.pickables.splice(idx, 1);
    }
    if (chunk.group) {
      this.scene.remove(chunk.group);
      disposeChunkMeshes(chunk.group, chunk.pickables);
    }
    chunk.group = null;
    chunk.pickables = [];
    chunk.refs = new Map();
    chunk.lists = new Map();
  }

  _rebuildChunkMeshes(chunk) {
    this._detachChunkMeshes(chunk);
    const built = buildChunkMeshes([...chunk.blocks.values()]);
    this._attachChunkMeshes(chunk, built);
  }

  _queueRebuild(chunk) {
    if (!this.rebuildQueue.includes(chunk)) {
      this.rebuildQueue.push(chunk);
    }
  }

  _processRebuildQueue() {
    const chunk = this.rebuildQueue.shift();
    if (!chunk) return;
    this._rebuildChunkMeshes(chunk);
  }

  _createChunk(cx, cz) {
    const blocks = this._collectChunkBlocks(cx, cz);
    const chunk = {
      cx,
      cz,
      blocks,
      columnTops: new Map(),
      group: null,
      pickables: [],
      refs: new Map(),
      lists: new Map(),
    };
    this._rebuildColumnTops(chunk);
    this._rebuildChunkMeshes(chunk);
    this.chunks.set(chunkKey(cx, cz), chunk);
    return chunk;
  }

  loadChunk(cx, cz) {
    if (this.chunks.has(chunkKey(cx, cz))) return;
    this._createChunk(cx, cz);
    this.mobManager?.onChunkLoaded(cx, cz);
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

    this.mobManager?.onChunkUnloaded(cx, cz);
    this.rebuildQueue = this.rebuildQueue.filter((c) => c !== chunk);
    this._detachChunkMeshes(chunk);
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
    this._processRebuildQueue();
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
    return chunk.columnTops.get(columnKey(bx, bz)) ?? 0;
  }

  _getColumnSupportAt(wx, wz, maxSurfaceY) {
    const bx = Math.floor(wx);
    const bz = Math.floor(wz);
    const chunk = this._getChunkAtBlock(bx, bz);
    if (!chunk) return null;

    let best = -Infinity;

    for (const block of chunk.blocks.values()) {
      if (block.x !== bx || block.z !== bz) continue;
      if (!isSolidBlock(block.type)) continue;

      const surface = block.y + 1;
      if (surface <= maxSurfaceY && surface > best) {
        best = surface;
      }
    }

    return best === -Infinity ? null : best;
  }

  getSupportHeight(x, z, feetY, radius = 0.35, allowStepUp = false) {
    const STEP_UP = 1.05;
    const SNAP = 0.12;
    const maxSurfaceY = feetY + (allowStepUp ? STEP_UP + 0.15 : SNAP + 0.05);

    const offsets =
      radius <= 0
        ? [[0, 0]]
        : [
            [0, 0],
            [radius, 0],
            [-radius, 0],
            [0, radius],
            [0, -radius],
          ];

    let best = -Infinity;

    for (const [ox, oz] of offsets) {
      const support = this._getColumnSupportAt(x + ox, z + oz, maxSurfaceY);
      if (support !== null && support > best) best = support;
    }

    return best === -Infinity ? null : best;
  }


  collides(pos) {
    const bx = Math.floor(pos.x);
    const by = Math.floor(pos.y);
    const bz = Math.floor(pos.z);
    const chunk = this._getChunkAtBlock(bx, bz);
    if (!chunk) return false;
    const block = chunk.blocks.get(blockKey(bx, by, bz));
    if (!block) return false;
    return isSolidBlock(block.type);
  }

  isWaterAt(x, z) {
    const bx = Math.floor(x);
    const bz = Math.floor(z);
    const chunk = this._getChunkAtBlock(bx, bz);
    if (!chunk) return false;
    for (const block of chunk.blocks.values()) {
      if (block.x === bx && block.z === bz && block.type === 'water') return true;
    }
    return false;
  }

  getPickables() {
    return this.pickables;
  }

  removeBlock(x, y, z) {
    const key = blockKey(x, y, z);
    const { cx, cz } = this._chunkCoords(x, z);
    const chunk = this._getChunkAtBlock(x, z);
    if (!chunk || !chunk.blocks.has(key)) return null;

    const block = chunk.blocks.get(key);
    const type = block.type;
    chunk.blocks.delete(key);
    this._setModification(key, null, cx, cz);
    this._updateColumnTop(chunk, x, z);

    const refKey = meshBlockKey(block);
    if (!removeBlockInstance(chunk.refs, chunk.lists, refKey, this.pickables, this.pickableSet)) {
      this._queueRebuild(chunk);
    }

    return type;
  }

  placeBlock(x, y, z, typeId) {
    const key = blockKey(x, y, z);
    const { cx, cz } = this._chunkCoords(x, z);
    const chunk = this._getChunkAtBlock(x, z);
    if (!chunk || chunk.blocks.has(key)) return false;

    const block = { x, y, z, type: typeId };
    chunk.blocks.set(key, block);
    this._setModification(key, typeId, cx, cz);
    this._updateColumnTop(chunk, x, z);

    this._queueRebuild(chunk);
    return true;
  }

  dispose() {
    for (const chunk of [...this.chunks.values()]) {
      this.unloadChunk(chunk.cx, chunk.cz);
    }
    this.chunks.clear();
    this.modifications.clear();
    this.modsByChunk.clear();
    this.pickables = [];
    this.pickableSet.clear();
    this.loadQueue = [];
    this.rebuildQueue = [];
  }
}
