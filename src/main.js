import * as THREE from 'three';
import {
  FirstPersonController,
  PLAYER_HEIGHT,
  PLAYER_RADIUS,
} from './controls/FirstPersonController.js';
import { createWorld } from './world/createWorld.js';
import { pickBlock, pickBlockFace } from './world/pickBlock.js';
import { Inventory } from './inventory/Inventory.js';
import { InventoryUI } from './ui/InventoryUI.js';
import { GAME_MODES } from './modes/gameModes.js';

const canvas = document.getElementById('game');
const modeMenu = document.getElementById('mode-menu');
const overlay = document.getElementById('overlay');
const winMessage = document.getElementById('win-message');
const modeHint = document.getElementById('mode-hint');

const REACH = 5;

const renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(window.innerWidth, window.innerHeight);

const camera = new THREE.PerspectiveCamera(
  75,
  window.innerWidth / window.innerHeight,
  0.1,
  250
);

let world = null;
let controller = null;
let inventory = null;
let inventoryUI = null;
let currentMode = null;
let lastTime = performance.now();

function playerOccupiesBlock(x, y, z) {
  const minX = camera.position.x - PLAYER_RADIUS;
  const maxX = camera.position.x + PLAYER_RADIUS;
  const minY = camera.position.y - PLAYER_HEIGHT;
  const maxY = camera.position.y;
  const minZ = camera.position.z - PLAYER_RADIUS;
  const maxZ = camera.position.z + PLAYER_RADIUS;

  return (
    maxX > x &&
    minX < x + 1 &&
    maxY > y &&
    minY < y + 1 &&
    maxZ > z &&
    minZ < z + 1
  );
}

function tryBreakBlock() {
  if (!controller?.isLocked || inventoryUI.isOpen) return;

  const target = pickBlock(camera, world.getPickables(), REACH);
  if (!target) return;

  const type = world.removeBlock(target.x, target.y, target.z);
  if (type) {
    inventory.addItem(type, 1);
  }
}

function tryPlaceBlock() {
  if (!controller?.isLocked || inventoryUI.isOpen) return;
  if (!inventory.hasSelected()) return;

  const hit = pickBlockFace(camera, world.getPickables(), REACH);
  if (!hit) return;

  const { place } = hit;
  const selected = inventory.getSelectedItem();
  if (!selected.type) return;

  if (playerOccupiesBlock(place.x, place.y, place.z)) return;
  if (world.collides(new THREE.Vector3(place.x + 0.5, place.y + 0.5, place.z + 0.5))) {
    return;
  }

  if (world.placeBlock(place.x, place.y, place.z, selected.type)) {
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

function showModeMenu() {
  document.exitPointerLock();
  if (world) {
    world.dispose();
    world = null;
  }

  currentMode = null;
  controller = null;
  inventory = null;
  inventoryUI = null;

  modeMenu.classList.remove('hidden');
  overlay.classList.add('hidden');
  winMessage.classList.add('hidden');
  document.getElementById('hotbar').classList.add('hidden');
  document.getElementById('inventory-panel').classList.add('hidden');
}

function startMode(mode) {
  currentMode = mode;
  modeMenu.classList.add('hidden');
  document.getElementById('hotbar').classList.remove('hidden');

  world = createWorld(mode);
  inventory = new Inventory();
  inventoryUI = new InventoryUI(inventory);

  controller = new FirstPersonController(camera, canvas, {
    collides: world.collides,
    getSupportHeight: world.getSupportHeight,
  });

  world.ensureWorldLoaded(world.spawn.x, world.spawn.z);
  const ground = world.getSupportHeight(
    world.spawn.x,
    world.spawn.z,
    10,
    PLAYER_RADIUS
  );
  controller.setPosition(world.spawn.x, ground, world.spawn.z);

  if (mode === GAME_MODES.MAZE) {
    modeHint.textContent =
      'Найдите золотой столб — это выход из лабиринта. Esc — в меню режимов.';
  } else {
    modeHint.textContent =
      'Бесконечный мир: исследуйте, добывайте ресурсы, стройте. Esc — в меню режимов.';
  }

  overlay.classList.remove('hidden');
  winMessage.classList.add('hidden');
}

function checkMazeWin() {
  if (currentMode !== GAME_MODES.MAZE || !world) return;
  if (world.checkWin(camera.position.x, camera.position.z)) {
    document.exitPointerLock();
    winMessage.classList.remove('hidden');
    overlay.classList.add('hidden');
  }
}

function animate(now) {
  requestAnimationFrame(animate);
  const dt = Math.min((now - lastTime) / 1000, 0.05);
  lastTime = now;

  if (!world) return;

  world.updateWorld(camera.position.x, camera.position.z);

  if (controller.isLocked && !inventoryUI.isOpen) {
    controller.update(dt);
    checkMazeWin();
  }

  renderer.render(world.scene, camera);
}

function onResize() {
  const w = window.innerWidth;
  const h = window.innerHeight;
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
  renderer.setSize(w, h);
}

modeMenu.querySelectorAll('[data-mode]').forEach((btn) => {
  btn.addEventListener('click', () => {
    startMode(btn.dataset.mode);
  });
});

overlay.addEventListener('click', () => {
  if (!controller || inventoryUI.isOpen) return;
  controller.requestPointerLock();
  overlay.classList.add('hidden');
});

document.getElementById('btn-back-menu')?.addEventListener('click', showModeMenu);

winMessage.querySelector('[data-action="menu"]')?.addEventListener('click', showModeMenu);
winMessage.querySelector('[data-action="retry"]')?.addEventListener('click', () => {
  if (currentMode) startMode(currentMode);
});

canvas.addEventListener('mousedown', (event) => {
  if (!controller?.isLocked || inventoryUI.isOpen) return;
  if (event.button === 0) tryBreakBlock();
  else if (event.button === 2) tryPlaceBlock();
});

canvas.addEventListener('wheel', (event) => {
  if (!controller?.isLocked || inventoryUI.isOpen) return;
  event.preventDefault();
  inventory.cycleSelection(event.deltaY > 0 ? 1 : -1);
});

canvas.addEventListener('contextmenu', (event) => event.preventDefault());

document.addEventListener('keydown', (event) => {
  if (event.code === 'Escape' && currentMode) {
    showModeMenu();
    return;
  }

  if (event.code === 'KeyE' && world) {
    toggleInventory();
    return;
  }

  if (!inventoryUI || inventoryUI.isOpen) return;

  const digit = event.code.match(/^Digit([1-9])$/);
  if (digit && controller?.isLocked) {
    inventory.selectSlot(Number(digit[1]) - 1);
  }
});

document.addEventListener('pointerlockchange', () => {
  if (!world || inventoryUI?.isOpen) return;
  if (document.pointerLockElement !== canvas && !winMessage.classList.contains('hidden')) {
    return;
  }
  if (document.pointerLockElement !== canvas) {
    overlay.classList.remove('hidden');
  }
});

window.addEventListener('resize', onResize);

requestAnimationFrame(animate);
