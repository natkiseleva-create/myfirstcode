/** Stone layers below grass (y = -1 … -STONE_DEPTH). */
export const STONE_DEPTH = 20;

export function stoneLayersForColumn(wx, wz) {
  const blocks = [];
  for (let y = -STONE_DEPTH; y < 0; y++) {
    blocks.push({ x: wx, y, z: wz, type: 'stone' });
  }
  return blocks;
}
