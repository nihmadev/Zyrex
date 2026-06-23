package lol.zyrex.common.repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FakePlayerRepository {
    private static final Set<UUID> fakePlayers = new HashSet<>();

    public static void add(UUID uuid) {
        fakePlayers.add(uuid);
    }

    public static void remove(UUID uuid) {
        fakePlayers.remove(uuid);
    }

    public static boolean isFake(UUID uuid) {
        return fakePlayers.contains(uuid);
    }

    public static Set<UUID> getFakePlayers() {
        return Collections.unmodifiableSet(fakePlayers);
    }

    public static void clear() {
        fakePlayers.clear();
    }
}
