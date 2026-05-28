import { randomAt } from './noise.js';
import { placeTree } from './generateTrees.js';
import { stoneLayersForColumn } from './terrainRules.js';

export const CHUNK_SIZE = 16;

/**
 * Procedural blocks for one chunk (world coordinates).
 * @returns {{ x: number, y: number, z: number, type: string }[]}
 */
export function generateChunkBlocks(cx, cz) {
  const blocks = [];
  const baseX = cx * CHUNK_SIZE;
  const baseZ = cz * CHUNK_SIZE;

  const columnTop = new Map();

  function setColumnTop(wx, wz, y) {
    const key = `${wx},${wz}`;
    const prev = columnTop.get(key) ?? -1;
    if (y > prev) columnTop.set(key, y);
  }

  function getColumnTop(wx, wz) {
    const y = columnTop.get(`${wx},${wz}`);
    return y === undefined ? 0 : y + 1;
  }

  function add(wx, wy, wz, type) {
    blocks.push({ x: wx, y: wy, z: wz, type });
    setColumnTop(wx, wz, wy);
  }

  for (let lx = 0; lx < CHUNK_SIZE; lx++) {
    for (let lz = 0; lz < CHUNK_SIZE; lz++) {
      const wx = baseX + lx;
      const wz = baseZ + lz;

      for (const stone of stoneLayersForColumn(wx, wz)) {
        add(stone.x, stone.y, stone.z, stone.type);
      }

      add(wx, 0, wz, 'grass');

      if (randomAt(wx, wz) < 0.08) {
        add(wx, 1, wz, 'dirt');
        if (randomAt(wx + 7919, wz + 7919) < 0.5) {
          add(wx, 2, wz, 'dirt');
        }
      }
    }
  }

  for (let lx = 0; lx < CHUNK_SIZE; lx++) {
    for (let lz = 0; lz < CHUNK_SIZE; lz++) {
      const wx = baseX + lx;
      const wz = baseZ + lz;

      if (wx * wx + wz * wz < 36) continue;
      if (randomAt(wx * 3 + 17, wz * 7 + 31) > 0.006) continue;

      const top = getColumnTop(wx, wz);
      if (top > 2.5) continue;

      placeTree(wx, wz, (x, y, z, type) => {
        add(x, y, z, type);
        return true;
      }, (x, z) => getColumnTop(x, z));
    }
  }

  return blocks;
}
