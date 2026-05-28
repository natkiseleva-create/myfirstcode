export const BLOCK_TYPES = {
  grass: {
    id: 'grass',
    label: 'Трава',
    topColor: 0x6ecf4a,
    sideColor: 0x5cb848,
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
  wood: {
    id: 'wood',
    label: 'Дерево',
    topColor: 0x8b6914,
    sideColor: 0x5c3d1e,
    stackSize: 64,
  },
  planks: {
    id: 'planks',
    label: 'Доски',
    topColor: 0xc9a86c,
    sideColor: 0xa08050,
    stackSize: 64,
  },
  crafting_table: {
    id: 'crafting_table',
    label: 'Верстак',
    topColor: 0xb8885a,
    sideColor: 0x8b5a2b,
    stackSize: 64,
  },
  leaves: {
    id: 'leaves',
    label: 'Листва',
    topColor: 0x3d8b37,
    sideColor: 0x2d6b28,
    stackSize: 64,
  },
  water: {
    id: 'water',
    label: 'Вода',
    topColor: 0x3a8fd4,
    sideColor: 0x2a6fa8,
    stackSize: 64,
    transparent: true,
  },
  sand: {
    id: 'sand',
    label: 'Песок',
    topColor: 0xd4c48a,
    sideColor: 0xc4b47a,
    stackSize: 64,
  },
  gold: {
    id: 'gold',
    label: 'Выход',
    topColor: 0xffd54f,
    sideColor: 0xffb300,
    stackSize: 64,
  },
};

export function getBlockType(id) {
  return BLOCK_TYPES[id] ?? null;
}
