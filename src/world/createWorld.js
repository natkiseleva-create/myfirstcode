import * as THREE from 'three';
import { ChunkManager } from './ChunkManager.js';

/**
 * @returns {{
 *   scene: THREE.Scene,
 *   updateWorld: (playerX: number, playerZ: number) => void,
 *   collides: (pos: THREE.Vector3) => boolean,
 *   getSupportHeight: (x: number, z: number, feetY: number, radius?: number) => number,
 *   getPickables: () => THREE.Mesh[],
 *   removeBlock: (x: number, y: number, z: number) => string | null,
 *   placeBlock: (x: number, y: number, z: number, typeId: string) => boolean,
 * }}
 */
export function createWorld() {
  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x87ceeb);
  scene.fog = new THREE.Fog(0x87ceeb, 50, 140);

  const ambient = new THREE.AmbientLight(0xffffff, 0.55);
  scene.add(ambient);

  const sun = new THREE.DirectionalLight(0xfff5e6, 0.85);
  sun.position.set(30, 50, 20);
  scene.add(sun);

  const chunks = new ChunkManager(scene);

  return {
    scene,
    updateWorld: (x, z) => chunks.update(x, z),
    ensureWorldLoaded: (x, z) => chunks.ensureLoaded(x, z),
    collides: (pos) => chunks.collides(pos),
    getSupportHeight: (x, z, feetY, radius) =>
      chunks.getSupportHeight(x, z, feetY, radius),
    getPickables: () => chunks.getPickables(),
    removeBlock: (x, y, z) => chunks.removeBlock(x, y, z),
    placeBlock: (x, y, z, typeId) => chunks.placeBlock(x, y, z, typeId),
  };
}
