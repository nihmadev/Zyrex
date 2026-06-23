package lol.zyrex.utils.features.aura.neuro;

import java.io.Serializable;

public class NeuroFrame implements Serializable {
    private static final long serialVersionUID = 2L;

    // ── Rotation deltas ───────────────────────────────────────────────────
    public float yawDelta;
    public float pitchDelta;
    public float yawDeltaAbs;
    public float pitchDeltaAbs;

    // ── Movement state ────────────────────────────────────────────────────
    public boolean movingForward;
    public boolean movingBackward;
    public boolean movingLeft;
    public boolean movingRight;
    public boolean jumping;
    public boolean sprinting;
    public boolean sneaking;
    public boolean onGround;

    // ── Combat state ──────────────────────────────────────────────────────
    public boolean attacked;
    public boolean missed;
    public boolean critical;
    public long attackDelayMs;
    public float attackCooldown;

    // ── Target ────────────────────────────────────────────────────────────
    public float distanceToTarget;
    public float targetYawDelta;
    public float targetPitchDelta;
    public float targetRelVelX;
    public float targetRelVelZ;

    // ── Absolute camera ───────────────────────────────────────────────────
    public float cameraYaw;
    public float cameraPitch;

    public NeuroFrame() {}

    public NeuroFrame(float yawDelta, float pitchDelta, float yawDeltaAbs, float pitchDeltaAbs,
                      boolean movingForward, boolean movingBackward, boolean movingLeft, boolean movingRight,
                      boolean jumping, boolean sprinting, boolean sneaking, boolean onGround,
                      boolean attacked, boolean missed, boolean critical, long attackDelayMs, float attackCooldown,
                      float distanceToTarget, float targetYawDelta, float targetPitchDelta,
                      float targetRelVelX, float targetRelVelZ,
                      float cameraYaw, float cameraPitch) {
        this.yawDelta = yawDelta;
        this.pitchDelta = pitchDelta;
        this.yawDeltaAbs = yawDeltaAbs;
        this.pitchDeltaAbs = pitchDeltaAbs;
        this.movingForward = movingForward;
        this.movingBackward = movingBackward;
        this.movingLeft = movingLeft;
        this.movingRight = movingRight;
        this.jumping = jumping;
        this.sprinting = sprinting;
        this.sneaking = sneaking;
        this.onGround = onGround;
        this.attacked = attacked;
        this.missed = missed;
        this.critical = critical;
        this.attackDelayMs = attackDelayMs;
        this.attackCooldown = attackCooldown;
        this.distanceToTarget = distanceToTarget;
        this.targetYawDelta = targetYawDelta;
        this.targetPitchDelta = targetPitchDelta;
        this.targetRelVelX = targetRelVelX;
        this.targetRelVelZ = targetRelVelZ;
        this.cameraYaw = cameraYaw;
        this.cameraPitch = cameraPitch;
    }
}
