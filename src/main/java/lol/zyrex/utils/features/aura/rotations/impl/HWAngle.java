package lol.zyrex.utils.features.aura.rotations.impl;

import lol.zyrex.utils.features.aura.point.Vector;
import lol.zyrex.utils.features.aura.rotations.constructor.RotateConstructor;
import lol.zyrex.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import lol.zyrex.Zyrex;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.utils.features.aura.striking.StrikeManager;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.utils.math.calc.Calculate;

import java.security.SecureRandom;

public class HWAngle extends RotateConstructor {
    private static long tickCounter = 0;
    private float resetProgress = 0.0f;
    private final SecureRandom secureRandom = new SecureRandom();
    private boolean jitterApplied = false;

    public HWAngle() {
        super("HolyWorld");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Aura aura = Aura.getInstance();
        StrikeManager attackHandler = Zyrex.getInstance().getAttackPerpetrator().getAttackHandler();

        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);
        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotationDifference < 0.001F) rotationDifference = 0.001F;

        float speed = canAttack ? randomLerp(0.86F, 0.96F) : randomLerp(0.1F, 0.4F);

        float maxRotation = 180F;
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * maxRotation);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * maxRotation);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(Calculate.getRandom(speed, speed + 0.2F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
        moveAngle.setPitch(MathHelper.lerp(Calculate.getRandom(speed, speed + 0.2F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch));

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
        return new Vec3d(0.1, 0.1, 0.1);
    }
}