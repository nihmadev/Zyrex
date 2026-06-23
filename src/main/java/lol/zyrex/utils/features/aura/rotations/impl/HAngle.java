package lol.zyrex.utils.features.aura.rotations.impl;

import lol.zyrex.Zyrex;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.utils.features.aura.point.Vector;
import lol.zyrex.utils.features.aura.rotations.constructor.RotateConstructor;
import lol.zyrex.utils.features.aura.striking.StrikeManager;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.utils.features.aura.warp.Turns;
import lol.zyrex.utils.math.calc.Calculate;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class HAngle extends RotateConstructor {
    private static long tickCounter = 0;
    private float resetProgress = 0.0f;
    private final SecureRandom secureRandom = new SecureRandom();
    private boolean jitterApplied = false;

    public HAngle() {
        super("HvH");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Aura aura = Aura.getInstance();
        StrikeManager attackHandler = Zyrex.getInstance().getAttackPerpetrator().getAttackHandler();
        if (entity !=null) {
            Vec3d aimPoint = Vector.hitbox(entity, 1,1.3F, 1, 6F);
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);
        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotationDifference < 0.001F) rotationDifference = 0.001F;

        float speed = 1;
        float jitterYaw = canAttack ? 0 : (float) (5 * Math.sin(System.currentTimeMillis() / 45D));
        float jitterPitch = canAttack ? 0 : (float) (5 * Math.sin(System.currentTimeMillis() / 45D));
        float maxRotation = 360F;
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * maxRotation);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * maxRotation);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(Calculate.getRandom(speed, speed + 0.2F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        moveAngle.setPitch(MathHelper.lerp(Calculate.getRandom(speed, speed + 0.2F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);

        return new Turns(moveAngle.getYaw(), moveAngle.getPitch());
    }

    private float applyGaussianJitter(float rotation, float strength) {
        return rotation + (float) (secureRandom.nextGaussian() * strength);
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}