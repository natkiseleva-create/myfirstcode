/**
 * @param {number} x
 * @param {number} z
 * @param {(x: number, y: number, z: number, typeId: string) => boolean} addBlock
 * @param {(x: number, z: number) => number} getColumnTop
 */
export function placeTree(x, z, addBlock, getColumnTop) {
  const surfaceY = getColumnTop(x, z);
  const trunkBase = Math.floor(surfaceY);

  const trunkHeight = 4 + Math.floor(Math.random() * 3);

  for (let y = trunkBase; y < trunkBase + trunkHeight; y++) {
    addBlock(x, y, z, 'wood');
  }

  const crownBase = trunkBase + trunkHeight - 2;

  for (let dx = -2; dx <= 2; dx++) {
    for (let dy = 0; dy <= 3; dy++) {
      for (let dz = -2; dz <= 2; dz++) {
        if (dx === 0 && dz === 0 && dy < trunkHeight - 1) continue;

        const dist = Math.abs(dx) + Math.abs(dy - 1) + Math.abs(dz);
        if (dist > 4) continue;
        if (Math.random() < 0.12 && dist > 2) continue;

        addBlock(x + dx, crownBase + dy, z + dz, 'leaves');
      }
    }
  }
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

    placeTree(x, z, addBlock, getColumnTop);
    placed++;
  }
}
