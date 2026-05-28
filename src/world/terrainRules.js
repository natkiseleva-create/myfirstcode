import { SEA_LEVEL, getSurfaceBlock, isWaterColumn } from './terrainGenerator.js';

/** Stone layers below surface. */
export const STONE_DEPTH = 20;

const NON_SOLID = new Set(['water']);


export function stoneLayersForColumn(wx, wz) {
  const blocks = [];
  for (let y = -STONE_DEPTH; y < 0; y++) {
    blocks.push({ x: wx, y, z: wz, type: 'stone' });
  }
  return blocks;
}

export function isSolidBlock(typeId) {
  return !NON_SOLID.has(typeId);
}

/**
 * Fill a terrain column (solids + optional water).
 * @param {number} wx
 * @param {number} wz
 * @param {number} surfaceY top solid block Y
 * @param {(x: number, y: number, z: number, type: string) => void} add
 */
export function fillTerrainColumn(wx, wz, surfaceY, add) {
  for (let y = -STONE_DEPTH; y < surfaceY; y++) {
    const type = y < surfaceY - 3 || y < 0 ? 'stone' : 'dirt';
    add(wx, y, wz, type);
  }

  const surfaceBlock = isWaterColumn(wx, wz)
    ? 'sand'
    : getSurfaceBlock(wx, wz, surfaceY);
  add(wx, surfaceY, wz, surfaceBlock);

  if (surfaceY < SEA_LEVEL) {
    for (let y = surfaceY + 1; y <= SEA_LEVEL; y++) {
      add(wx, y, wz, 'water');
    }
  }
}
