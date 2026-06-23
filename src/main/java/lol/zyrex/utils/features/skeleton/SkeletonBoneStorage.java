package lol.zyrex.utils.features.skeleton;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkeletonBoneStorage {
    private static final Map<UUID, Map<String, BoneData>> BONE_POSITIONS = new ConcurrentHashMap<>();

    public static void store(UUID uuid, String name, BoneData data) {
        BONE_POSITIONS.computeIfAbsent(uuid, k -> new HashMap<>()).put(name, data);
    }

    public static Map<String, BoneData> get(UUID uuid) {
        Map<String, BoneData> map = BONE_POSITIONS.get(uuid);
        return map != null ? map : Collections.emptyMap();
    }

    public static BoneData get(UUID uuid, String name) {
        Map<String, BoneData> map = BONE_POSITIONS.get(uuid);
        return map != null ? map.get(name) : null;
    }

    public static void remove(UUID uuid) {
        BONE_POSITIONS.remove(uuid);
    }

    public static void clear() {
        BONE_POSITIONS.clear();
    }
}
