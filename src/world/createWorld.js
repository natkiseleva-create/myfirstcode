import * as THREE from 'three';

const BLOCK_SIZE = 1;

function createBlockMaterial(topColor, sideColor) {
  const materials = [
    new THREE.MeshLambertMaterial({ color: sideColor }),
    new THREE.MeshLambertMaterial({ color: sideColor }),
    new THREE.MeshLambertMaterial({ color: topColor }),
    new THREE.MeshLambertMaterial({ color: 0x3d2817 }),
    new THREE.MeshLambertMaterial({ color: sideColor }),
    new THREE.MeshLambertMaterial({ color: sideColor }),
  ];
  return materials;
}

const blockKey = (x, y, z) => `${x},${y},${z}`;

/**
 * @returns {{
 *   scene: THREE.Scene,
 *   collides: (pos: THREE.Vector3) => boolean,
 *   getGroundHeight: (x: number, z: number) => number,
 *   getBlockMeshes: () => THREE.Mesh[],
 *   removeBlock: (x: number, y: number, z: number) => boolean,
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
  const grassMat = createBlockMaterial(0x5a9e3a, 0x6b4423);
  const stoneMat = createBlockMaterial(0x888888, 0x777777);
  const dirtMat = createBlockMaterial(0x6b4423, 0x5a3820);

  function addBlock(x, y, z, materials) {
    const key = blockKey(x, y, z);
    if (blocks.has(key)) return;

    const mesh = new THREE.Mesh(geometry, materials);
    mesh.position.set(x + 0.5, y + 0.5, z + 0.5);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    mesh.userData.block = { x, y, z };
    scene.add(mesh);

    blocks.set(key, mesh);
    blockPositions.add(key);
  }

  function removeBlock(x, y, z) {
    const key = blockKey(x, y, z);
    const mesh = blocks.get(key);
    if (!mesh) return false;

    scene.remove(mesh);
    blocks.delete(key);
    blockPositions.delete(key);
    return true;
  }

  const worldSize = 16;
  for (let x = -worldSize; x < worldSize; x++) {
    for (let z = -worldSize; z < worldSize; z++) {
      addBlock(x, 0, z, grassMat);
      if (Math.random() < 0.08) {
        addBlock(x, 1, z, dirtMat);
        if (Math.random() < 0.5) {
          addBlock(x, 2, z, dirtMat);
        }
      }
    }
  }

  addBlock(3, 1, 3, stoneMat);
  addBlock(3, 2, 3, stoneMat);
  addBlock(3, 3, 3, stoneMat);
  addBlock(4, 1, 3, stoneMat);
  addBlock(5, 1, 5, stoneMat);
  addBlock(5, 2, 5, stoneMat);
  addBlock(-6, 1, 4, stoneMat);
  addBlock(-6, 2, 4, stoneMat);
  addBlock(-6, 3, 4, stoneMat);

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
  };
}
