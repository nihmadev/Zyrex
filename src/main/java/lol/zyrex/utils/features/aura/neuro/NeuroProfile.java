package lol.zyrex.utils.features.aura.neuro;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NeuroProfile implements Serializable {
    private static final long serialVersionUID = 2L;

    // ── Metadata ──────────────────────────────────────────────────────────
    private final String name;
    private final long createdAt;
    private final List<NeuroFrame> frames;
    private int totalFrames;

    // ═════════════════════════════════════════════════════════════════════
    //                   Rotation statistics
    // ═════════════════════════════════════════════════════════════════════
    private float avgYawDelta;
    private float avgPitchDelta;
    private float stdYawDelta;
    private float stdPitchDelta;
    private float avgYawDeltaAbs;
    private float avgPitchDeltaAbs;
    private float stdYawDeltaAbs;
    private float stdPitchDeltaAbs;

    // ═════════════════════════════════════════════════════════════════════
    //                   Movement profile
    // ═════════════════════════════════════════════════════════════════════
    private float forwardRatio;
    private float backwardRatio;
    private float leftRatio;
    private float rightRatio;
    private float jumpRatio;
    private float sprintRatio;
    private float sneakRatio;
    private float groundRatio;
    private float preferredStrafe; // >0 = right bias, <0 = left bias

    // Movement transition matrix 4x4: F/B/L/R -> F/B/L/R
    private float[][] movementTransitionMatrix;

    // ═════════════════════════════════════════════════════════════════════
    //                   Attack profile
    // ═════════════════════════════════════════════════════════════════════
    private float avgAttackDelay;
    private float stdAttackDelay;
    private float critRatio;
    private float missRate;
    private int totalAttacks;
    private int totalCrits;
    private int totalMisses;

    // Attack delay histogram: 10 bins 0..1000ms
    private float[] attackDelayHistogram;

    // ═════════════════════════════════════════════════════════════════════
    //                   Distance profile
    // ═════════════════════════════════════════════════════════════════════
    private float avgDistance;
    private float stdDistance;
    private float minDistance;
    private float maxDistance;
    private float[] distanceHistogram; // 12 bins 0..6

    // ═════════════════════════════════════════════════════════════════════
    //                   Target tracking profile
    // ═════════════════════════════════════════════════════════════════════
    private float avgTargetYawDelta;
    private float avgTargetPitchDelta;

    // ═════════════════════════════════════════════════════════════════════
    //                   Behavioural traits
    // ═════════════════════════════════════════════════════════════════════
    private float aggression;      // how aggressively the player aims (0..1)
    private float smoothness;      // how smooth the mouse movement is (0..1)
    private float cameraActivity;  // how much the camera moves overall

    private static final int DELAY_BINS = 10;
    private static final int DIST_BINS = 12;
    private static final float MAX_DELAY_MS = 1000f;
    private static final float MAX_DISTANCE = 6f;

    public NeuroProfile(String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.frames = new ArrayList<>();
        this.movementTransitionMatrix = new float[4][4];
        this.attackDelayHistogram = new float[DELAY_BINS];
        this.distanceHistogram = new float[DIST_BINS];
    }

    public void addFrame(NeuroFrame frame) {
        frames.add(frame);
    }

    public void finalizeStats() {
        if (frames.isEmpty()) return;
        totalFrames = frames.size();

        computeRotationStats();
        computeMovementProfile();
        computeAttackProfile();
        computeDistanceProfile();
        computeTargetTrackingProfile();
        computeBehaviouralTraits();
    }

    // ── Rotation ──────────────────────────────────────────────────────────

    private void computeRotationStats() {
        double sumYaw = 0, sumPitch = 0, sumYaw2 = 0, sumPitch2 = 0;
        double sumYawAbs = 0, sumPitchAbs = 0, sumYawAbs2 = 0, sumPitchAbs2 = 0;

        for (NeuroFrame f : frames) {
            sumYaw += f.yawDelta;
            sumPitch += f.pitchDelta;
            sumYaw2 += (double) f.yawDelta * f.yawDelta;
            sumPitch2 += (double) f.pitchDelta * f.pitchDelta;
            sumYawAbs += f.yawDeltaAbs;
            sumPitchAbs += f.pitchDeltaAbs;
            sumYawAbs2 += (double) f.yawDeltaAbs * f.yawDeltaAbs;
            sumPitchAbs2 += (double) f.pitchDeltaAbs * f.pitchDeltaAbs;
        }

        float n = totalFrames;
        avgYawDelta = (float) (sumYaw / n);
        avgPitchDelta = (float) (sumPitch / n);
        avgYawDeltaAbs = (float) (sumYawAbs / n);
        avgPitchDeltaAbs = (float) (sumPitchAbs / n);

        stdYawDelta = (float) Math.sqrt(Math.max(0, sumYaw2 / n - (double) avgYawDelta * avgYawDelta));
        stdPitchDelta = (float) Math.sqrt(Math.max(0, sumPitch2 / n - (double) avgPitchDelta * avgPitchDelta));
        stdYawDeltaAbs = (float) Math.sqrt(Math.max(0, sumYawAbs2 / n - (double) avgYawDeltaAbs * avgYawDeltaAbs));
        stdPitchDeltaAbs = (float) Math.sqrt(Math.max(0, sumPitchAbs2 / n - (double) avgPitchDeltaAbs * avgPitchDeltaAbs));
    }

    // ── Movement ──────────────────────────────────────────────────────────

    private void computeMovementProfile() {
        int fwd = 0, bwd = 0, left = 0, right = 0;
        int jumps = 0, sprints = 0, sneaks = 0, ground = 0;

        // Last known movement direction for transition matrix
        int lastDir = -1; // 0=F, 1=B, 2=L, 3=R
        int[][] transitions = new int[4][4];

        for (NeuroFrame f : frames) {
            if (f.movingForward) fwd++;
            if (f.movingBackward) bwd++;
            if (f.movingLeft) left++;
            if (f.movingRight) right++;
            if (f.jumping) jumps++;
            if (f.sprinting) sprints++;
            if (f.sneaking) sneaks++;
            if (f.onGround) ground++;

            int dir = f.movingForward ? 0 : f.movingBackward ? 1 : f.movingLeft ? 2 : f.movingRight ? 3 : -1;
            if (lastDir >= 0 && dir >= 0) {
                transitions[lastDir][dir]++;
            }
            lastDir = dir;
        }

        float n = totalFrames;
        forwardRatio = (float) fwd / n;
        backwardRatio = (float) bwd / n;
        leftRatio = (float) left / n;
        rightRatio = (float) right / n;
        jumpRatio = (float) jumps / n;
        sprintRatio = (float) sprints / n;
        sneakRatio = (float) sneaks / n;
        groundRatio = (float) ground / n;

        preferredStrafe = rightRatio - leftRatio;

        for (int i = 0; i < 4; i++) {
            int rowSum = 0;
            for (int j = 0; j < 4; j++) rowSum += transitions[i][j];
            for (int j = 0; j < 4; j++) {
                movementTransitionMatrix[i][j] = rowSum > 0 ? (float) transitions[i][j] / rowSum : 0.25f;
            }
        }
    }

    // ── Attack ────────────────────────────────────────────────────────────

    private void computeAttackProfile() {
        totalAttacks = 0;
        totalCrits = 0;
        totalMisses = 0;
        long sumDelay = 0;
        int delayCount = 0;

        for (int bin = 0; bin < DELAY_BINS; bin++) attackDelayHistogram[bin] = 0;

        for (NeuroFrame f : frames) {
            if (!f.attacked) continue;
            totalAttacks++;
            if (f.critical) totalCrits++;
            if (f.missed) totalMisses++;

            if (f.attackDelayMs > 0 && f.attackDelayMs < MAX_DELAY_MS) {
                sumDelay += f.attackDelayMs;
                delayCount++;
                int bin = Math.min((int) (f.attackDelayMs / (MAX_DELAY_MS / DELAY_BINS)), DELAY_BINS - 1);
                attackDelayHistogram[bin]++;
            }
        }

        avgAttackDelay = delayCount > 0 ? (float) sumDelay / delayCount : 200f;
        critRatio = totalAttacks > 0 ? (float) totalCrits / totalAttacks : 0f;
        missRate = totalAttacks > 0 ? (float) totalMisses / totalAttacks : 0f;

        // std of attack delay
        if (delayCount > 0) {
            double sumDelay2 = 0;
            for (NeuroFrame f : frames) {
                if (f.attacked && f.attackDelayMs > 0 && f.attackDelayMs < MAX_DELAY_MS) {
                    float d = f.attackDelayMs - avgAttackDelay;
                    sumDelay2 += (double) d * d;
                }
            }
            stdAttackDelay = (float) Math.sqrt(sumDelay2 / delayCount);
        } else {
            stdAttackDelay = 50f;
        }

        // Normalize histogram
        float attackTotal = Math.max(totalAttacks, 1);
        for (int bin = 0; bin < DELAY_BINS; bin++) {
            attackDelayHistogram[bin] /= attackTotal;
        }
    }

    // ── Distance ──────────────────────────────────────────────────────────

    private void computeDistanceProfile() {
        double sumDist = 0, sumDist2 = 0;
        minDistance = Float.MAX_VALUE;
        maxDistance = 0;
        for (int bin = 0; bin < DIST_BINS; bin++) distanceHistogram[bin] = 0;

        for (NeuroFrame f : frames) {
            float d = f.distanceToTarget;
            sumDist += d;
            sumDist2 += (double) d * d;
            if (d < minDistance) minDistance = d;
            if (d > maxDistance) maxDistance = d;
            int bin = Math.min((int) (d / (MAX_DISTANCE / DIST_BINS)), DIST_BINS - 1);
            distanceHistogram[bin]++;
        }

        float n = totalFrames;
        avgDistance = (float) (sumDist / n);
        stdDistance = (float) Math.sqrt(Math.max(0, sumDist2 / n - (double) avgDistance * avgDistance));

        for (int bin = 0; bin < DIST_BINS; bin++) {
            distanceHistogram[bin] /= n;
        }
    }

    // ── Target tracking ───────────────────────────────────────────────────

    private void computeTargetTrackingProfile() {
        double sumTy = 0, sumTp = 0;
        int count = 0;
        for (NeuroFrame f : frames) {
            if (f.attacked) {
                sumTy += Math.abs(f.targetYawDelta);
                sumTp += Math.abs(f.targetPitchDelta);
                count++;
            }
        }
        avgTargetYawDelta = count > 0 ? (float) (sumTy / count) : 0f;
        avgTargetPitchDelta = count > 0 ? (float) (sumTp / count) : 0f;
    }

    // ── Behavioural traits ────────────────────────────────────────────────

    private void computeBehaviouralTraits() {
        // Aggression: how fast/aggressive the camera moves
        float rawAggression = (avgYawDeltaAbs + avgPitchDeltaAbs) / 40f;
        aggression = Math.min(1f, rawAggression);

        // Smoothness: inversely related to std/mean ratio
        float yawCV = avgYawDeltaAbs > 0.01f ? stdYawDeltaAbs / avgYawDeltaAbs : 1f;
        float pitchCV = avgPitchDeltaAbs > 0.01f ? stdPitchDeltaAbs / avgPitchDeltaAbs : 1f;
        float rawSmoothness = 1f - Math.min(1f, (yawCV + pitchCV) / 4f);
        smoothness = Math.max(0.1f, rawSmoothness);

        // Camera activity: how much the camera moves
        cameraActivity = Math.min(1f, (avgYawDeltaAbs + avgPitchDeltaAbs) / 30f);
    }

    // ── Public API ────────────────────────────────────────────────────────

    // ── Rotation getters ──────────────────────────────────────────────────
    public float getAvgYawDelta() { return avgYawDelta; }
    public float getAvgPitchDelta() { return avgPitchDelta; }
    public float getStdYawDelta() { return stdYawDelta; }
    public float getStdPitchDelta() { return stdPitchDelta; }
    public float getAvgYawDeltaAbs() { return avgYawDeltaAbs; }
    public float getAvgPitchDeltaAbs() { return avgPitchDeltaAbs; }
    public float getStdYawDeltaAbs() { return stdYawDeltaAbs; }
    public float getStdPitchDeltaAbs() { return stdPitchDeltaAbs; }

    // ── Movement getters ──────────────────────────────────────────────────
    public float getForwardRatio() { return forwardRatio; }
    public float getBackwardRatio() { return backwardRatio; }
    public float getLeftRatio() { return leftRatio; }
    public float getRightRatio() { return rightRatio; }
    public float getJumpRatio() { return jumpRatio; }
    public float getSprintRatio() { return sprintRatio; }
    public float getSneakRatio() { return sneakRatio; }
    public float getGroundRatio() { return groundRatio; }
    public float getPreferredStrafe() { return preferredStrafe; }
    public float[][] getMovementTransitionMatrix() { return movementTransitionMatrix; }

    // ── Attack getters ────────────────────────────────────────────────────
    public float getAvgAttackDelay() { return avgAttackDelay; }
    public float getStdAttackDelay() { return stdAttackDelay; }
    public float getCritRatio() { return critRatio; }
    public float getMissRate() { return missRate; }
    public int getTotalAttacks() { return totalAttacks; }
    public float[] getAttackDelayHistogram() { return attackDelayHistogram; }

    // ── Distance getters ──────────────────────────────────────────────────
    public float getAvgDistance() { return avgDistance; }
    public float getStdDistance() { return stdDistance; }
    public float getMinDistance() { return minDistance; }
    public float getMaxDistance() { return maxDistance; }
    public float[] getDistanceHistogram() { return distanceHistogram; }

    // ── Target tracking ───────────────────────────────────────────────────
    public float getAvgTargetYawDelta() { return avgTargetYawDelta; }
    public float getAvgTargetPitchDelta() { return avgTargetPitchDelta; }

    // ── Behavioural traits ────────────────────────────────────────────────
    public float getAggression() { return aggression; }
    public float getSmoothness() { return smoothness; }
    public float getCameraActivity() { return cameraActivity; }

    // ── General ──────────────────────────────────────────────────────────
    public String getName() { return name; }
    public long getCreatedAt() { return createdAt; }
    public List<NeuroFrame> getFrames() { return frames; }
    public int getTotalFrames() { return totalFrames; }

    @Override
    public String toString() {
        return String.format("%s [%d frames | delay=%.0fms(%.0f) | crit=%.0f%% | miss=%.0f%% | dist=%.1f(%.1f) | aggr=%.2f | smooth=%.2f]",
                name, totalFrames, avgAttackDelay, stdAttackDelay, critRatio * 100, missRate * 100,
                avgDistance, stdDistance, aggression, smoothness);
    }
}
