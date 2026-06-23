package lol.zyrex.utils.features.aura.target;

import lol.zyrex.optimization.OptimizationManager;
import lol.zyrex.utils.features.aura.point.MultiPoint;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.utils.features.aura.utils.RaycastAngle;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import lol.zyrex.common.repository.friend.FriendUtils;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.features.impl.combat.AntiBot;
import lol.zyrex.utils.features.aura.rotations.constructor.LinearConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetFinder implements QuickImports {
    final MultiPoint pointFinder = new MultiPoint();
    final OptimizationManager optimizer = OptimizationManager.getInstance();
    
    // Кэшированные списки для избежания аллокаций
    final List<LivingEntity> filteredTargets = new ArrayList<>(32);
    
    LivingEntity currentTarget;
    List<LivingEntity> potentialTargets;

    public TargetFinder() {
        this.currentTarget = null;
        this.potentialTargets = new ArrayList<>(32);
    }

    public void lockTarget(LivingEntity target) {
        if (this.currentTarget == null) {
            this.currentTarget = target;
        }
    }

    public void releaseTarget() {
        this.currentTarget = null;
    }

    public void validateTarget(Predicate<LivingEntity> predicate) {
        findFirstMatch(predicate).ifPresent(this::lockTarget);

        if (this.currentTarget != null && !predicate.test(this.currentTarget)) {
            releaseTarget();
        }
    }

    public void searchTargets(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
        if (currentTarget != null && (!pointFinder.hasValidPoint(currentTarget, maxDistance, ignoreWalls) || getFov(currentTarget, maxDistance, ignoreWalls) > maxFov)) {
            releaseTarget();
        }

        // Используем оптимизированный метод вместо Stream API
        createFilteredTargets(entities, maxDistance, maxFov, ignoreWalls);
    }

    private double getFov(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
        Vec3d attackVector = pointFinder.computeVector(entity, maxDistance, TurnsConnection.INSTANCE.getRotation(), new LinearConstructor().randomValue(), ignoreWalls).getLeft();
        return RaycastAngle.rayTrace(maxDistance, entity.getBoundingBox()) ? 0 : TurnsConnection.computeRotationDifference(MathAngle.cameraAngle(), MathAngle.calculateAngle(attackVector));
    }

    /**
     * Оптимизированный метод фильтрации энтити без Stream API
     */
    private void createFilteredTargets(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
        filteredTargets.clear();
        
        Vec3d playerPos = mc.player.getPos();
        double maxDistSq = maxDistance * maxDistance;
        
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living)) continue;
            
            // Быстрая проверка дистанции без sqrt
            double dx = entity.getX() - playerPos.x;
            double dy = entity.getY() - playerPos.y;
            double dz = entity.getZ() - playerPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            
            if (distSq > maxDistSq) continue;
            
            // Проверка валидной точки и FOV
            if (!pointFinder.hasValidPoint(living, maxDistance, ignoreWalls)) continue;
            if (getFov(living, maxDistance, ignoreWalls) >= maxFov) continue;
            
            filteredTargets.add(living);
        }
        
        // Сортировка по дистанции (используем distanceTo только для финальной сортировки)
        if (filteredTargets.size() > 1) {
            filteredTargets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player)));
        }
        
        this.potentialTargets = filteredTargets;
    }

    private Optional<LivingEntity> findFirstMatch(Predicate<LivingEntity> predicate) {
        for (LivingEntity entity : potentialTargets) {
            if (predicate.test(entity)) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class EntityFilter {
        List<String> targetSettings;

        public boolean isValid(LivingEntity entity) {
            if (isLocalPlayer(entity)) return false;
            if (isInvalidHealth(entity)) return false;
            if (isBotPlayer(entity)) return false;
            return isValidEntityType(entity);
        }

        private boolean isLocalPlayer(LivingEntity entity) {
            return entity == mc.player;
        }

        private boolean isInvalidHealth(LivingEntity entity) {
            return !entity.isAlive() || entity.getHealth() <= 0;
        }

        private boolean isBotPlayer(LivingEntity entity) {
            return entity instanceof PlayerEntity player && AntiBot.getInstance().isBot(player);
        }

        private boolean isNakedPlayer(LivingEntity entity) {
            return entity.isPlayer();
        }

        private boolean isValidEntityType(LivingEntity entity) {
            return switch (entity) {
                case PlayerEntity player when targetSettings.contains("Friends") || !FriendUtils.isFriend(player) -> targetSettings.contains("Players");
                case AnimalEntity animal -> targetSettings.contains("Animals");
                case MobEntity mob -> targetSettings.contains("Mobs");
                case ArmorStandEntity armorStand -> targetSettings.contains("Armor Stand");

                default -> false;
            };
        }
    }
}