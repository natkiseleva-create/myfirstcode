import * as THREE from 'three';
import { FirstPersonController } from './controls/FirstPersonController.js';
import { createWorld } from './world/createWorld.js';
import { pickBlock, pickBlockFace } from './world/pickBlock.js';
import { Inventory } from './inventory/Inventory.js';
import { InventoryUI } from './ui/InventoryUI.js';

const canvas = document.getElementById('game');
const overlay = document.getElementById('overlay');

const PLAYER_HEIGHT = 1.7;
const PLAYER_RADIUS = 0.35;
const REACH = 5;

const renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(window.innerWidth, window.innerHeight);

const camera = new THREE.PerspectiveCamera(
  75,
  window.innerWidth / window.innerHeight,
  0.1,
  200
);

const { scene, collides, getGroundHeight, getBlockMeshes, removeBlock, placeBlock } =
  createWorld();

const inventory = new Inventory();
const inventoryUI = new InventoryUI(inventory);

const controller = new FirstPersonController(camera, canvas, {
  groundY: 0,
  collides,
});

const spawnX = 0;
const spawnZ = 0;
const spawnGround = getGroundHeight(spawnX, spawnZ);
controller.setPosition(spawnX, spawnGround, spawnZ);

let lastTime = performance.now();

function playerOccupiesBlock(x, y, z) {
  const minX = camera.position.x - PLAYER_RADIUS;
  const maxX = camera.position.x + PLAYER_RADIUS;
  const minY = camera.position.y - PLAYER_HEIGHT;
  const maxY = camera.position.y;
  const minZ = camera.position.z - PLAYER_RADIUS;
  const maxZ = camera.position.z + PLAYER_RADIUS;

  const blockMinX = x;
  const blockMaxX = x + 1;
  const blockMinY = y;
  const blockMaxY = y + 1;
  const blockMinZ = z;
  const blockMaxZ = z + 1;

  return (
    maxX > blockMinX &&
    minX < blockMaxX &&
    maxY > blockMinY &&
    minY < blockMaxY &&
    maxZ > blockMinZ &&
    minZ < blockMaxZ
  );
}

function tryBreakBlock() {
  if (!controller.isLocked || inventoryUI.isOpen) return;

  const target = pickBlock(camera, getBlockMeshes(), REACH);
  if (!target) return;

  const type = removeBlock(target.x, target.y, target.z);
  if (type) {
    inventory.addItem(type, 1);
  }
}

function tryPlaceBlock() {
  if (!controller.isLocked || inventoryUI.isOpen) return;
  if (!inventory.hasSelected()) return;

  const hit = pickBlockFace(camera, getBlockMeshes(), REACH);
  if (!hit) return;

  const { place } = hit;
  const selected = inventory.getSelectedItem();
  if (!selected.type) return;

  if (playerOccupiesBlock(place.x, place.y, place.z)) return;
  if (collides(new THREE.Vector3(place.x + 0.5, place.y + 0.5, place.z + 0.5))) {
    return;
  }

  if (placeBlock(place.x, place.y, place.z, selected.type)) {
    inventory.consumeSelected(1);
  }
}

function toggleInventory() {
  const opening = inventoryUI.toggle();

  if (opening) {
    document.exitPointerLock();
    overlay.classList.add('hidden');
  } else if (document.pointerLockElement !== canvas) {
    overlay.classList.remove('hidden');
  }
}

function animate(now) {
  requestAnimationFrame(animate);
  const dt = Math.min((now - lastTime) / 1000, 0.05);
  lastTime = now;

  if (controller.isLocked && !inventoryUI.isOpen) {
    const feetX = camera.position.x;
    const feetZ = camera.position.z;
    controller.groundY = getGroundHeight(feetX, feetZ);
    controller.update(dt);
  }

  renderer.render(scene, camera);
}

function onResize() {
  const w = window.innerWidth;
  const h = window.innerHeight;
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
  renderer.setSize(w, h);
}

overlay.addEventListener('click', () => {
  if (inventoryUI.isOpen) return;
  controller.requestPointerLock();
  overlay.classList.add('hidden');
});

canvas.addEventListener('mousedown', (event) => {
  if (!controller.isLocked || inventoryUI.isOpen) return;

  if (event.button === 0) {
    tryBreakBlock();
  } else if (event.button === 2) {
    tryPlaceBlock();
  }
});

canvas.addEventListener('wheel', (event) => {
  if (!controller.isLocked || inventoryUI.isOpen) return;
  event.preventDefault();
  inventory.cycleSelection(event.deltaY > 0 ? 1 : -1);
});

canvas.addEventListener('contextmenu', (event) => {
  event.preventDefault();
});

document.addEventListener('keydown', (event) => {
  if (event.code === 'KeyE') {
    toggleInventory();
    return;
  }

  if (inventoryUI.isOpen) return;

  const digit = event.code.match(/^Digit([1-9])$/);
  if (digit && controller.isLocked) {
    inventory.selectSlot(Number(digit[1]) - 1);
  }
});

document.addEventListener('pointerlockchange', () => {
  if (document.pointerLockElement !== canvas && !inventoryUI.isOpen) {
    overlay.classList.remove('hidden');
  }
});

window.addEventListener('resize', onResize);

requestAnimationFrame(animate);
