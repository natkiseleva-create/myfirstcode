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

        // Mouse look
        // (handled externally via addYaw/addPitch)

        // Movement
        double speed = sprinting ? SPRINT_SPEED : WALK_SPEED;

        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);

        double moveX = 0, moveZ = 0;
        if (forward) { moveX -= sin; moveZ -= cos; }
        if (backward) { moveX += sin; moveZ += cos; }
        if (left) { moveX -= cos; moveZ += sin; }
        if (right) { moveX += cos; moveZ -= sin; }

        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (len > 0) {
            moveX /= len;
            moveZ /= len;
        }

        velocityX = moveX * speed;
        velocityZ = moveZ * speed;

        // Gravity
        if (!onGround) {
            velocityY -= GRAVITY * dt;
        }

        if (jumping && onGround) {
            velocityY = JUMP_VELOCITY;
            onGround = false;
        }

        // Move X
        double newX = x + velocityX * dt;
        if (!world.collides(newX, y, z) && !world.collides(newX, y + 0.1, z)) {
            x = newX;
        }

        // Move Z
        double newZ = z + velocityZ * dt;
        if (!world.collides(x, y, newZ) && !world.collides(x, y + 0.1, newZ)) {
            z = newZ;
        }

        // Move Y
        double newY = y + velocityY * dt;
        if (!world.collides(x, newY, z) && !world.collides(x, newY + 0.1, z)) {
            y = newY;
        } else {
            if (velocityY < 0) {
                // Hit ground
                double groundY = Math.floor(y) + 1;
                y = groundY;
                velocityY = 0;
                onGround = true;
            } else {
                velocityY = 0;
            }
        }

        // Check if standing on ground (feet level)
        double feetY = y - PLAYER_HEIGHT;
        double support = world.getSupportHeight(x, z, feetY);
        if (support > feetY - GROUND_SNAP) {
            y = support + PLAYER_HEIGHT;
            velocityY = 0;
            onGround = true;
        }

        // World updates
        world.update((int) x, (int) z);
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
