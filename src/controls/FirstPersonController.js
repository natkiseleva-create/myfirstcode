import * as THREE from 'three';

const GRAVITY = 28;
const JUMP_VELOCITY = 9;
const WALK_SPEED = 5;
const SPRINT_SPEED = 9;
const MOUSE_SENSITIVITY = 0.002;
const PLAYER_HEIGHT = 1.7;
const PLAYER_RADIUS = 0.35;

export class FirstPersonController {
  /**
   * @param {THREE.PerspectiveCamera} camera
   * @param {HTMLElement} domElement
   * @param {object} options
   * @param {number} [options.groundY=0]
   * @param {(pos: THREE.Vector3) => boolean} [options.collides]
   */
  constructor(camera, domElement, options = {}) {
    this.camera = camera;
    this.domElement = domElement;
    this.groundY = options.groundY ?? 0;
    this.collides = options.collides ?? (() => false);

    this.velocity = new THREE.Vector3();
    this.direction = new THREE.Vector3();
    this.moveInput = { forward: 0, right: 0 };

    this.yaw = 0;
    this.pitch = 0;
    this.isLocked = false;
    this.onGround = false;

    this.keys = {
      forward: false,
      backward: false,
      left: false,
      right: false,
      jump: false,
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
        this.keys.jump = true;
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
        this.keys.jump = false;
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

  _applyHorizontalMovement(dt, speed) {
    if (this.moveInput.forward === 0 && this.moveInput.right === 0) {
      return;
    }

    const yawQuat = new THREE.Quaternion().setFromAxisAngle(
      new THREE.Vector3(0, 1, 0),
      this.yaw
    );
    this.direction.set(this.moveInput.right, 0, -this.moveInput.forward);
    this.direction.normalize().applyQuaternion(yawQuat).multiplyScalar(speed);

    const next = this.camera.position.clone().addScaledVector(this.direction, dt);
    if (!this._horizontalBlocked(next)) {
      this.camera.position.x = next.x;
      this.camera.position.z = next.z;
    } else {
      const tryX = this.camera.position.clone();
      tryX.x = next.x;
      if (!this._horizontalBlocked(tryX)) {
        this.camera.position.x = next.x;
      }
      const tryZ = this.camera.position.clone();
      tryZ.z = next.z;
      if (!this._horizontalBlocked(tryZ)) {
        this.camera.position.z = next.z;
      }
    }
  }

  _horizontalBlocked(position) {
    const feet = position.clone();
    feet.y = this.groundY;
    const head = feet.clone();
    head.y += PLAYER_HEIGHT;

    const samples = [
      feet,
      head,
      new THREE.Vector3(feet.x + PLAYER_RADIUS, feet.y + PLAYER_HEIGHT * 0.5, feet.z),
      new THREE.Vector3(feet.x - PLAYER_RADIUS, feet.y + PLAYER_HEIGHT * 0.5, feet.z),
      new THREE.Vector3(feet.x, feet.y + PLAYER_HEIGHT * 0.5, feet.z + PLAYER_RADIUS),
      new THREE.Vector3(feet.x, feet.y + PLAYER_HEIGHT * 0.5, feet.z - PLAYER_RADIUS),
    ];

    return samples.some((p) => this.collides(p));
  }

  _syncCameraRotation() {
    this.camera.rotation.order = 'YXZ';
    this.camera.rotation.y = this.yaw;
    this.camera.rotation.x = this.pitch;
  }

  update(dt) {
    this._updateMoveInput();
    this._syncCameraRotation();

    const speed = this.keys.sprint ? SPRINT_SPEED : WALK_SPEED;
    this._applyHorizontalMovement(dt, speed);

    if (this.keys.jump && this.onGround) {
      this.velocity.y = JUMP_VELOCITY;
      this.onGround = false;
    }

    this.velocity.y -= GRAVITY * dt;
    this.camera.position.y += this.velocity.y * dt;

    const floorY = this.groundY + PLAYER_HEIGHT;
    if (this.camera.position.y <= floorY) {
      this.camera.position.y = floorY;
      this.velocity.y = 0;
      this.onGround = true;
    } else {
      const feet = this.camera.position.clone();
      feet.y -= PLAYER_HEIGHT;
      if (this.collides(feet)) {
        this.camera.position.y = floorY;
        this.velocity.y = 0;
        this.onGround = true;
      } else {
        this.onGround = false;
      }
    }
  }

  setPosition(x, y, z) {
    this.camera.position.set(x, y + PLAYER_HEIGHT, z);
    this.velocity.set(0, 0, 0);
  }
}
