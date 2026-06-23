package lol.zyrex.utils.features.aura.neuro;

import lol.zyrex.Zyrex;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.utils.features.aura.rotations.constructor.RotateConstructor;
import lol.zyrex.utils.features.aura.striking.StrikeManager;
import lol.zyrex.utils.features.aura.warp.Turns;
import lol.zyrex.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;
import java.util.Random;

public class NeuroAngle extends RotateConstructor {

    private final Random rng = new SecureRandom();
    private final StopWatch attackTimer = new StopWatch();
    private float smoothFactor = 0.5f;
    private float rotAmplitude = 1f;
    private long lastAttackTime = 0L;

    // ── State machine for post-attack rotation behaviour ──────────────────
    private enum Phase { APPROACHING, ATTACKING, RECOVERING, TRACKING }
    private Phase phase = Phase.TRACKING;
    private long phaseStart = 0L;

    public NeuroAngle() {
        super("Neuro");
    }

    @Override
    public Turns limitAngleChange(Turns current, Turns target, Vec3d vec3d, Entity entity) {
        NeuroProfile profile = NeuroRepository.getInstance().getActiveProfile();
        if (profile == null || profile.getTotalFrames() < 10) {
            return lerpTurns(current, target, 0.5f);
        }

        // Compute delta needed to reach target
        float targetYawDelta = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float targetPitchDelta = MathHelper.wrapDegrees(target.getPitch() - current.getPitch());
        float totalRotation = (float) Math.hypot(Math.abs(targetYawDelta), Math.abs(targetPitchDelta));

        // Determine phase based on attack state
        StrikeManager strikeManager = Zyrex.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        boolean canAttack = entity != null && strikeManager.canAttack(aura.getConfig(), 0);
        updatePhase(canAttack);

        // Load player's behavioural traits
        float aggression = profile.getAggression();
        smoothFactor = profile.getSmoothness();
        rotAmplitude = profile.getAvgYawDeltaAbs() + profile.getAvgPitchDeltaAbs();

        // Generate rotation speed based on player profile
        float speed = generateSpeed(profile, canAttack, totalRotation);

        // Generate jitter that matches the player's natural hand movement
        float jitterYaw = generateJitter(profile, true);
        float jitterPitch = generateJitter(profile, false);

        // Apply smooth interpolation
        float moveYaw = targetYawDelta * speed;
        float movePitch = targetPitchDelta * speed;

        float resultYaw = current.getYaw() + moveYaw + jitterYaw;
        float resultPitch = MathHelper.clamp(current.getPitch() + movePitch + jitterPitch, -90f, 90f);

        return new Turns(resultYaw, resultPitch);
    }

    private void updatePhase(boolean canAttack) {
        long now = System.currentTimeMillis();

        if (canAttack && phase != Phase.ATTACKING) {
            phase = Phase.ATTACKING;
            phaseStart = now;
            lastAttackTime = now;
            attackTimer.reset();
        } else if (phase == Phase.ATTACKING && !canAttack) {
            phase = Phase.RECOVERING;
            phaseStart = now;
        } else if (phase == Phase.RECOVERING && now - phaseStart > 300) {
            phase = Phase.TRACKING;
            phaseStart = now;
        } else if (phase == Phase.TRACKING && canAttack) {
            phase = Phase.APPROACHING;
            phaseStart = now;
        }
    }

    private float generateSpeed(NeuroProfile profile, boolean canAttack, float totalRotation) {
        float aggression = profile.getAggression();
        float smoothness = profile.getSmoothness();

        // Base speed from how fast the player naturally moves their camera
        float avgRotSpeed = (profile.getAvgYawDeltaAbs() + profile.getAvgPitchDeltaAbs()) / 2f;
        float normSpeed = MathHelper.clamp(avgRotSpeed / 20f, 0.05f, 1f);

        // During attack: faster if aggressive, slower if smooth
        float speed;
        if (canAttack) {
            speed = 0.7f + aggression * 0.25f;
            speed = MathHelper.lerp(smoothness, speed * 0.6f, speed);
        } else {
            // Recovery phase: slower, more deliberate
            long timeSinceAttack = System.currentTimeMillis() - lastAttackTime;
            float recoveryFactor = MathHelper.clamp(timeSinceAttack / 500f, 0f, 1f);
            speed = (0.15f + normSpeed * 0.4f) + (0.3f * recoveryFactor);
        }

        // Scale by rotation distance - small adjustments are faster
        float rotScale = MathHelper.clamp(totalRotation / 30f, 0.3f, 1.5f);
        speed *= rotScale;

        // Add profile-based variability with the player's std dev
        float variability = (profile.getStdYawDeltaAbs() + profile.getStdPitchDeltaAbs()) / 40f;
        variability = MathHelper.clamp(variability, 0f, 0.3f);
        speed += (rng.nextFloat() - 0.5f) * 2f * variability;

        return MathHelper.clamp(speed, 0.03f, 0.98f);
    }

    private float generateJitter(NeuroProfile profile, boolean isYaw) {
        float std = isYaw ? profile.getStdYawDeltaAbs() : profile.getStdPitchDeltaAbs();
        float avg = isYaw ? profile.getAvgYawDeltaAbs() : profile.getAvgPitchDeltaAbs();
        float smoothness = profile.getSmoothness();

        // Jitter amplitude proportional to player's natural variation
        float jitterScale = std * 0.15f;

        // Smoother players get gentler jitter
        jitterScale *= (1f - smoothness * 0.5f);

        // Natural hand tremor simulation using multiple sine waves
        float t = System.currentTimeMillis() / 1000f;
        float tremor = (float) (
                Math.sin(t * 8.0f + (isYaw ? 0 : 1.5f)) * 0.4 +
                Math.sin(t * 13.7f + (isYaw ? 2.1f : 0.7f)) * 0.35 +
                Math.sin(t * 21.3f + (isYaw ? 1.2f : 3.8f)) * 0.25
        );

        // Phase modulation - jitter is higher during tracking, lower during attack
        float phaseMod = phase == Phase.ATTACKING ? 0.3f : phase == Phase.RECOVERING ? 0.7f : 1.0f;

        return (float) (tremor * jitterScale * phaseMod + (rng.nextGaussian() * jitterScale * 0.3 * phaseMod));
    }

    @Override
    public Vec3d randomValue() {
        NeuroProfile profile = NeuroRepository.getInstance().getActiveProfile();
        if (profile == null) return Vec3d.ZERO;

        float aggression = profile.getAggression();
        float smoothness = profile.getSmoothness();
        float avgDist = profile.getAvgDistance();

        // Spread proportional to distance and inversely proportional to aggression
        float spread = MathHelper.clamp(avgDist * 0.015f * (1.5f - aggression * 0.5f), 0f, 0.12f);

        // Smooth players aim more consistently at the same point
        float consistencyOffset = (1f - smoothness) * spread * 0.5f;

        return new Vec3d(
                (rng.nextFloat() - 0.5f) * spread + (rng.nextFloat() - 0.5f) * consistencyOffset,
                (rng.nextFloat() - 0.5f) * spread + (rng.nextFloat() - 0.5f) * consistencyOffset,
                0
        );
    }

    private Turns lerpTurns(Turns from, Turns to, float t) {
        float yaw = from.getYaw() + MathHelper.wrapDegrees(to.getYaw() - from.getYaw()) * t;
        float pitch = MathHelper.lerp(t, from.getPitch(), to.getPitch());
        return new Turns(yaw, MathHelper.clamp(pitch, -90f, 90f));
    }
}
