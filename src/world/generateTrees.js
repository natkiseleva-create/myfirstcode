import { randomAt } from './noise.js';

const CROWN_RADIUS = 2;
const MIN_CROWN_LEAVES = 12;

/**
 * @param {number} x
 * @param {number} z
 * @param {(x: number, y: number, z: number, typeId: string) => boolean} addBlock
 * @param {(x: number, z: number) => number} getColumnTop
 */
export function placeTree(x, z, addBlock, getColumnTop) {
  const surfaceY = getColumnTop(x, z);
  const trunkBase = Math.floor(surfaceY);

  const trunkHeight = 4 + Math.floor(randomAt(x * 13 + 7, z * 11 + 3) * 3);
  let leavesPlaced = 0;

  const crownBase = trunkBase + trunkHeight - 2;

  for (let dx = -CROWN_RADIUS; dx <= CROWN_RADIUS; dx++) {
    for (let dy = 0; dy <= 3; dy++) {
      for (let dz = -CROWN_RADIUS; dz <= CROWN_RADIUS; dz++) {
        if (dx === 0 && dz === 0 && dy < trunkHeight - 1) continue;

        const dist = Math.abs(dx) + Math.abs(dy - 1) + Math.abs(dz);
        if (dist > 4) continue;

        if (addBlock(x + dx, crownBase + dy, z + dz, 'leaves')) {
          leavesPlaced++;
        }
      }
    }
  }

  if (leavesPlaced < MIN_CROWN_LEAVES) {
    for (let dx = -1; dx <= 1; dx++) {
      for (let dz = -1; dz <= 1; dz++) {
        if (addBlock(x + dx, crownBase + 2, z + dz, 'leaves')) {
          leavesPlaced++;
        }
      }
    }
    if (addBlock(x, crownBase + 3, z, 'leaves')) leavesPlaced++;
  }

  if (leavesPlaced < MIN_CROWN_LEAVES) {
    return false;
  }

  for (let y = trunkBase; y < trunkBase + trunkHeight; y++) {
    addBlock(x, y, z, 'wood');
  }

  return true;
}

/**
 * Remove wood blocks that are not part of a tree crown (no leaves nearby).
 * @param {{ x: number, y: number, z: number, type: string }[]} blocks
 */
export function pruneOrphanWood(blocks) {
  const byKey = new Map();
  for (const block of blocks) {
    byKey.set(`${block.x},${block.y},${block.z}`, block);
  }

  const hasLeafNear = (x, y, z) => {
    for (let dy = 0; dy <= 6; dy++) {
      for (let dx = -3; dx <= 3; dx++) {
        for (let dz = -3; dz <= 3; dz++) {
          const neighbor = byKey.get(`${x + dx},${y + dy},${z + dz}`);
          if (neighbor?.type === 'leaves') return true;
        }
      }
    }
    return false;
  };

  return blocks.filter((block) => {
    if (block.type !== 'wood') return true;
    return hasLeafNear(block.x, block.y, block.z);
  });
}

/**
 * @param {object} options
 * @param {(x: number, y: number, z: number, typeId: string) => boolean} options.addBlock
 * @param {(x: number, z: number) => number} options.getColumnTop
 * @param {number} [options.worldSize=16]
 * @param {number} [options.count=35]
 * @param {number} [options.spawnClearRadius=4]
 */
export function generateTrees({
  addBlock,
  getColumnTop,
  worldSize = 16,
  count = 35,
  spawnClearRadius = 4,
}) {
  let placed = 0;
  let attempts = 0;
  const maxAttempts = count * 12;

  while (placed < count && attempts < maxAttempts) {
    attempts++;
    const x = Math.floor(Math.random() * (worldSize * 2 - 2)) - worldSize + 1;
    const z = Math.floor(Math.random() * (worldSize * 2 - 2)) - worldSize + 1;

    if (x * x + z * z < spawnClearRadius * spawnClearRadius) continue;

    const top = getColumnTop(x, z);
    if (top > 2.5) continue;

    if (placeTree(x, z, addBlock, getColumnTop)) placed++;
  }
}
