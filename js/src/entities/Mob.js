import * as THREE from 'three';
import { randomAt } from '../world/noise.js';

const MOB_TYPES = {
  pig: { color: 0xf0a0a8, body: [0.9, 0.6, 0.5], speed: 2.2 },
  cow: { color: 0x6b4a3a, body: [1.0, 0.9, 0.6], speed: 1.6 },
  sheep: { color: 0xe8e8e8, body: [0.85, 0.75, 0.85], speed: 1.9 },
};

let mobId = 0;

export class Mob {
  /**
   * @param {string} kind
   * @param {number} x
   * @param {number} z
   * @param {object} world
   */
  constructor(kind, x, z, world) {
    this.id = mobId++;
    this.kind = kind;
    this.world = world;
    this.speed = MOB_TYPES[kind]?.speed ?? 2;
    this.wanderTimer = 0;
    this.wanderDir = new THREE.Vector3();
    this.velocity = new THREE.Vector3();
    this.alive = true;

    const def = MOB_TYPES[kind] ?? MOB_TYPES.pig;
    const [bw, bh, bd] = def.body;

    this.group = new THREE.Group();
    const bodyMat = new THREE.MeshLambertMaterial({ color: def.color });
    const body = new THREE.Mesh(new THREE.BoxGeometry(bw, bh, bd), bodyMat);
    body.position.y = bh * 0.5;
    this.group.add(body);

    const head = new THREE.Mesh(
      new THREE.BoxGeometry(bw * 0.55, bh * 0.55, bd * 0.45),
      bodyMat
    );
    head.position.set(0, bh * 0.85, bd * 0.45);
    this.group.add(head);

    const feetY = world.getSupportHeight(x, z, 64, 0.25);
    this.group.position.set(x, feetY, z);
    this.pickNewDirection();
  }

  pickNewDirection() {
    const angle = randomAt(this.id * 7, this.group.position.x + this.group.position.z) * Math.PI * 2;
    this.wanderDir.set(Math.cos(angle), 0, Math.sin(angle));
    this.wanderTimer = 1.5 + randomAt(this.id * 3, this.group.position.z) * 3;
  }

  update(dt) {
    if (!this.alive) return;

    this.wanderTimer -= dt;
    if (this.wanderTimer <= 0) this.pickNewDirection();

    const pos = this.group.position;
    const nextX = pos.x + this.wanderDir.x * this.speed * dt;
    const nextZ = pos.z + this.wanderDir.z * this.speed * dt;

    const feetY = this.world.getSupportHeight(nextX, nextZ, pos.y, 0.25);
    const groundY = feetY;

    if (groundY > 2 && !this.world.isWaterAt(nextX, nextZ)) {
      pos.x = nextX;
      pos.z = nextZ;
    } else {
      this.pickNewDirection();
    }

    pos.y += (groundY - pos.y) * Math.min(1, dt * 12);

    const lookX = pos.x + this.wanderDir.x;
    const lookZ = pos.z + this.wanderDir.z;
    this.group.rotation.y = Math.atan2(lookX - pos.x, lookZ - pos.z);
  }

  dispose() {
    this.alive = false;
    this.group.traverse((child) => {
      if (child.geometry) child.geometry.dispose();
      if (child.material) child.material.dispose();
    });
    this.group.parent?.remove(this.group);
  }
}

export function randomMobKind(wx, wz) {
  const r = randomAt(wx, wz, 901);
  if (r < 0.34) return 'pig';
  if (r < 0.67) return 'cow';
  return 'sheep';
}
