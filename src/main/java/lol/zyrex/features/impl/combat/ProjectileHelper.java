package lol.zyrex.features.impl.combat;

import lol.zyrex.common.repository.friend.FriendUtils;
import lol.zyrex.events.player.RotationUpdateEvent;
import lol.zyrex.events.render.WorldRenderEvent;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.managers.event.types.EventType;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.display.geometry.Render3D;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.utils.features.aura.warp.Turns;
import lol.zyrex.utils.features.aura.warp.TurnsConfig;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.math.task.TaskPriority;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class ProjectileHelper extends Module {

    public static ProjectileHelper getInstance() {
        return lol.zyrex.utils.client.Instance.get(ProjectileHelper.class);
    }

    private final SliderSettings searchDistance = new SliderSettings("Дистанция", "Радиус поиска цели")
            .setValue(48).range(5F, 128F);

    private final SliderSettings fovSetting = new SliderSettings("FOV", "Угол обзора (180 = полусфера)")
            .setValue(180).range(15F, 180F);

    public SliderSettings getFovSetting() {
        return fovSetting;
    }

    private final SliderSettings wallRange = new SliderSettings("Wall Range", "Макс дист сквозь стены")
            .setValue(8).range(0F, 64F);

    private final MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтр целей по типу")
            .value("Players", "Mobs", "Animals", "Armor Stand")
            .selected("Players", "Mobs", "Animals");

    private final MultiSelectSetting features = new MultiSelectSetting("Опции", "Доп. функции")
            .value("Wall Check", "FOV Visualize", "Trajectory", "Predict", "Auto Shoot")
            .selected("Wall Check", "FOV Visualize", "Predict");

    private final SelectSetting aimPoint = new SelectSetting("Точка", "Куда целиться")
            .value("Auto", "Head", "Body", "Feet")
            .selected("Auto");

    private LivingEntity target;
    private Vec3d lastVel;
    private Vec3d lastOrigin;
    private Vec3d prevPos;
    private LivingEntity prevTarget;

    public ProjectileHelper() {
        super("ProjectileHelper", "Projectile Helper", ModuleCategory.COMBAT);
        setup(searchDistance, fovSetting, wallRange, targetType, features, aimPoint);
    }

    private float getCharge() {
        ItemStack stack = mc.player.getMainHandStack();
        if (!(stack.getItem() instanceof BowItem)) return 1f;
        if (!mc.player.isUsingItem()) return 0f;
        return MathHelper.clamp((mc.player.getItemUseTime() + tickCounter.getTickDelta(false)) / 20f, 0f, 1f);
    }

    private double getSpeed() {
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.getItem() instanceof BowItem) return 3.0 * getCharge();
        if (stack.getItem() instanceof CrossbowItem && ((CrossbowItem) stack.getItem()).isCharged(stack)) return 3.0;
        if (stack.getItem() instanceof TridentItem) return 2.5;
        return 0;
    }

    private boolean isHolding() {
        ItemStack main = mc.player.getMainHandStack();
        if (main.getItem() instanceof BowItem) return mc.player.isUsingItem();
        if (main.getItem() instanceof CrossbowItem) return ((CrossbowItem) main.getItem()).isCharged(main);
        if (main.getItem() instanceof TridentItem) return mc.player.getItemUseTime() >= 10;
        return false;
    }

    private double computeFlightTime(double h, double speed) {
        if (h < 0.01 || speed <= 0) return 0;
        double maxH = speed / 0.01;
        if (h >= maxH) return 300;
        return Math.log(1 - 0.01 * h / speed) / Math.log(0.99);
    }

    private double computePitchAnalytical(double dx, double dy, double dz, double speed) {
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h < 0.5) return -Math.toDegrees(Math.atan2(dy, h));

        double bestPitch = -Math.toDegrees(Math.atan2(dy, h));
        double bestError = Double.MAX_VALUE;

        for (double pitch = -60; pitch <= 60; pitch += 0.5) {
            double rad = Math.toRadians(pitch);
            double vh = speed * Math.cos(rad);
            if (vh <= 0.01 * h) continue;

            double t = computeFlightTime(h, vh);
            if (t <= 0 || t > 300) continue;

            double vy0 = -speed * Math.sin(rad);
            double yAtTarget = (vy0 + 5.0) * (1 - Math.pow(0.99, t)) / 0.01 - 5.0 * t;
            double error = Math.abs(yAtTarget - dy);

            if (error < bestError) {
                bestError = error;
                bestPitch = pitch;
                if (error < 0.05) break;
            }
        }

        return bestPitch;
    }

    private boolean isBlocked(Vec3d from, Vec3d to) {
        BlockHitResult r = mc.world.raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (r.getType() == HitResult.Type.MISS) return false;
        return r.getPos().distanceTo(from) < to.distanceTo(from) - 0.3;
    }

    private double angleTo(Vec3d pos) {
        Vec3d look = MathAngle.getRotationVector(mc.player.getPitch(), mc.player.getYaw());
        Vec3d dir = pos.subtract(mc.player.getEyePos()).normalize();
        return Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(dir), -1, 1)));
    }

    private boolean isValid(LivingEntity e) {
        if (e == null || e == mc.player || !e.isAlive()) return false;
        if (FriendUtils.isFriend(e)) return false;
        if (!targetType.isSelected("Players") && e instanceof PlayerEntity) return false;
        if (!targetType.isSelected("Mobs") && e instanceof MobEntity) return false;
        if (!targetType.isSelected("Animals") && e instanceof AnimalEntity) return false;
        if (!targetType.isSelected("Armor Stand") && e instanceof ArmorStandEntity) return false;
        return true;
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() != EventType.PRE) return;

        if (!isHolding()) {
            target = null;
            lastVel = null;
            lastOrigin = null;
            return;
        }

        if (target != null && (!target.isAlive() || !isValid(target))) {
            target = null;
        }

        if (target == null) {
            Vec3d eye = mc.player.getEyePos();
            float fov = fovSetting.getValue();
            boolean wall = features.isSelected("Wall Check");
            float maxWr = wallRange.getValue();
            double bestAngle = Double.MAX_VALUE;

            for (var entity : mc.world.getEntities()) {
                if (!(entity instanceof LivingEntity le) || !isValid(le)) continue;
                Vec3d pos = le.getPos().add(0, le.getHeight() * 0.5, 0);
                double dist = pos.distanceTo(eye);
                if (dist > searchDistance.getValue()) continue;

                double a = angleTo(pos);
                if (a > fov) continue;

                if (wall && isBlocked(eye, pos) && dist > maxWr) continue;

                if (a < bestAngle) {
                    bestAngle = a;
                    target = le;
                }
            }
        }

        if (target == null) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d playerVel = mc.player.getVelocity();
        Vec3d targetPos = target.getPos();
        Vec3d targetCenter = targetPos.add(0, target.getHeight() * 0.5, 0);
        double speed = getSpeed();

        Vec3d targetVel;
        if (target == prevTarget && prevPos != null) {
            targetVel = targetPos.subtract(prevPos);
        } else {
            targetVel = target.getVelocity();
        }
        prevPos = targetPos;
        prevTarget = target;

        if (features.isSelected("Predict")) {
            Vec3d delta = targetCenter.subtract(eye);
            double hDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            double flightTime = computeFlightTime(hDist, Math.max(speed, 0.1));
            flightTime = MathHelper.clamp(flightTime, 0.5, 60);
            targetCenter = targetCenter.add(targetVel.multiply(flightTime));
        }

        eye = eye.add(playerVel);

        double hRatio = MathHelper.clamp(eye.distanceTo(targetCenter) / 64.0, 0.2, 0.8);
        if (aimPoint.isSelected("Head")) hRatio = 0.9;
        else if (aimPoint.isSelected("Body")) hRatio = 0.5;
        else if (aimPoint.isSelected("Feet")) hRatio = 0.05;
        Vec3d aim = targetCenter.add(0, target.getHeight() * (hRatio - 0.5), 0);

        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) computePitchAnalytical(dx, dy, dz, speed);

        pitch = MathHelper.clamp(pitch, -90f, 90f);

        Vec3d dir = MathAngle.getRotationVector(pitch, yaw);
        lastVel = dir.multiply(speed).add(playerVel);
        lastOrigin = eye;

        TurnsConnection.INSTANCE.rotateTo(
                new Turns(yaw, pitch),
                TurnsConfig.DEFAULT,
                TaskPriority.HIGH_IMPORTANCE_1,
                this
        );

        if (features.isSelected("Auto Shoot") && mc.player.isUsingItem() && getCharge() >= 0.95f) {
            mc.interactionManager.stopUsingItem(mc.player);
            mc.player.swingHand(mc.player.getActiveHand());
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (features.isSelected("Trajectory") && lastOrigin != null && lastVel != null) {
            List<Vec3d> pts = new ArrayList<>();
            Vec3d pos = lastOrigin;
            Vec3d vel = lastVel;
            pts.add(pos);

            for (int t = 0; t < 300; t++) {
                Vec3d prev = pos;
                pos = pos.add(vel);
                boolean water = mc.world.getFluidState(BlockPos.ofFloored(pos)).isIn(net.minecraft.registry.tag.FluidTags.WATER);
                vel = vel.multiply(water ? 0.6 : 0.99).add(0, -0.05, 0);
                pts.add(pos);

                BlockHitResult hit = mc.world.raycast(new RaycastContext(
                        prev, pos, RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE, mc.player));
                if (hit.getType() != HitResult.Type.MISS || pos.y < -128) break;
            }

            for (int i = 1; i < pts.size(); i++) {
                float p = (float) i / pts.size();
                int c = ColorAssist.multAlpha(ColorAssist.getClientColor(), MathHelper.clamp(p, 0.2f, 1f));
                Render3D.drawLine(pts.get(i - 1), pts.get(i), c, 1.5f, false);
            }
        }

        if (features.isSelected("FOV Visualize")) {
            Vec3d eye = mc.player.getEyePos();
            Vec3d look = MathAngle.getRotationVector(mc.player.getPitch(), mc.player.getYaw());
            double fovRad = Math.toRadians(fovSetting.getValue());
            double coneDist = 20;

            Vec3d center = eye.add(look.multiply(coneDist));
            Vec3d up = Math.abs(look.dotProduct(new Vec3d(0, 1, 0))) > 0.99
                    ? new Vec3d(1, 0, 0)
                    : new Vec3d(0, 1, 0);
            Vec3d right = look.crossProduct(up).normalize();
            Vec3d localUp = right.crossProduct(look).normalize();

            double coneRadius = coneDist * Math.tan(fovRad * 0.5);
            int segments = 24;

            int color = target != null
                    ? ColorAssist.getColor(100, 255, 100, 180)
                    : ColorAssist.getColor(255, 255, 255, 100);

            for (int i = 0; i < segments; i++) {
                double a1 = 2 * Math.PI * i / segments;
                double a2 = 2 * Math.PI * (i + 1) / segments;

                Vec3d p1 = center.add(right.multiply(Math.cos(a1) * coneRadius))
                        .add(localUp.multiply(Math.sin(a1) * coneRadius));
                Vec3d p2 = center.add(right.multiply(Math.cos(a2) * coneRadius))
                        .add(localUp.multiply(Math.sin(a2) * coneRadius));

                Render3D.drawLine(p1, p2, color, 1.5f, false);
            }
        }
    }

}
