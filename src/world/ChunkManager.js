import * as THREE from 'three';
import { getBlockType } from '../blocks/blockTypes.js';
import { CHUNK_SIZE, generateChunkBlocks } from './chunkGenerator.js';

const BLOCK_SIZE = 1;
const VIEW_DISTANCE = 4;
const UNLOAD_DISTANCE = 5;

const materialCache = new Map();
const sharedGeometry = new THREE.BoxGeometry(BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);

function getMaterials(blockType) {
  if (materialCache.has(blockType.id)) {
    return materialCache.get(blockType.id);
  }

  const materials = [
    new THREE.MeshLambertMaterial({ color: blockType.sideColor }),
    new THREE.MeshLambertMaterial({ color: blockType.sideColor }),
    new THREE.MeshLambertMaterial({ color: blockType.topColor }),
    new THREE.MeshLambertMaterial({ color: 0x3d2817 }),
    new THREE.MeshLambertMaterial({ color: blockType.sideColor }),
    new THREE.MeshLambertMaterial({ color: blockType.sideColor }),
  ];
  materialCache.set(blockType.id, materials);
  return materials;
}

const blockKey = (x, y, z) => `${x},${y},${z}`;
const chunkKey = (cx, cz) => `${cx},${cz}`;

export class ChunkManager {
  /** @param {THREE.Scene} scene */
  constructor(scene) {
    this.scene = scene;
    this.chunks = new Map();
    this.blocks = new Map();
    this.modifications = new Map();
    this.meshList = [];
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
    const result = [];
    const seen = new Set();

    for (const block of procedural) {
      const key = blockKey(block.x, block.y, block.z);
      const mod = this.modifications.get(key);
      if (mod === null) continue;
      const type = mod ?? block.type;
      result.push({ x: block.x, y: block.y, z: block.z, type });
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
        result.push({ x, y, z, type });
        seen.add(key);
      }
    }

    return result;
  }

  _createMesh(x, y, z, typeId) {
    const blockType = getBlockType(typeId);
    if (!blockType) return null;

    const mesh = new THREE.Mesh(sharedGeometry, getMaterials(blockType));
    mesh.position.set(x + 0.5, y + 0.5, z + 0.5);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    mesh.userData.block = { x, y, z, type: typeId };
    return mesh;
  }

  _addBlockMesh(x, y, z, typeId) {
    const key = blockKey(x, y, z);
    if (this.blocks.has(key)) return false;

    const mesh = this._createMesh(x, y, z, typeId);
    if (!mesh) return false;

    this.scene.add(mesh);
    this.blocks.set(key, mesh);
    this.meshList.push(mesh);
    return true;
  }

  _removeBlockMesh(x, y, z) {
    const key = blockKey(x, y, z);
    const mesh = this.blocks.get(key);
    if (!mesh) return null;

    const type = mesh.userData.block.type;
    this.scene.remove(mesh);
    this.blocks.delete(key);
    const idx = this.meshList.indexOf(mesh);
    if (idx >= 0) this.meshList.splice(idx, 1);
    return type;
  }

  loadChunk(cx, cz) {
    const key = chunkKey(cx, cz);
    if (this.chunks.has(key)) return;

    const blockDefs = this._collectChunkBlocks(cx, cz);
    const blockKeys = [];

    for (const def of blockDefs) {
      if (this._addBlockMesh(def.x, def.y, def.z, def.type)) {
        blockKeys.push(blockKey(def.x, def.y, def.z));
      }
    }

    this.chunks.set(key, { cx, cz, blockKeys });
  }

  unloadChunk(cx, cz) {
    const key = chunkKey(cx, cz);
    const chunk = this.chunks.get(key);
    if (!chunk) return;

    for (const bk of chunk.blockKeys) {
      const mesh = this.blocks.get(bk);
      if (!mesh) continue;
      this.scene.remove(mesh);
      this.blocks.delete(bk);
      const idx = this.meshList.indexOf(mesh);
      if (idx >= 0) this.meshList.splice(idx, 1);
    }

    this.chunks.delete(key);
  }

  update(playerX, playerZ) {
    const pcx = Math.floor(playerX / CHUNK_SIZE);
    const pcz = Math.floor(playerZ / CHUNK_SIZE);

    if (pcx === this.lastPlayerChunk.cx && pcz === this.lastPlayerChunk.cz) {
      return;
    }
    this.lastPlayerChunk = { cx: pcx, cz: pcz };

    for (let dx = -VIEW_DISTANCE; dx <= VIEW_DISTANCE; dx++) {
      for (let dz = -VIEW_DISTANCE; dz <= VIEW_DISTANCE; dz++) {
        this.loadChunk(pcx + dx, pcz + dz);
      }
    }

    for (const [key, chunk] of this.chunks) {
      const dist = Math.max(
        Math.abs(chunk.cx - pcx),
        Math.abs(chunk.cz - pcz)
      );
      if (dist > UNLOAD_DISTANCE) {
        this.unloadChunk(chunk.cx, chunk.cz);
      }
    }
  }

  /** Force load around position (e.g. on spawn). */
  ensureLoaded(playerX, playerZ) {
    this.lastPlayerChunk = { cx: NaN, cz: NaN };
    this.update(playerX, playerZ);
  }

  getColumnTop(x, z) {
    const bx = Math.floor(x);
    const bz = Math.floor(z);
    let maxY = -1;

    for (const key of this.blocks.keys()) {
      const [kx, ky, kz] = key.split(',').map(Number);
      if (kx === bx && kz === bz && ky > maxY) {
        maxY = ky;
      }
    }

    if (maxY < 0) {
      const { cx, cz } = this._chunkCoords(bx, bz);
      const ck = chunkKey(cx, cz);
      if (!this.chunks.has(ck)) {
        this.loadChunk(cx, cz);
        return this.getColumnTop(x, z);
      }
      return 0;
    }

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

    let support = 0;
    for (const [ox, oz] of offsets) {
      const top = this.getColumnTop(x + ox, z + oz);
      if (top <= feetY + 0.15) {
        support = Math.max(support, top);
      }
    }
    return support;
  }

  collides(pos) {
    const bx = Math.floor(pos.x);
    const by = Math.floor(pos.y);
    const bz = Math.floor(pos.z);
    return this.blocks.has(blockKey(bx, by, bz));
  }

  getBlockMeshes() {
    return this.meshList;
  }

  removeBlock(x, y, z) {
    const key = blockKey(x, y, z);
    const type = this._removeBlockMesh(x, y, z);
    if (type !== null) {
      this.modifications.set(key, null);
      const { cx, cz } = this._chunkCoords(x, z);
      const chunk = this.chunks.get(chunkKey(cx, cz));
      if (chunk) {
        const idx = chunk.blockKeys.indexOf(key);
        if (idx >= 0) chunk.blockKeys.splice(idx, 1);
      }
    }
    return type;
  }

  placeBlock(x, y, z, typeId) {
    const key = blockKey(x, y, z);
    if (this.blocks.has(key)) return false;
    if (!this._addBlockMesh(x, y, z, typeId)) return false;

    this.modifications.set(key, typeId);
    const { cx, cz } = this._chunkCoords(x, z);
    const chunk = this.chunks.get(chunkKey(cx, cz));
    if (chunk && !chunk.blockKeys.includes(key)) {
      chunk.blockKeys.push(key);
    }
    return true;
  }
}
