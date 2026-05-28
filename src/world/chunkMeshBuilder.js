import * as THREE from 'three';
import { getBlockType } from '../blocks/blockTypes.js';

const BOX = new THREE.BoxGeometry(1, 1, 1);
const materialCache = new Map();

function getMaterial(typeId) {
  if (materialCache.has(typeId)) return materialCache.get(typeId);

  const blockType = getBlockType(typeId);
  if (!blockType) return null;

  const mat = new THREE.MeshLambertMaterial({ color: blockType.sideColor });
  materialCache.set(typeId, mat);
  return mat;
}

const _dummy = new THREE.Object3D();

/**
 * @param {{ x: number, y: number, z: number, type: string }[]} blocks
 * @returns {{ group: THREE.Group, pickables: THREE.InstancedMesh[] }}
 */
export function buildChunkMeshes(blocks) {
  const group = new THREE.Group();
  const pickables = [];
  const byType = new Map();

  for (const block of blocks) {
    if (!byType.has(block.type)) byType.set(block.type, []);
    byType.get(block.type).push(block);
  }

  for (const [typeId, list] of byType) {
    const material = getMaterial(typeId);
    if (!material || list.length === 0) continue;

    const mesh = new THREE.InstancedMesh(BOX, material, list.length);
    mesh.frustumCulled = true;

    for (let i = 0; i < list.length; i++) {
      const b = list[i];
      _dummy.position.set(b.x + 0.5, b.y + 0.5, b.z + 0.5);
      _dummy.updateMatrix();
      mesh.setMatrixAt(i, _dummy.matrix);
    }

    mesh.instanceMatrix.needsUpdate = true;
    mesh.userData.blocks = list;

    group.add(mesh);
    pickables.push(mesh);
  }

  return { group, pickables };
}

export function disposeChunkMeshes(group) {
  while (group.children.length > 0) {
    group.remove(group.children[0]);
  }
}
