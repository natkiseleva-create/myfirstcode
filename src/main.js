import * as THREE from 'three';
import { FirstPersonController } from './controls/FirstPersonController.js';
import { createWorld } from './world/createWorld.js';
import { pickBlock } from './world/pickBlock.js';

const canvas = document.getElementById('game');
const overlay = document.getElementById('overlay');

const renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(window.innerWidth, window.innerHeight);

const camera = new THREE.PerspectiveCamera(
  75,
  window.innerWidth / window.innerHeight,
  0.1,
  200
);

const { scene, collides, getGroundHeight, getBlockMeshes, removeBlock } =
  createWorld();

const controller = new FirstPersonController(camera, canvas, {
  groundY: 0,
  collides,
});

const spawnX = 0;
const spawnZ = 0;
const spawnGround = getGroundHeight(spawnX, spawnZ);
controller.setPosition(spawnX, spawnGround, spawnZ);

const BREAK_REACH = 5;
let lastTime = performance.now();

function tryBreakBlock() {
  if (!controller.isLocked) return;

  const target = pickBlock(camera, getBlockMeshes(), BREAK_REACH);
  if (!target) return;

  removeBlock(target.x, target.y, target.z);
}

function animate(now) {
  requestAnimationFrame(animate);
  const dt = Math.min((now - lastTime) / 1000, 0.05);
  lastTime = now;

  if (controller.isLocked) {
    const feetX = camera.position.x;
    const feetZ = camera.position.z;
    const ground = getGroundHeight(feetX, feetZ);
    controller.groundY = ground;
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
  controller.requestPointerLock();
  overlay.classList.add('hidden');
});

canvas.addEventListener('mousedown', (event) => {
  if (event.button !== 0) return;
  tryBreakBlock();
});

canvas.addEventListener('contextmenu', (event) => {
  event.preventDefault();
});

document.addEventListener('pointerlockchange', () => {
  if (document.pointerLockElement !== canvas) {
    overlay.classList.remove('hidden');
  }
});

window.addEventListener('resize', onResize);

requestAnimationFrame(animate);
