/** @type {Record<string, { label: string, input: { type: string, count: number }, output: { type: string, count: number } }>} */
export const CRAFT_RECIPES = {
  wood_to_planks: {
    label: 'Доски',
    input: { type: 'wood', count: 1 },
    output: { type: 'planks', count: 4 },
  },
};
