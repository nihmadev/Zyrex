package lol.zyrex.utils.features.aura.neuro;

import lol.zyrex.events.player.AttackEvent;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.utils.features.aura.warp.Turns;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class NeuroRecorder implements QuickImports {

    private static final NeuroRecorder INSTANCE = new NeuroRecorder();

    private NeuroProfile recordingProfile = null;
    private boolean recording = false;

    // Previous tick state
    private float prevYaw = 0f;
    private float prevPitch = 0f;
    private long lastAttackTime = 0L;
    private boolean attackedThisTick = false;
    private boolean missedThisTick = false;
    private boolean critThisTick = false;
    private boolean wasOnGroundBeforeAttack = true;

    private NeuroRecorder() {}

    public static NeuroRecorder getInstance() {
        return INSTANCE;
    }

    public void startRecording(NeuroProfile profile) {
        this.recordingProfile = profile;
        this.recording = true;
        if (mc.player != null) {
            prevYaw = mc.player.getYaw();
            prevPitch = mc.player.getPitch();
            wasOnGroundBeforeAttack = mc.player.isOnGround();
        }
        lastAttackTime = System.currentTimeMillis();
        attackedThisTick = false;
        missedThisTick = false;
        critThisTick = false;
    }

    public NeuroProfile stopRecording() {
        recording = false;
        NeuroProfile finished = recordingProfile;
        if (finished != null) {
            finished.finalizeStats();
        }
        recordingProfile = null;
        return finished;
    }

    public boolean isRecording() {
        return recording;
    }

    public String getRecordingName() {
        return recordingProfile != null ? recordingProfile.getName() : null;
    }

    public void notifyAttack(boolean hit, boolean critical) {
        if (!recording) return;
        attackedThisTick = true;
        if (!hit) missedThisTick = true;
        if (critical) critThisTick = true;
        lastAttackTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!recording) return;
        // Detect attack from natural game flow
        attackedThisTick = true;
        critThisTick = !mc.player.isOnGround() && !wasOnGroundBeforeAttack;
        wasOnGroundBeforeAttack = mc.player.isOnGround();
        lastAttackTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!recording || mc.player == null || recordingProfile == null) return;

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDelta = currentYaw - prevYaw;
        float pitchDelta = currentPitch - prevPitch;

        while (yawDelta > 180f) yawDelta -= 360f;
        while (yawDelta < -180f) yawDelta += 360f;

        float yawDeltaAbs = Math.abs(yawDelta);
        float pitchDeltaAbs = Math.abs(pitchDelta);

        // Input state
        boolean forward = mc.player.input != null && mc.player.input.playerInput.forward();
        boolean backward = mc.player.input != null && mc.player.input.playerInput.backward();
        boolean left = mc.player.input != null && mc.player.input.playerInput.left();
        boolean right = mc.player.input != null && mc.player.input.playerInput.right();

        boolean jumping = !mc.player.isOnGround();
        boolean sprinting = mc.player.isSprinting();
        boolean sneaking = mc.player.isSneaking();
        boolean onGround = mc.player.isOnGround();

        // Target data
        LivingEntity target = Aura.getInstance().getTarget();
        float distance = target != null ? mc.player.distanceTo(target) : 0f;

        float targetYawDelta = 0f;
        float targetPitchDelta = 0f;
        float targetRelVelX = 0f;
        float targetRelVelZ = 0f;

        if (target != null) {
            Vec3d eyePos = mc.player.getEyePos();
            Vec3d targetCenter = target.getBoundingBox().getCenter();
            Turns angleToTarget = MathAngle.calculateAngle(targetCenter);
            targetYawDelta = MathAngle.computeAngleDifference(angleToTarget.getYaw(), currentYaw);
            targetPitchDelta = MathAngle.computeAngleDifference(angleToTarget.getPitch(), currentPitch);

            Vec3d relVel = target.getVelocity().subtract(mc.player.getVelocity());
            targetRelVelX = (float) relVel.x;
            targetRelVelZ = (float) relVel.z;
        }

        long delay = attackedThisTick ? (System.currentTimeMillis() - lastAttackTime) : 0L;

        float attackCooldown = mc.player.getAttackCooldownProgress(0);

        NeuroFrame frame = new NeuroFrame(
                yawDelta, pitchDelta, yawDeltaAbs, pitchDeltaAbs,
                forward, backward, left, right,
                jumping, sprinting, sneaking, onGround,
                attackedThisTick, missedThisTick, critThisTick, delay, attackCooldown,
                distance, targetYawDelta, targetPitchDelta,
                targetRelVelX, targetRelVelZ,
                currentYaw, currentPitch
        );

        recordingProfile.addFrame(frame);

        prevYaw = currentYaw;
        prevPitch = currentPitch;
        attackedThisTick = false;
        missedThisTick = false;
        critThisTick = false;
    }
}
