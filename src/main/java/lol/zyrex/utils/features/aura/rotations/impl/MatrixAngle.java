package lol.zyrex.utils.features.aura.rotations.impl;

import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.utils.features.aura.point.Vector;
import lol.zyrex.utils.features.aura.rotations.constructor.RotateConstructor;
import lol.zyrex.utils.features.aura.utils.RaycastAngle;
import lol.zyrex.utils.features.aura.warp.Turns;
import lol.zyrex.utils.features.aura.warp.TurnsConfig;
import lol.zyrex.utils.math.calc.Calculate;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.utils.features.aura.striking.StrikeManager;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.Zyrex;
import lol.zyrex.utils.math.time.StopWatch;

import java.security.SecureRandom;

public class MatrixAngle extends RotateConstructor {
    public MatrixAngle() {
        super("Matrix");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Zyrex.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();

        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotationDifference < 0.001F) rotationDifference = 0.001F;
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        float preAttackSpeed = 1;
        float postAttackSpeed = randomLerp(0, 0.5F);
        float speed = canAttack ? preAttackSpeed : postAttackSpeed;
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * (canAttack ? 360 : 100));
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
        float jitterYaw = canAttack ? 0 : (float) (randomLerp(0, 6) * Math.sin(System.currentTimeMillis() / randomLerp(15F, 145F)));
        float jitterPitch = canAttack ? 0 : (float) (randomLerp(1, 3) * Math.sin(System.currentTimeMillis() / randomLerp(15F, 145F)));

        if (!aura.isState() || entity == null) {
            speed = 0.7F;
            jitterYaw = 0;
            jitterPitch = 0;
        }

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);

        return moveAngle;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}