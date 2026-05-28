import * as THREE from 'three';
import { getBlockType } from '../blocks/blockTypes.js';

const BOX = new THREE.BoxGeometry(1, 1, 1);
const materialCache = new Map();
const _dummy = new THREE.Object3D();
const _matrix = new THREE.Matrix4();

function getMaterial(typeId) {
  if (materialCache.has(typeId)) return materialCache.get(typeId);

  const blockType = getBlockType(typeId);
  if (!blockType) return null;

  const mat = new THREE.MeshLambertMaterial({
    color: blockType.sideColor,
    transparent: Boolean(blockType.transparent),
    opacity: blockType.transparent ? 0.78 : 1,
  });
  materialCache.set(typeId, mat);
  return mat;
}

function setInstanceMatrix(mesh, index, block) {
  _dummy.position.set(block.x + 0.5, block.y + 0.5, block.z + 0.5);
  _dummy.updateMatrix();
  mesh.setMatrixAt(index, _dummy.matrix);
}

/**
 * @param {{ x: number, y: number, z: number, type: string }[]} blocks
 * @returns {{
 *   group: THREE.Group,
 *   pickables: THREE.InstancedMesh[],
 *   refs: Map<string, { mesh: THREE.InstancedMesh, index: number, typeId: string }>,
 *   lists: Map<string, { x: number, y: number, z: number, type: string }[]>,
 * }}
 */
export function buildChunkMeshes(blocks) {
  const group = new THREE.Group();
  const pickables = [];
  const refs = new Map();
  const lists = new Map();

  for (const block of blocks) {
    if (!lists.has(block.type)) lists.set(block.type, []);
    lists.get(block.type).push(block);
  }

  for (const [typeId, list] of lists) {
    const material = getMaterial(typeId);
    if (!material || list.length === 0) continue;

    const mesh = new THREE.InstancedMesh(BOX, material, list.length);
    mesh.frustumCulled = false;

    for (let i = 0; i < list.length; i++) {
      setInstanceMatrix(mesh, i, list[i]);
      refs.set(blockRefKey(list[i]), { mesh, index: i, typeId });
    }

    mesh.instanceMatrix.needsUpdate = true;
    mesh.userData.blocks = list;
    mesh.userData.typeId = typeId;

    group.add(mesh);
    pickables.push(mesh);
  }

  return { group, pickables, refs, lists };
}

export function blockRefKey(block) {
  return `${block.x},${block.y},${block.z}`;
}

/**
 * Remove one instance without rebuilding the whole chunk.
 * @returns {boolean}
 */
export function removeBlockInstance(refs, lists, blockKeyStr, pickables, pickableSet) {
  const ref = refs.get(blockKeyStr);
  if (!ref) return false;

  const list = lists.get(ref.typeId);
  const mesh = ref.mesh;
  const removeIndex = ref.index;
  const lastIndex = list.length - 1;

  if (removeIndex !== lastIndex) {
    const lastBlock = list[lastIndex];
    list[removeIndex] = lastBlock;
    setInstanceMatrix(mesh, removeIndex, lastBlock);
    const lastKey = blockRefKey(lastBlock);
    refs.set(lastKey, { mesh, index: removeIndex, typeId: ref.typeId });
  }

  list.pop();
  refs.delete(blockKeyStr);

  if (list.length === 0) {
    mesh.parent?.remove(mesh);
    mesh.dispose();
    lists.delete(ref.typeId);
    const pi = pickables.indexOf(mesh);
    if (pi >= 0) pickables.splice(pi, 1);
    pickableSet?.delete(mesh);
    return true;
  }

  mesh.count = list.length;
  mesh.instanceMatrix.needsUpdate = true;
  mesh.userData.blocks = list;
  return true;
}

export function disposeChunkMeshes(group, pickables) {
  for (const mesh of pickables) {
    mesh.parent?.remove(mesh);
    mesh.dispose();
  }
  pickables.length = 0;
  while (group.children.length > 0) {
    group.remove(group.children[0]);
  }
}
