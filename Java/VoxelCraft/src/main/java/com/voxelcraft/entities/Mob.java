package com.voxelcraft.entities;

import com.voxelcraft.math.FastNoise;
import com.voxelcraft.world.World;

public class Mob {
    public enum Kind {
        PIG(0xf0a0a8, 0.9, 0.6, 0.5, 2.2),
        COW(0x6b4a3a, 1.0, 0.9, 0.6, 1.6),
        SHEEP(0xe8e8e8, 0.85, 0.75, 0.85, 1.9);

        public final int color;
        public final double width;
        public final double height;
        public final double depth;
        public final double speed;

        Kind(int color, double width, double height, double depth, double speed) {
            this.color = color;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.speed = speed;
        }
    }

    private static int nextId = 1;

    public final int id = nextId++;
    public final Kind kind;
    public double x;
    public double y;
    public double z;
    public double yaw;
    private double wanderX;
    private double wanderZ;
    private double wanderTimer;

    public Mob(Kind kind, double x, double z, World world) {
        this.kind = kind;
        this.x = x;
        this.z = z;
        this.y = world.getSupportHeight(x, z, 64);
        pickNewDirection();
    }

    public void update(double dt, World world) {
        wanderTimer -= dt;
        if (wanderTimer <= 0) {
            pickNewDirection();
        }

        double nextX = x + wanderX * kind.speed * dt;
        double nextZ = z + wanderZ * kind.speed * dt;
        double groundY = world.getSupportHeight(nextX, nextZ, y);

        if (groundY > 2 && !world.isWaterAt(nextX, nextZ)) {
            x = nextX;
            z = nextZ;
        } else {
            pickNewDirection();
        }

        y += (groundY - y) * Math.min(1.0, dt * 12.0);
        yaw = Math.atan2(wanderX, wanderZ);
    }

    private void pickNewDirection() {
        double angle = FastNoise.randomAt(id * 7, (int) Math.floor(x + z), 0) * Math.PI * 2.0;
        wanderX = Math.cos(angle);
        wanderZ = Math.sin(angle);
        wanderTimer = 1.5 + FastNoise.randomAt(id * 3, (int) Math.floor(z), 0) * 3.0;
    }

    public static Kind randomKind(int wx, int wz) {
        double r = FastNoise.randomAt(wx, wz, 901);
        if (r < 0.34) return Kind.PIG;
        if (r < 0.67) return Kind.COW;
        return Kind.SHEEP;
    }
}
