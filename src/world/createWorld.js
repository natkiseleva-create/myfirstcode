import * as THREE from 'three';
import { getBlockType } from '../blocks/blockTypes.js';

const BLOCK_SIZE = 1;
const materialCache = new Map();

function getMaterials(blockType) {
  if (materialCache.has(blockType.id)) {
    return materialCache.get(blockType.id);
  }

  const materials = [
    new THREE.MeshLambertMaterial({ color: blockType.sideColor }),
    new THREE.MeshLambertMaterial({ color: blockType.sideColor }),
    new THREE.MeshLambertMaterial({ color: blockType.topColor }),
    new THREE.MeshLambertMaterial({ color: 0x3d2817 }),
    new THREE.MeshLambertMaterial({ color: blockType.sideColor }),
    new THREE.MeshLambertMaterial({ color: blockType.sideColor }),
  ];
  materialCache.set(blockType.id, materials);
  return materials;
}

const blockKey = (x, y, z) => `${x},${y},${z}`;

/**
 * @returns {{
 *   scene: THREE.Scene,
 *   collides: (pos: THREE.Vector3) => boolean,
 *   getGroundHeight: (x: number, z: number) => number,
 *   getBlockMeshes: () => THREE.Mesh[],
 *   removeBlock: (x: number, y: number, z: number) => string | null,
 *   placeBlock: (x: number, y: number, z: number, typeId: string) => boolean,
 * }}
 */
export function createWorld() {
  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x87ceeb);
  scene.fog = new THREE.Fog(0x87ceeb, 40, 120);

  const ambient = new THREE.AmbientLight(0xffffff, 0.55);
  scene.add(ambient);

  const sun = new THREE.DirectionalLight(0xfff5e6, 0.85);
  sun.position.set(30, 50, 20);
  scene.add(sun);

  const blockPositions = new Set();
  const blocks = new Map();
  const geometry = new THREE.BoxGeometry(BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);

  function addBlock(x, y, z, typeId) {
    const blockType = getBlockType(typeId);
    if (!blockType) return false;

    const key = blockKey(x, y, z);
    if (blocks.has(key)) return false;

    const mesh = new THREE.Mesh(geometry, getMaterials(blockType));
    mesh.position.set(x + 0.5, y + 0.5, z + 0.5);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    mesh.userData.block = { x, y, z, type: typeId };
    scene.add(mesh);

    blocks.set(key, mesh);
    blockPositions.add(key);
    return true;
  }

  function removeBlock(x, y, z) {
    const key = blockKey(x, y, z);
    const mesh = blocks.get(key);
    if (!mesh) return null;

    const type = mesh.userData.block.type;
    scene.remove(mesh);
    blocks.delete(key);
    blockPositions.delete(key);
    return type;
  }

  function placeBlock(x, y, z, typeId) {
    return addBlock(x, y, z, typeId);
  }

  const worldSize = 16;
  for (let x = -worldSize; x < worldSize; x++) {
    for (let z = -worldSize; z < worldSize; z++) {
      addBlock(x, 0, z, 'grass');
      if (Math.random() < 0.08) {
        addBlock(x, 1, z, 'dirt');
        if (Math.random() < 0.5) {
          addBlock(x, 2, z, 'dirt');
        }
      }
    }
  }

  addBlock(3, 1, 3, 'stone');
  addBlock(3, 2, 3, 'stone');
  addBlock(3, 3, 3, 'stone');
  addBlock(4, 1, 3, 'stone');
  addBlock(5, 1, 5, 'stone');
  addBlock(5, 2, 5, 'stone');
  addBlock(-6, 1, 4, 'stone');
  addBlock(-6, 2, 4, 'stone');
  addBlock(-6, 3, 4, 'stone');

  function collides(pos) {
    const bx = Math.floor(pos.x);
    const by = Math.floor(pos.y);
    const bz = Math.floor(pos.z);
    return blockPositions.has(blockKey(bx, by, bz));
  }

  function getGroundHeight(x, z) {
    const bx = Math.floor(x);
    const bz = Math.floor(z);
    let maxY = -1;
    for (const key of blockPositions) {
      const [kx, ky, kz] = key.split(',').map(Number);
      if (kx === bx && kz === bz && ky > maxY) {
        maxY = ky;
      }
    }
    return maxY >= 0 ? maxY + 1 : 0;
  }

  function getBlockMeshes() {
    return [...blocks.values()];
  }

  return {
    scene,
    collides,
    getGroundHeight,
    getBlockMeshes,
    removeBlock,
    placeBlock,
  };
}
