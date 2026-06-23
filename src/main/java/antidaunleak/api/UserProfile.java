package antidaunleak.api;

import java.util.HashMap;
import java.util.Map;

public class UserProfile {

    private static final UserProfile instance = new UserProfile();

    public static UserProfile getInstance() {
        return instance;
    }

    private final Map<String, String> cache = new HashMap<>();

    private UserProfile() {
        cache.put("username", "Player");
        cache.put("hwid", "hwid-0000000000000-0000000");
        cache.put("role", "User");
        cache.put("uid", "0");
        cache.put("subTime", "0");
    }

    public String profile(String profile) {
        return cache.getOrDefault(profile, "");
    }
}
