import { fbm, randomAt } from './noise.js';

export const SEA_LEVEL = 4;
const MIN_HEIGHT = 2;
const MAX_HEIGHT = 14;

/**
 * Solid surface height (top block Y) for a column.
 * @param {number} wx
 * @param {number} wz
 */
export function getTerrainHeight(wx, wz) {
  const hills = fbm(wx * 0.012, wz * 0.012, 5, 11);
  const detail = fbm(wx * 0.045, wz * 0.045, 3, 29) * 0.35;
  let height = Math.floor(SEA_LEVEL + 2 + hills * 9 + detail * 4);

  const riverValley = Math.abs(fbm(wx * 0.028 + 40, wz * 0.028 - 20, 4, 53) - 0.5);
  if (riverValley < 0.045) {
    height = Math.min(height, SEA_LEVEL);
  }

  const lakeBasin = fbm(wx * 0.018 + 120, wz * 0.018 + 80, 4, 71);
  if (lakeBasin < 0.2) {
    height = Math.min(height, SEA_LEVEL - 1 + Math.floor(lakeBasin * 5));
  }

  return Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height));
}

/** @param {number} wx @param {number} wz */
export function isWaterColumn(wx, wz) {
  return getTerrainHeight(wx, wz) < SEA_LEVEL;
}

/** Surface block on land (not underwater floor). */
export function getSurfaceBlock(wx, wz, surfaceY) {
  if (isWaterColumn(wx, wz)) {
    return 'sand';
  }

  if (surfaceY <= SEA_LEVEL + 1 && hasWaterNear(wx, wz, 2)) {
    return 'sand';
  }

  if (surfaceY > SEA_LEVEL + 7 && randomAt(wx + 3, wz + 9, 17) < 0.08) {
    return 'stone';
  }

  return 'grass';
}

function hasWaterNear(wx, wz, radius) {
  for (let dx = -radius; dx <= radius; dx++) {
    for (let dz = -radius; dz <= radius; dz++) {
      if (isWaterColumn(wx + dx, wz + dz)) return true;
    }
  }
  return false;
}

/** Can trees grow here? */
export function canPlaceTree(wx, wz) {
  if (isWaterColumn(wx, wz)) return false;
  if (hasWaterNear(wx, wz, 1)) return false;
  const h = getTerrainHeight(wx, wz);
  return h >= SEA_LEVEL + 2 && h <= MAX_HEIGHT - 3;
}
