package lol.zyrex.utils.features.skeleton;

import net.minecraft.util.math.Vec3d;

public class BoneData {
    public final Vec3d position;
    public final float pitch;
    public final float yaw;
    public final float roll;

    public BoneData(Vec3d position, float pitch, float yaw, float roll) {
        this.position = position;
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }
}
