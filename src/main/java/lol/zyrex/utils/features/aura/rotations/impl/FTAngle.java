package lol.zyrex.utils.features.aura.rotations.impl;

import lol.zyrex.utils.client.chat.ChatMessage;
import lol.zyrex.utils.features.aura.point.Vector;
import lol.zyrex.utils.features.aura.rotations.constructor.RotateConstructor;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.utils.interactions.simulate.PlayerSimulation;
import lol.zyrex.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import lol.zyrex.utils.math.time.TimerUtil;
import lol.zyrex.Zyrex;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.utils.features.aura.striking.StrikeManager;
import lol.zyrex.utils.features.aura.warp.Turns;

import java.security.SecureRandom;

public class FTAngle extends RotateConstructor {
    private int swingCount = 0;
    private boolean hasSwungTwice = false;
    private boolean hasSwung = false;
    private boolean disableRotation = false;
    TimerUtil timer = new TimerUtil();

    public FTAngle() {
        super("FunTime");
    }

    @Override
    public Turns limitAngleChange(Turns currentTurns, Turns targetTurns, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Zyrex.getInstance().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        int count = attackHandler.getCount();

        Turns TurnsDelta = MathAngle.calculateDelta(currentTurns, targetTurns);
        float yawDelta = TurnsDelta.getYaw(), pitchDelta = TurnsDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotationDifference < 0.001F) rotationDifference = 0.001F;

        if (entity != null) {
            float speed = attackHandler.canAttack(Aura.getInstance().getConfig(), 0) ? randomLerp(0.7F, 1.0F) : new SecureRandom().nextBoolean() ? 0.4F : 0.2F;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            Turns moveTurns = new Turns(currentTurns.getYaw(), currentTurns.getPitch());
            moveTurns.setYaw(MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentTurns.getYaw(), currentTurns.getYaw() + moveYaw));
            moveTurns.setPitch(MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentTurns.getPitch(), currentTurns.getPitch() + movePitch));

            return moveTurns;
        } else {
            int suck = count % 3;
            float speed = attackTimer.finished(430) ? new SecureRandom().nextBoolean() ? 0.4F : 0.2F : -0.2F;
            float random = attackTimer.elapsedTime() / 40F + (count % 6);

            Turns randomTurns = switch (suck) {
                case 0 -> new Turns((float) Math.cos(random), (float) Math.sin(random));
                case 1 -> new Turns((float) Math.sin(random), (float) Math.cos(random));
                case 2 -> new Turns((float) Math.sin(random), (float) -Math.cos(random));
                default -> new Turns((float) -Math.cos(random), (float) Math.sin(random));
            };

            float yaw = !attackTimer.finished(2000) ? randomLerp(12, 24) * randomTurns.getYaw() : 0;
            float pitch2 = randomLerp(0, 2) * (float) Math.cos((double) System.currentTimeMillis() / 5000);
            float pitch = !attackTimer.finished(2000) ? randomLerp(2, 6) * randomTurns.getPitch() + pitch2 : 0;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            Turns moveTurns = new Turns(currentTurns.getYaw(), currentTurns.getPitch());
            moveTurns.setYaw(MathHelper.lerp(Math.clamp(randomLerp(speed, speed + 0.2F), 0, 1), currentTurns.getYaw(), currentTurns.getYaw() + moveYaw) + yaw);
            moveTurns.setPitch(MathHelper.lerp(Math.clamp(randomLerp(speed, speed + 0.2F), 0, 1), currentTurns.getPitch(), currentTurns.getPitch() + movePitch) + pitch);

            return moveTurns;
        }
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }
}