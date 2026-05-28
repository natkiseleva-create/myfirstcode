import * as THREE from 'three';

const GRAVITY = 220;
const JUMP_VELOCITY = 17;
const WALK_SPEED = 5;
const SPRINT_SPEED = 9;
const MOUSE_SENSITIVITY = 0.002;
export const PLAYER_HEIGHT = 1.7;
export const PLAYER_RADIUS = 0.35;
const STEP_HEIGHT = 1.05;
const SKIN = 0.02;
const GROUND_SNAP = 0.12;

export class FirstPersonController {
  /**
   * @param {THREE.PerspectiveCamera} camera
   * @param {HTMLElement} domElement
   * @param {object} options
   * @param {(pos: THREE.Vector3) => boolean} [options.collides]
   * @param {(x: number, z: number, feetY: number, radius?: number) => number} [options.getSupportHeight]
   */
  constructor(camera, domElement, options = {}) {
    this.camera = camera;
    this.domElement = domElement;
    this.collides = options.collides ?? (() => false);
    this.getSupportHeight =
      options.getSupportHeight ?? (() => 0);

    this.velocity = new THREE.Vector3();
    this.direction = new THREE.Vector3();
    this.moveInput = { forward: 0, right: 0 };

    this.yaw = 0;
    this.pitch = 0;
    this.isLocked = false;
    this.onGround = false;
    this.jumpRequested = false;

    this._sample = new THREE.Vector3();

    this.keys = {
      forward: false,
      backward: false,
      left: false,
      right: false,
      sprint: false,
    };

    this._onKeyDown = this._onKeyDown.bind(this);
    this._onKeyUp = this._onKeyUp.bind(this);
    this._onMouseMove = this._onMouseMove.bind(this);
    this._onPointerLockChange = this._onPointerLockChange.bind(this);

    document.addEventListener('keydown', this._onKeyDown);
    document.addEventListener('keyup', this._onKeyUp);
    document.addEventListener('mousemove', this._onMouseMove);
    document.addEventListener('pointerlockchange', this._onPointerLockChange);
  }

  getFeetY() {
    return this.camera.position.y - PLAYER_HEIGHT;
  }

  requestPointerLock() {
    this.domElement.requestPointerLock();
  }

  dispose() {
    document.removeEventListener('keydown', this._onKeyDown);
    document.removeEventListener('keyup', this._onKeyUp);
    document.removeEventListener('mousemove', this._onMouseMove);
    document.removeEventListener('pointerlockchange', this._onPointerLockChange);
  }

  _onPointerLockChange() {
    this.isLocked = document.pointerLockElement === this.domElement;
  }

  _onKeyDown(event) {
    if (!this.isLocked) return;
    switch (event.code) {
      case 'KeyW':
        this.keys.forward = true;
        break;
      case 'KeyS':
        this.keys.backward = true;
        break;
      case 'KeyA':
        this.keys.left = true;
        break;
      case 'KeyD':
        this.keys.right = true;
        break;
      case 'Space':
        if (!event.repeat) this.jumpRequested = true;
        event.preventDefault();
        break;
      case 'ShiftLeft':
      case 'ShiftRight':
        this.keys.sprint = true;
        break;
      default:
        break;
    }
  }

  _onKeyUp(event) {
    switch (event.code) {
      case 'KeyW':
        this.keys.forward = false;
        break;
      case 'KeyS':
        this.keys.backward = false;
        break;
      case 'KeyA':
        this.keys.left = false;
        break;
      case 'KeyD':
        this.keys.right = false;
        break;
      case 'Space':
        this.jumpRequested = false;
        break;
      case 'ShiftLeft':
      case 'ShiftRight':
        this.keys.sprint = false;
        break;
      default:
        break;
    }
  }

  _onMouseMove(event) {
    if (!this.isLocked) return;

    this.yaw -= event.movementX * MOUSE_SENSITIVITY;
    this.pitch -= event.movementY * MOUSE_SENSITIVITY;
    this.pitch = THREE.MathUtils.clamp(this.pitch, -Math.PI / 2 + 0.01, Math.PI / 2 - 0.01);
  }

  _updateMoveInput() {
    this.moveInput.forward =
      (this.keys.forward ? 1 : 0) - (this.keys.backward ? 1 : 0);
    this.moveInput.right =
      (this.keys.right ? 1 : 0) - (this.keys.left ? 1 : 0);
  }

  _bodyCollidesAt(x, z, feetY) {
    const samples = this._getBodySamples(x, z, feetY);
    return samples.some((p) => this.collides(p));
  }

  _getBodySamples(x, z, feetY) {
    const midY = feetY + PLAYER_HEIGHT * 0.5;
    return [
      new THREE.Vector3(x, feetY + SKIN, z),
      new THREE.Vector3(x, feetY + PLAYER_HEIGHT - SKIN, z),
      new THREE.Vector3(x + PLAYER_RADIUS, midY, z),
      new THREE.Vector3(x - PLAYER_RADIUS, midY, z),
      new THREE.Vector3(x, midY, z + PLAYER_RADIUS),
      new THREE.Vector3(x, midY, z - PLAYER_RADIUS),
    ];
  }

  _applyHorizontalMovement(dt, speed) {
    if (this.moveInput.forward === 0 && this.moveInput.right === 0) {
      return;
    }

    const yawQuat = new THREE.Quaternion().setFromAxisAngle(
      new THREE.Vector3(0, 1, 0),
      this.yaw
    );
    this.direction.set(this.moveInput.right, 0, -this.moveInput.forward);
    this.direction.normalize().applyQuaternion(yawQuat).multiplyScalar(speed * dt);

    const feetY = this.getFeetY();
    const nextX = this.camera.position.x + this.direction.x;
    const nextZ = this.camera.position.z + this.direction.z;

    if (!this._bodyCollidesAt(nextX, nextZ, feetY)) {
      this.camera.position.x = nextX;
      this.camera.position.z = nextZ;
      return;
    }

    if (!this._bodyCollidesAt(nextX, this.camera.position.z, feetY)) {
      this.camera.position.x = nextX;
    } else if (!this._bodyCollidesAt(this.camera.position.x, nextZ, feetY)) {
      this.camera.position.z = nextZ;
    }

    if (this._bodyCollidesAt(this.camera.position.x, this.camera.position.z, feetY)) {
      this._tryStepUp(nextX, nextZ, feetY);
    }
  }

  _tryStepUp(targetX, targetZ, feetY) {
    const steppedFeetY = feetY + STEP_HEIGHT;
    if (this._bodyCollidesAt(targetX, targetZ, steppedFeetY)) return;

    const support = this.getSupportHeight(
      targetX,
      targetZ,
      steppedFeetY,
      PLAYER_RADIUS
    );
    if (Math.abs(support - steppedFeetY) > 0.2) return;

    this.camera.position.x = targetX;
    this.camera.position.z = targetZ;
    this.camera.position.y = steppedFeetY + PLAYER_HEIGHT;
    this.velocity.y = Math.max(0, this.velocity.y);
    this.onGround = true;
  }

  _syncCameraRotation() {
    this.camera.rotation.order = 'YXZ';
    this.camera.rotation.y = this.yaw;
    this.camera.rotation.x = this.pitch;
  }

  _resolveCeiling() {
    const headY = this.camera.position.y - SKIN;
    const head = this._sample.set(
      this.camera.position.x,
      headY,
      this.camera.position.z
    );
    if (this.collides(head) && this.velocity.y > 0) {
      const cellTop = Math.floor(headY) + 1;
      this.camera.position.y = cellTop - SKIN;
      this.velocity.y = 0;
    }
  }

  _resolveGround() {
    const px = this.camera.position.x;
    const pz = this.camera.position.z;
    const feetY = this.getFeetY();
    const groundY = this.getSupportHeight(px, pz, feetY, PLAYER_RADIUS);
    const aboveGround = feetY - groundY;

    if (this.velocity.y <= 0) {
      if (aboveGround < -0.02) {
        this.camera.position.y = groundY + PLAYER_HEIGHT;
        this.velocity.y = 0;
        this.onGround = true;
        return;
      }

      if (aboveGround >= -0.02 && aboveGround <= GROUND_SNAP) {
        this.camera.position.y = groundY + PLAYER_HEIGHT;
        this.velocity.y = 0;
        this.onGround = true;
        return;
      }
    }

    if (this.velocity.y <= 0 && this._bodyCollidesAt(px, pz, feetY)) {
      this.camera.position.y = groundY + PLAYER_HEIGHT;
      this.velocity.y = 0;
      this.onGround = true;
      return;
    }

    this.onGround = false;
  }

  _tryJump() {
    if (!this.jumpRequested || !this.onGround || this.velocity.y > 0.05) {
      return;
    }

    this.velocity.y = JUMP_VELOCITY;
    this.onGround = false;
    this.jumpRequested = false;
  }

  update(dt) {
    this._updateMoveInput();
    this._syncCameraRotation();

    const speed = this.keys.sprint ? SPRINT_SPEED : WALK_SPEED;
    this._applyHorizontalMovement(dt, speed);

    this._tryJump();

    this.velocity.y -= GRAVITY * dt;
    this.camera.position.y += this.velocity.y * dt;

    this._resolveCeiling();
    this._resolveGround();
  }

  setPosition(x, y, z) {
    this.camera.position.set(x, y + PLAYER_HEIGHT, z);
    this.velocity.set(0, 0, 0);
    this.onGround = true;
    this.jumpRequested = false;
  }
}
