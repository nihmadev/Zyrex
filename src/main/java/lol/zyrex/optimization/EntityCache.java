package lol.zyrex.optimization;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Кэш энтити для оптимизации поиска и фильтрации
 */
public class EntityCache {
    
    private static EntityCache instance;
    
    private final List<LivingEntity> livingEntities = new ArrayList<>(128);
    private final List<PlayerEntity> players = new ArrayList<>(32);
    private final List<LivingEntity> nearbyEntities = new ArrayList<>(64);
    
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 50; // 50ms
    
    private Vec3d lastPlayerPos = Vec3d.ZERO;
    private double lastRange = 0;
    
    public static EntityCache getInstance() {
        if (instance == null) {
            instance = new EntityCache();
        }
        return instance;
    }
    
    /**
     * Обновить кэш энтити из мира
     */
    public void update(Iterable<Entity> worldEntities, Vec3d playerPos, double range) {
        long now = System.currentTimeMillis();
        
        // Проверяем нужно ли обновлять
        boolean needsUpdate = now - lastUpdateTime > UPDATE_INTERVAL ||
                              playerPos.squaredDistanceTo(lastPlayerPos) > 4 || // Игрок сдвинулся на 2+ блока
                              range != lastRange;
        
        if (!needsUpdate) return;
        
        lastUpdateTime = now;
        lastPlayerPos = playerPos;
        lastRange = range;
        
        livingEntities.clear();
        players.clear();
        nearbyEntities.clear();
        
        double rangeSq = range * range;
        
        for (Entity entity : worldEntities) {
            if (!(entity instanceof LivingEntity living)) continue;
            
            livingEntities.add(living);
            
            if (living instanceof PlayerEntity player) {
                players.add(player);
            }
            
            // Быстрая проверка дистанции
            if (isInRangeFast(living, playerPos, rangeSq)) {
                nearbyEntities.add(living);
            }
        }
    }
    
    /**
     * Быстрая проверка дистанции без sqrt
     */
    private boolean isInRangeFast(Entity entity, Vec3d pos, double rangeSq) {
        double dx = entity.getX() - pos.x;
        double dy = entity.getY() - pos.y;
        double dz = entity.getZ() - pos.z;
        return dx * dx + dy * dy + dz * dz <= rangeSq;
    }
    
    /**
     * Получить все LivingEntity
     */
    public List<LivingEntity> getLivingEntities() {
        return livingEntities;
    }
    
    /**
     * Получить всех игроков
     */
    public List<PlayerEntity> getPlayers() {
        return players;
    }
    
    /**
     * Получить энтити в радиусе
     */
    public List<LivingEntity> getNearbyEntities() {
        return nearbyEntities;
    }
    
    /**
     * Получить отфильтрованные энтити
     */
    public List<LivingEntity> getFiltered(Predicate<LivingEntity> filter) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : nearbyEntities) {
            if (filter.test(entity)) {
                result.add(entity);
            }
        }
        return result;
    }
    
    /**
     * Найти ближайшую энтити
     */
    public LivingEntity findNearest(Vec3d pos, Predicate<LivingEntity> filter) {
        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        
        for (LivingEntity entity : nearbyEntities) {
            if (!filter.test(entity)) continue;
            
            double distSq = entity.squaredDistanceTo(pos);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = entity;
            }
        }
        
        return nearest;
    }
    
    /**
     * Проверить есть ли энтити в Box
     */
    public boolean hasEntityInBox(Box box, Predicate<LivingEntity> filter) {
        for (LivingEntity entity : nearbyEntities) {
            if (filter.test(entity) && box.intersects(entity.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Очистить кэш
     */
    public void clear() {
        livingEntities.clear();
        players.clear();
        nearbyEntities.clear();
        lastUpdateTime = 0;
    }
}