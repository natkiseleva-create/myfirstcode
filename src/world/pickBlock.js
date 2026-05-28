import * as THREE from 'three';

const raycaster = new THREE.Raycaster();
const screenCenter = new THREE.Vector2(0, 0);
const worldNormal = new THREE.Vector3();

/**
 * @param {THREE.Camera} camera
 * @param {THREE.Mesh[]} blockMeshes
 * @param {number} [maxDistance=5]
 * @returns {{ x: number, y: number, z: number, type: string } | null}
 */
export function pickBlock(camera, blockMeshes, maxDistance = 5) {
  const hit = raycastBlock(camera, blockMeshes, maxDistance);
  if (!hit) return null;
  return hit.object.userData.block ?? null;
}

/**
 * @param {THREE.Camera} camera
 * @param {THREE.Mesh[]} blockMeshes
 * @param {number} [maxDistance=5]
 * @returns {{ block: { x: number, y: number, z: number, type: string }, place: { x: number, y: number, z: number } } | null}
 */
export function pickBlockFace(camera, blockMeshes, maxDistance = 5) {
  const hit = raycastBlock(camera, blockMeshes, maxDistance);
  if (!hit) return null;

  const block = hit.object.userData.block;
  if (!block) return null;

  worldNormal.copy(hit.face.normal).transformDirection(hit.object.matrixWorld);
  const nx = Math.round(worldNormal.x);
  const ny = Math.round(worldNormal.y);
  const nz = Math.round(worldNormal.z);

  return {
    block,
    place: {
      x: block.x + nx,
      y: block.y + ny,
      z: block.z + nz,
    },
  };
}

function raycastBlock(camera, blockMeshes, maxDistance) {
  if (blockMeshes.length === 0) return null;

  raycaster.setFromCamera(screenCenter, camera);
  raycaster.far = maxDistance;

  const hits = raycaster.intersectObjects(blockMeshes, false);
  if (hits.length === 0) return null;

  return hits[0];
}
