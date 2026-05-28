/** How many stone blocks are built downward in each column (extended while mining). */
export const STONE_RENDER_DEPTH = 48;

const blockKey = (x, y, z) => `${x},${y},${z}`;

/**
 * Below the surface (y < 0) is infinite stone unless dug out.
 * @param {Map<string, string | null>} modifications
 */
export function isInfiniteStone(x, y, z, modifications) {
  if (y >= 0) return false;
  const key = blockKey(x, y, z);
  if (modifications.get(key) === null) return false;
  return true;
}

export function stoneLayersForColumn(wx, wz) {
  const blocks = [];
  for (let y = -STONE_RENDER_DEPTH; y < 0; y++) {
    blocks.push({ x: wx, y, z: wz, type: 'stone' });
  }
  return blocks;
}
