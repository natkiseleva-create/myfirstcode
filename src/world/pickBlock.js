import * as THREE from 'three';

const raycaster = new THREE.Raycaster();
const screenCenter = new THREE.Vector2(0, 0);
const worldNormal = new THREE.Vector3();

/**
 * @param {THREE.Intersection} hit
 * @returns {{ x: number, y: number, z: number, type?: string } | null}
 */
export function blockFromHit(hit) {
  if (hit.instanceId !== undefined && hit.object.userData.blocks) {
    const block = hit.object.userData.blocks[hit.instanceId];
    if (block) return block;
  }

  worldNormal.copy(hit.face.normal);
  if (hit.object.matrixWorld) {
    worldNormal.transformDirection(hit.object.matrixWorld);
  }

  const p = hit.point.clone();
  p.x -= worldNormal.x * 0.02;
  p.y -= worldNormal.y * 0.02;
  p.z -= worldNormal.z * 0.02;

  return {
    x: Math.floor(p.x),
    y: Math.floor(p.y),
    z: Math.floor(p.z),
  };
}

/**
 * @param {THREE.Camera} camera
 * @param {THREE.Object3D[]} pickables
 * @param {number} [maxDistance=5]
 */
export function raycastBlocks(camera, pickables, maxDistance = 5) {
  if (pickables.length === 0) return null;

  raycaster.setFromCamera(screenCenter, camera);
  raycaster.far = maxDistance;

  const hits = raycaster.intersectObjects(pickables, false);
  if (hits.length === 0) return null;

  return hits[0];
}

/**
 * @param {THREE.Camera} camera
 * @param {THREE.Object3D[]} pickables
 * @param {number} [maxDistance=5]
 * @returns {{ x: number, y: number, z: number, type: string } | null}
 */
export function pickBlock(camera, pickables, maxDistance = 5) {
  const hit = raycastBlocks(camera, pickables, maxDistance);
  if (!hit) return null;
  return blockFromHit(hit);
}

/**
 * @param {THREE.Camera} camera
 * @param {THREE.Object3D[]} pickables
 * @param {number} [maxDistance=5]
 */
export function pickBlockFace(camera, pickables, maxDistance = 5) {
  const hit = raycastBlocks(camera, pickables, maxDistance);
  if (!hit) return null;

  const block = blockFromHit(hit);
  if (!block) return null;

  worldNormal.copy(hit.face.normal);
  if (hit.object.matrixWorld) {
    worldNormal.transformDirection(hit.object.matrixWorld);
  }

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
