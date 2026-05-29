/**
 * Grid recipes: pattern uses null for empty cells.
 * grid: 2 = player inventory craft, 3 = needs crafting table UI mode.
 */

/** @typedef {{ type: string, count: number }} Stack */

/**
 * @param {(string|null)[][]} pattern
 * @param {number} gridSize
 */
function normalizePattern(pattern, gridSize) {
  const rows = [];
  for (let y = 0; y < gridSize; y++) {
    const row = pattern[y] ?? [];
    const cells = [];
    for (let x = 0; x < gridSize; x++) {
      cells.push(row[x] ?? null);
    }
    rows.push(cells);
  }
  return rows;
}

/**
 * @param {(string|null)[][]} grid - gridSize x gridSize types or null
 * @param {(string|null)[][]} pattern
 */
function patternMatches(grid, pattern) {
  const size = grid.length;
  const patH = pattern.length;
  const patW = pattern[0]?.length ?? 0;

  for (let offY = 0; offY <= size - patH; offY++) {
    for (let offX = 0; offX <= size - patW; offX++) {
      let ok = true;
      for (let y = 0; y < size && ok; y++) {
        for (let x = 0; x < size; x++) {
          const py = y - offY;
          const px = x - offX;
          let expected = null;
          if (py >= 0 && py < patH && px >= 0 && px < patW) {
            expected = pattern[py][px];
          }
          const got = grid[y][x];
          if (expected !== got) {
            ok = false;
            break;
          }
        }
      }
      if (ok) return true;
    }
  }
  return false;
}

/**
 * @param {(string|null)[][]} grid
 * @param {{ type: string, count: number }}[] | null} ingredients
 */
function shapelessMatches(grid, ingredients) {
  if (!ingredients?.length) return false;
  const flat = grid.flat().filter(Boolean);
  if (flat.length === 0) return false;

  const needed = ingredients.map((i) => ({ ...i }));

  for (const cell of flat) {
    const slot = needed.find((n) => n.type === cell && n.count > 0);
    if (!slot) return false;
    slot.count -= 1;
  }

  return needed.every((n) => n.count <= 0) && flat.length === ingredients.reduce((s, i) => s + i.count, 0);
}

/** @type {{ id: string, grid: number, pattern?: (string|null)[][], shapeless?: { type: string, count: number }[], output: Stack }[]} */
export const GRID_RECIPES = [
  {
    id: 'planks',
    grid: 2,
    shapeless: [{ type: 'wood', count: 1 }],
    output: { type: 'planks', count: 4 },
  },
  {
    id: 'crafting_table',
    grid: 2,
    pattern: [
      ['planks', 'planks'],
      ['planks', 'planks'],
    ],
    output: { type: 'crafting_table', count: 1 },
  },
  {
    id: 'planks_3',
    grid: 3,
    shapeless: [{ type: 'wood', count: 1 }],
    output: { type: 'planks', count: 4 },
  },
  {
    id: 'crafting_table_3',
    grid: 3,
    pattern: [
      ['planks', 'planks', null],
      ['planks', 'planks', null],
      [null, null, null],
    ],
    output: { type: 'crafting_table', count: 1 },
  },
];

/**
 * @param {(string|null)[][]} cells - size x size
 * @param {number} gridSize
 * @returns {{ recipe: typeof GRID_RECIPES[0], output: Stack } | null}
 */
export function matchRecipe(cells, gridSize) {
  const grid = normalizePattern(cells, gridSize);

  for (const recipe of GRID_RECIPES) {
    if (recipe.grid !== gridSize) continue;

    if (recipe.shapeless) {
      if (shapelessMatches(grid, recipe.shapeless)) {
        return { recipe, output: { ...recipe.output } };
      }
      continue;
    }

    if (recipe.pattern && patternMatches(grid, normalizePattern(recipe.pattern, gridSize))) {
      return { recipe, output: { ...recipe.output } };
    }
  }

  return null;
}
