import { randomAt } from './noise.js';

/**
 * Recursive backtracker maze.
 * @param {number} cells - odd number of cells (e.g. 15)
 * @returns {{
 *   width: number,
 *   height: number,
 *   grid: number[][],
 *   spawn: { x: number, z: number },
 *   exit: { x: number, z: number },
 * }}
 */
export function generateMazeGrid(cells = 15) {
  const w = cells * 2 + 1;
  const h = cells * 2 + 1;
  const grid = Array.from({ length: h }, () => Array(w).fill(1));

  const stack = [[1, 1]];
  grid[1][1] = 0;

  const dirs = [
    [2, 0],
    [-2, 0],
    [0, 2],
    [0, -2],
  ];

  while (stack.length > 0) {
    const [cx, cz] = stack[stack.length - 1];
    const shuffled = dirs
      .map((d, i) => ({ d, r: randomAt(cx + i, cz + i) }))
      .sort((a, b) => a.r - b.r)
      .map((x) => x.d);

    let carved = false;
    for (const [dx, dz] of shuffled) {
      const nx = cx + dx;
      const nz = cz + dz;
      if (nx > 0 && nx < w - 1 && nz > 0 && nz < h - 1 && grid[nz][nx] === 1) {
        grid[cz + dz / 2][cx + dx / 2] = 0;
        grid[nz][nx] = 0;
        stack.push([nx, nz]);
        carved = true;
        break;
      }
    }
    if (!carved) stack.pop();
  }

  grid[1][1] = 0;
  grid[h - 2][w - 2] = 0;

  return {
    width: w,
    height: h,
    grid,
    spawn: { x: 1, z: 1 },
    exit: { x: w - 2, z: h - 2 },
  };
}

/**
 * @returns {{ x: number, y: number, z: number, type: string }[]}
 */
export function mazeGridToBlocks(maze) {
  const blocks = [];
  const { grid, width, height, exit } = maze;
  const wallHeight = 3;

  for (let z = 0; z < height; z++) {
    for (let x = 0; x < width; x++) {
      if (grid[z][x] === 1) {
        for (let y = 0; y < wallHeight; y++) {
          blocks.push({ x, y, z, type: 'stone' });
        }
      } else {
        blocks.push({ x, y: 0, z, type: 'grass' });
        if (x === exit.x && z === exit.z) {
          blocks.push({ x, y: 1, z, type: 'gold' });
        }
      }
    }
  }

  return blocks;
}
