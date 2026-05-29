package com.voxelcraft.world;

import com.voxelcraft.block.Block;
import com.voxelcraft.control.FirstPersonController;

public final class BlockRaycaster {
    private BlockRaycaster() {}

    public static final double REACH = 5.0;

    public record Hit(int x, int y, int z, int normalX, int normalY, int normalZ, Block block) {
        public int placeX() { return x + normalX; }
        public int placeY() { return y + normalY; }
        public int placeZ() { return z + normalZ; }
    }

    public static Hit pick(FirstPersonController controller, World world) {
        double[] dir = direction(controller);
        double ox = controller.getEyeX();
        double oy = controller.getEyeY();
        double oz = controller.getEyeZ();

        int lastX = (int) Math.floor(ox);
        int lastY = (int) Math.floor(oy);
        int lastZ = (int) Math.floor(oz);

        double step = 0.04;
        for (double t = 0; t <= REACH; t += step) {
            int x = (int) Math.floor(ox + dir[0] * t);
            int y = (int) Math.floor(oy + dir[1] * t);
            int z = (int) Math.floor(oz + dir[2] * t);
            Block block = world.getBlock(x, y, z);
            if (block != null && TerrainGenerator.isSolidBlock(block.type.id)) {
                int nx = Integer.compare(lastX, x);
                int ny = Integer.compare(lastY, y);
                int nz = Integer.compare(lastZ, z);
                return new Hit(x, y, z, nx, ny, nz, block);
            }
            lastX = x;
            lastY = y;
            lastZ = z;
        }
        return null;
    }

    public static double[] direction(FirstPersonController controller) {
        double cosPitch = Math.cos(controller.pitch);
        return new double[] {
            -Math.sin(controller.yaw) * cosPitch,
            Math.sin(controller.pitch),
            -Math.cos(controller.yaw) * cosPitch
        };
    }
}
