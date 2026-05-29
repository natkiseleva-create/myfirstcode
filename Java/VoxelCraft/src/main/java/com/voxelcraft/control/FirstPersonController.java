package com.voxelcraft.control;

import com.voxelcraft.world.World;

public class FirstPersonController {
    public static final double PLAYER_HEIGHT = 1.7;
    public static final double PLAYER_RADIUS = 0.35;

    private static final double GRAVITY = 42;
    private static final double JUMP_VELOCITY = 11.8;
    private static final double WALK_SPEED = 5;
    private static final double SPRINT_SPEED = 9;
    private static final double MOUSE_SENSITIVITY = 0.002;
    private static final double STEP_HEIGHT = 1.05;
    private static final double GROUND_SNAP = 0.12;
    private static final double SKIN = 0.02;

    private final World world;

    public double x, y, z;
    public double velocityX, velocityY, velocityZ;
    public double yaw, pitch;
    public boolean onGround;
    public boolean isLocked;

    public boolean forward, backward, left, right, jumping, sprinting;

    public FirstPersonController(World world, double spawnX, double spawnY, double spawnZ) {
        this.world = world;
        this.x = spawnX;
        this.y = spawnY + PLAYER_HEIGHT;
        this.z = spawnZ;
    }

    public void update(double dt) {
        if (!isLocked) return;

        applyHorizontalMovement(dt);

        if (jumping && onGround && velocityY <= 0.05) {
            velocityY = JUMP_VELOCITY;
            onGround = false;
        }

        velocityY -= GRAVITY * dt;
        y += velocityY * dt;

        resolveCeiling();
        resolveGround();

        world.update((int) x, (int) z);
    }

    private void applyHorizontalMovement(double dt) {
        double speed = sprinting ? SPRINT_SPEED : WALK_SPEED;

        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);

        double moveX = 0;
        double moveZ = 0;
        if (forward) { moveX -= sin; moveZ -= cos; }
        if (backward) { moveX += sin; moveZ += cos; }
        if (left) { moveX -= cos; moveZ += sin; }
        if (right) { moveX += cos; moveZ -= sin; }

        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (len <= 0) return;

        moveX = moveX / len * speed * dt;
        moveZ = moveZ / len * speed * dt;

        double feetY = getFeetY();
        double nextX = x + moveX;
        double nextZ = z + moveZ;

        if (!bodyCollidesAt(nextX, nextZ, feetY)) {
            x = nextX;
            z = nextZ;
            return;
        }

        if (!bodyCollidesAt(nextX, z, feetY)) {
            x = nextX;
        } else if (!bodyCollidesAt(x, nextZ, feetY)) {
            z = nextZ;
        }

        if (bodyCollidesAt(x, z, feetY)) {
            tryStepUp(nextX, nextZ, feetY);
        }
    }

    private void tryStepUp(double targetX, double targetZ, double feetY) {
        double steppedFeetY = feetY + STEP_HEIGHT;
        if (bodyCollidesAt(targetX, targetZ, steppedFeetY)) return;

        double support = world.getSupportHeight(targetX, targetZ, steppedFeetY);
        if (Math.abs(support - steppedFeetY) > 0.2) return;

        x = targetX;
        z = targetZ;
        y = support + PLAYER_HEIGHT;
        velocityY = Math.max(0, velocityY);
        onGround = true;
    }

    private void resolveCeiling() {
        double headY = y - SKIN;
        if (world.collides(x, headY, z) && velocityY > 0) {
            y = Math.floor(headY) + 1 - SKIN;
            velocityY = 0;
        }
    }

    private void resolveGround() {
        double feetY = getFeetY();
        double groundY = world.getSupportHeight(x, z, feetY);
        double aboveGround = feetY - groundY;

        if (velocityY <= 0) {
            if (aboveGround < -0.02) {
                y = groundY + PLAYER_HEIGHT;
                velocityY = 0;
                onGround = true;
                return;
            }

            if (aboveGround >= -0.02 && aboveGround <= GROUND_SNAP) {
                y = groundY + PLAYER_HEIGHT;
                velocityY = 0;
                onGround = true;
                return;
            }
        }

        if (velocityY <= 0 && bodyCollidesAt(x, z, feetY)) {
            double centerGround = world.getSupportHeight(x, z, feetY);
            y = centerGround + PLAYER_HEIGHT;
            velocityY = 0;
            onGround = true;
            return;
        }

        onGround = false;
    }

    private boolean bodyCollidesAt(double px, double pz, double feetY) {
        double midY = feetY + PLAYER_HEIGHT * 0.5;
        return collides(px, feetY + SKIN, pz)
            || collides(px, feetY + PLAYER_HEIGHT - SKIN, pz)
            || collides(px + PLAYER_RADIUS, midY, pz)
            || collides(px - PLAYER_RADIUS, midY, pz)
            || collides(px, midY, pz + PLAYER_RADIUS)
            || collides(px, midY, pz - PLAYER_RADIUS);
    }

    private boolean collides(double px, double py, double pz) {
        return world.collides(px, py, pz);
    }

    private double getFeetY() {
        return y - PLAYER_HEIGHT;
    }

    public void addYaw(double delta) {
        yaw += delta * MOUSE_SENSITIVITY;
    }

    public void addPitch(double delta) {
        pitch += delta * MOUSE_SENSITIVITY;
        pitch = Math.max(-Math.PI / 2 + 0.01, Math.min(Math.PI / 2 - 0.01, pitch));
    }

    public double getEyeX() { return x; }
    public double getEyeY() { return y; }
    public double getEyeZ() { return z; }
}
