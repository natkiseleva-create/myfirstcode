import * as THREE from 'three';

const raycaster = new THREE.Raycaster();
const screenCenter = new THREE.Vector2(0, 0);

/**
 * @param {THREE.Camera} camera
 * @param {THREE.Mesh[]} blockMeshes
 * @param {number} [maxDistance=5]
 * @returns {{ x: number, y: number, z: number } | null}
 */
export function pickBlock(camera, blockMeshes, maxDistance = 5) {
  if (blockMeshes.length === 0) return null;

  raycaster.setFromCamera(screenCenter, camera);
  raycaster.far = maxDistance;

  const hits = raycaster.intersectObjects(blockMeshes, false);
  if (hits.length === 0) return null;

  return hits[0].object.userData.block ?? null;
}
