package lol.zyrex.optimization;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Менеджер оптимизаций клиента
 */
public class OptimizationManager {
    
    private static OptimizationManager instance;
    
    // Кэш энтити для уменьшения аллокаций
    private final List<LivingEntity> cachedLivingEntities = new CopyOnWriteArrayList<>();
    private final List<Entity> cachedEntities = new CopyOnWriteArrayList<>();
    private long lastEntityCacheUpdate = 0;
    private static final long ENTITY_CACHE_TTL = 50; // 50ms = ~1 тик
    
    // Пул объектов для Vec3d (избегаем создания новых объектов)
    private final Vec3d[] vec3dPool = new Vec3d[32];
    private int vec3dPoolIndex = 0;
    
    public static OptimizationManager getInstance() {
        if (instance == null) {
            instance = new OptimizationManager();
        }
        return instance;
    }
    
    private OptimizationManager() {
        for (int i = 0; i < vec3dPool.length; i++) {
            vec3dPool[i] = Vec3d.ZERO;
        }
    }
    
    /**
     * Получить кэшированный список LivingEntity
     */
    public List<LivingEntity> getCachedLivingEntities(Iterable<Entity> worldEntities, Vec3d playerPos, double maxRange) {
        long now = System.currentTimeMillis();
        if (now - lastEntityCacheUpdate > ENTITY_CACHE_TTL) {
            updateEntityCache(worldEntities, playerPos, maxRange);
            lastEntityCacheUpdate = now;
        }
        return cachedLivingEntities;
    }
    
    private void updateEntityCache(Iterable<Entity> worldEntities, Vec3d playerPos, double maxRange) {
        cachedLivingEntities.clear();
        double maxRangeSq = maxRange * maxRange;
        
        for (Entity entity : worldEntities) {
            if (!(entity instanceof LivingEntity living)) continue;
            
            // Быстрая проверка дистанции без sqrt
            double dx = entity.getX() - playerPos.x;
            double dy = entity.getY() - playerPos.y;
            double dz = entity.getZ() - playerPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            
            if (distSq <= maxRangeSq) {
                cachedLivingEntities.add(living);
            }
        }
    }
    
    /**
     * Быстрая проверка видимости без создания объектов
     */
    public boolean isInRangeFast(Entity entity, Vec3d playerPos, double maxRange) {
        double dx = entity.getX() - playerPos.x;
        double dy = entity.getY() - playerPos.y;
        double dz = entity.getZ() - playerPos.z;
        return dx * dx + dy * dy + dz * dz <= maxRange * maxRange;
    }
    
    /**
     * Быстрая проверка пересечения Box без аллокаций
     */
    public boolean boxIntersectsFast(Box box, double x, double y, double z) {
        return x >= box.minX && x <= box.maxX &&
               y >= box.minY && y <= box.maxY &&
               z >= box.minZ && z <= box.maxZ;
    }
    
    /**
     * Сброс кэша энтити (вызывать при смене мира)
     */
    public void clearEntityCache() {
        cachedLivingEntities.clear();
        cachedEntities.clear();
        lastEntityCacheUpdate = 0;
    }
    
    /**
     * Вызывается каждые 5 секунд для проверки памяти
     */
    public void tick() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        if ((double) usedMemory / maxMemory > 0.8) {
            System.gc();
        }
    }
}
