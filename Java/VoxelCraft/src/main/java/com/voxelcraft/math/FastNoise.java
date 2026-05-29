package com.voxelcraft.math;

public class FastNoise {
    private final int seed;

    public FastNoise(int seed) {
        this.seed = seed;
    }

    public static int hash(int x, int z, int seed) {
        int h = seed + x * 374761393 + z * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h = (h ^ (h >> 16));
        return h & 0x7fffffff;
    }

    public static double randomAt(int x, int z, int seed) {
        return (hash(x, z, seed) & 0x7fffffff) / (double) 0x7fffffff;
    }

    public static double randomAt(int x, int z) {
        return randomAt(x, z, 12345);
    }

    public static double smoothNoise(double x, double z, int seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = smoothstep(x - x0);
        double fz = smoothstep(z - z0);

        double v00 = randomAt(x0, z0, seed);
        double v10 = randomAt(x0 + 1, z0, seed);
        double v01 = randomAt(x0, z0 + 1, seed);
        double v11 = randomAt(x0 + 1, z0 + 1, seed);

        double ix0 = lerp(v00, v10, fx);
        double ix1 = lerp(v01, v11, fx);
        return lerp(ix0, ix1, fz);
    }

    public static double fbm(double x, double z, int octaves, int seed) {
        double value = 0;
        double amplitude = 1;
        double frequency = 1;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            value += smoothNoise(x * frequency, z * frequency, seed + i * 97) * amplitude;
            maxValue += amplitude;
            amplitude *= 0.5;
            frequency *= 2.05;
        }

        return value / maxValue;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }
}
