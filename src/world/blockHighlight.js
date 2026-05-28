import * as THREE from 'three';
import { blockFromHit, raycastBlocks } from './pickBlock.js';

const BOX = new THREE.BoxGeometry(1.002, 1.002, 1.002);
const EDGES = new THREE.EdgesGeometry(BOX);
const MATERIAL = new THREE.LineBasicMaterial({ color: 0x000000 });

let highlightMesh = null;

/**
 * @param {THREE.Scene} scene
 */
export function createBlockHighlight(scene) {
  if (highlightMesh) {
    highlightMesh.removeFromParent();
  }

  highlightMesh = new THREE.LineSegments(EDGES, MATERIAL);
  highlightMesh.visible = false;
  highlightMesh.frustumCulled = false;
  scene.add(highlightMesh);
  return highlightMesh;
}

export function hideBlockHighlight() {
  if (highlightMesh) highlightMesh.visible = false;
}

/**
 * @param {THREE.Camera} camera
 * @param {THREE.Object3D[]} pickables
 * @param {number} [maxDistance=5]
 */
export function updateBlockHighlight(camera, pickables, maxDistance = 5) {
  if (!highlightMesh) return;

  const hit = raycastBlocks(camera, pickables, maxDistance);
  if (!hit) {
    highlightMesh.visible = false;
    return;
  }

  const block = blockFromHit(hit);
  if (!block) {
    highlightMesh.visible = false;
    return;
  }

  highlightMesh.position.set(block.x + 0.5, block.y + 0.5, block.z + 0.5);
  highlightMesh.visible = true;
}

export function disposeBlockHighlight() {
  if (!highlightMesh) return;
  highlightMesh.geometry.dispose();
  highlightMesh.removeFromParent();
  highlightMesh = null;
}
