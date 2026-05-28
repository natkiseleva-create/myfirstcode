import { buildChunkMeshes, disposeChunkMeshes } from './chunkMeshBuilder.js';
import { generateMazeGrid, mazeGridToBlocks } from './generateMaze.js';
import { isInfiniteStone } from './terrainRules.js';

const blockKey = (x, y, z) => `${x},${y},${z}`;

export class MazeWorld {
  /** @param {THREE.Scene} scene */
  constructor(scene) {
    this.scene = scene;
    this.maze = generateMazeGrid(15);
    this.blocks = new Map();
    this.modifications = new Map();
    this.pickables = [];
    this.group = null;
    this.exit = {
      x: this.maze.exit.x + 0.5,
      z: this.maze.exit.z + 0.5,
    };
    this.won = false;

    const blockList = mazeGridToBlocks(this.maze);
    for (const b of blockList) {
      this.blocks.set(blockKey(b.x, b.y, b.z), b);
    }

    this._buildMeshes();
  }

  _buildMeshes() {
    if (this.group) {
      for (const mesh of this.pickables) {
        const idx = this.pickables.indexOf(mesh);
        if (idx >= 0) this.pickables.splice(idx, 1);
      }
      this.scene.remove(this.group);
      disposeChunkMeshes(this.group);
    }

    const { group, pickables } = buildChunkMeshes([...this.blocks.values()]);
    this.group = group;
    this.pickables = pickables;
    this.scene.add(group);
  }

  _extendStoneDown(x, z, y) {
    if (y >= 0) return;
    const key = blockKey(x, y, z);
    if (this.modifications.get(key) === null) return;
    if (this.blocks.has(key)) return;
    this.blocks.set(key, { x, y, z, type: 'stone' });
  }

  dispose() {
    if (this.group) {
      this.scene.remove(this.group);
      disposeChunkMeshes(this.group);
      this.group = null;
    }
    this.pickables = [];
    this.blocks.clear();
    this.modifications.clear();
  }

  getSpawn() {
    return {
      x: this.maze.spawn.x + 0.5,
      z: this.maze.spawn.z + 0.5,
    };
  }

  update() {}

  ensureLoaded() {}

  getColumnTop(x, z) {
    const bx = Math.floor(x);
    const bz = Math.floor(z);
    let maxY = -Infinity;
    for (const block of this.blocks.values()) {
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
      if (top <= feetY + 0.15) support = Math.max(support, top);
    }
    return support === -Infinity ? 0 : support;
  }

  collides(pos) {
    const bx = Math.floor(pos.x);
    const by = Math.floor(pos.y);
    const bz = Math.floor(pos.z);
    const key = blockKey(bx, by, bz);
    if (this.blocks.has(key)) return true;
    return isInfiniteStone(bx, by, bz, this.modifications);
  }

  getPickables() {
    return this.pickables;
  }

  removeBlock(x, y, z) {
    const key = blockKey(x, y, z);
    const inMap = this.blocks.has(key);
    const procedural = isInfiniteStone(x, y, z, this.modifications);
    if (!inMap && !procedural) return null;

    let type = 'stone';
    if (inMap) {
      type = this.blocks.get(key).type;
      this.blocks.delete(key);
    }

    this.modifications.set(key, null);

    if (y < 0 && isInfiniteStone(x, y - 1, z, this.modifications)) {
      this._extendStoneDown(x, z, y - 1);
    }

    this._buildMeshes();
    return type;
  }

  placeBlock(x, y, z, typeId) {
    const key = blockKey(x, y, z);
    if (
      this.blocks.has(key) ||
      isInfiniteStone(x, y, z, this.modifications)
    ) {
      return false;
    }
    this.blocks.set(key, { x, y, z, type: typeId });
    this.modifications.set(key, typeId);
    this._buildMeshes();
    return true;
  }

  checkWin(playerX, playerZ) {
    if (this.won) return true;
    const dx = playerX - this.exit.x;
    const dz = playerZ - this.exit.z;
    if (dx * dx + dz * dz < 1.2) {
      this.won = true;
      return true;
    }
    return false;
  }
}
