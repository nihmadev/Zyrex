package lol.zyrex.utils.features.aura.neuro;

import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NeuroRepository {

    private static final NeuroRepository INSTANCE = new NeuroRepository();

    private final List<NeuroProfile> profiles = new ArrayList<>();
    private NeuroProfile activeProfile = null;

    private NeuroRepository() {}

    public static NeuroRepository getInstance() {
        return INSTANCE;
    }

    private Path getNeuroDir() {
        Path base = MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("zyrex").resolve("neuro");
        try {
            Files.createDirectories(base);
        } catch (IOException ignored) {}
        return base;
    }

    private Path profilePath(String name) {
        return getNeuroDir().resolve(name + ".dat");
    }

    public void save(NeuroProfile profile) {
        profile.finalizeStats();
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(profilePath(profile.getName()).toFile()))) {
            oos.writeObject(profile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (profiles.stream().noneMatch(p -> p.getName().equals(profile.getName()))) {
            profiles.add(profile);
        }
    }

    public void loadAll() {
        profiles.clear();
        File dir = getNeuroDir().toFile();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".dat"));
        if (files == null) return;
        for (File f : files) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                NeuroProfile p = (NeuroProfile) ois.readObject();
                profiles.add(p);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public NeuroProfile getByName(String name) {
        return profiles.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public List<NeuroProfile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    public NeuroProfile getActiveProfile() { return activeProfile; }

    public void setActiveProfile(NeuroProfile p) { this.activeProfile = p; }
}
