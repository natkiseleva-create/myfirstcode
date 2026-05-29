package com.voxelcraft.render;

import com.voxelcraft.control.FirstPersonController;
import org.joml.Matrix4f;

public final class Camera {
    private Camera() {}

    public static Matrix4f projection(int width, int height) {
        float aspect = Math.max(1f, (float) width / Math.max(1, height));
        return new Matrix4f().perspective((float) Math.toRadians(75), aspect, 0.1f, 250f);
    }

    /** Matches Three.js camera with rotation order YXZ. */
    public static Matrix4f view(FirstPersonController controller) {
        double eyeX = controller.getEyeX();
        double eyeY = controller.getEyeY();
        double eyeZ = controller.getEyeZ();

        float cosPitch = (float) Math.cos(controller.pitch);
        float sinPitch = (float) Math.sin(controller.pitch);
        float sinYaw = (float) Math.sin(controller.yaw);
        float cosYaw = (float) Math.cos(controller.yaw);

        float dirX = -sinYaw * cosPitch;
        float dirY = sinPitch;
        float dirZ = -cosYaw * cosPitch;

        return new Matrix4f().setLookAt(
            (float) eyeX, (float) eyeY, (float) eyeZ,
            (float) eyeX + dirX, (float) eyeY + dirY, (float) eyeZ + dirZ,
            0f, 1f, 0f
        );
    }
}
