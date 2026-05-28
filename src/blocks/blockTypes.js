export const BLOCK_TYPES = {
  grass: {
    id: 'grass',
    label: 'Трава',
    topColor: 0x5a9e3a,
    sideColor: 0x6b4423,
    stackSize: 64,
  },
  dirt: {
    id: 'dirt',
    label: 'Земля',
    topColor: 0x6b4423,
    sideColor: 0x5a3820,
    stackSize: 64,
  },
  stone: {
    id: 'stone',
    label: 'Камень',
    topColor: 0x888888,
    sideColor: 0x777777,
    stackSize: 64,
  },
};

export function getBlockType(id) {
  return BLOCK_TYPES[id] ?? null;
}
