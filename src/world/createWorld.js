import * as THREE from 'three';
import { GAME_MODES } from '../modes/gameModes.js';
import { ChunkManager } from './ChunkManager.js';
import { MazeWorld } from './MazeWorld.js';

function createScene(fogNear, fogFar) {
  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x87ceeb);
  scene.fog = new THREE.Fog(0x87ceeb, fogNear, fogFar);

  const ambient = new THREE.AmbientLight(0xffffff, 0.55);
  scene.add(ambient);

  const sun = new THREE.DirectionalLight(0xfff5e6, 0.85);
  sun.position.set(30, 50, 20);
  scene.add(sun);

  return scene;
}

/**
 * @param {'free' | 'maze'} mode
 */
export function createWorld(mode = GAME_MODES.FREE) {
  if (mode === GAME_MODES.MAZE) {
    const scene = createScene(20, 70);
    const maze = new MazeWorld(scene);
    const spawn = maze.getSpawn();

    return {
      mode,
      scene,
      spawn,
      updateWorld: () => maze.update(),
      ensureWorldLoaded: () => maze.ensureLoaded(),
      collides: (pos) => maze.collides(pos),
      getSupportHeight: (x, z, feetY, radius) =>
        maze.getSupportHeight(x, z, feetY, radius),
      getPickables: () => maze.getPickables(),
      removeBlock: (x, y, z) => maze.removeBlock(x, y, z),
      placeBlock: (x, y, z, typeId) => maze.placeBlock(x, y, z, typeId),
      checkWin: (x, z) => maze.checkWin(x, z),
      dispose: () => maze.dispose(),
    };
  }

  const scene = createScene(35, 90);
  const chunks = new ChunkManager(scene);

  return {
    mode,
    scene,
    spawn: { x: 0, z: 0 },
    updateWorld: (x, z) => chunks.update(x, z),
    ensureWorldLoaded: (x, z) => chunks.ensureLoaded(x, z),
    collides: (pos) => chunks.collides(pos),
    getSupportHeight: (x, z, feetY, radius) =>
      chunks.getSupportHeight(x, z, feetY, radius),
    getPickables: () => chunks.getPickables(),
    removeBlock: (x, y, z) => chunks.removeBlock(x, y, z),
    placeBlock: (x, y, z, typeId) => chunks.placeBlock(x, y, z, typeId),
    checkWin: () => false,
    dispose: () => chunks.dispose(),
  };
}
