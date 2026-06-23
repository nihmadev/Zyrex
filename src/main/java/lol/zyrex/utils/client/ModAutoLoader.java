package lol.zyrex.utils.client;

import lol.zyrex.utils.client.logs.Logger;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class ModAutoLoader {

    private static final String VIA_FABRIC_PLUS_URL = "https://cdn.modrinth.com/data/rIC2XJV4/versions/8sPJsNcy/ViaFabricPlus-4.0.5-BACKPORT.jar";
    private static final String VIA_FABRIC_PLUS_FILE = "ViaFabricPlus-4.0.5-BACKPORT.jar";
    
    private static final String SODIUM_URL = "https://cdn.modrinth.com/data/AANobbMI/versions/c3YkZvne/sodium-fabric-0.6.13%2Bmc1.21.4.jar";
    private static final String SODIUM_FILE = "sodium-fabric-0.6.13+mc1.21.4.jar";

    public static void checkAndDownloadMods() {
        CompletableFuture.runAsync(() -> {
            try {
                Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
                
                // ViaFabricPlus
                Path viaFile = modsDir.resolve(VIA_FABRIC_PLUS_FILE);
                if (!Files.exists(viaFile)) {
                    Logger.info("ViaFabricPlus not found, downloading...");
                    downloadFile(VIA_FABRIC_PLUS_URL, viaFile);
                    Logger.info("ViaFabricPlus downloaded! Restart game to apply.");
                }
                
                // Sodium
                Path sodiumFile = modsDir.resolve(SODIUM_FILE);
                if (!Files.exists(sodiumFile)) {
                    Logger.info("Sodium not found, downloading...");
                    downloadFile(SODIUM_URL, sodiumFile);
                    Logger.info("Sodium downloaded! Restart game to apply.");
                }
            } catch (Exception e) {
                Logger.error("Failed to download mods: " + e.getMessage());
            }
        });
    }

    private static void downloadFile(String urlStr, Path destination) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        if (conn.getResponseCode() == 200) {
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        conn.disconnect();
    }
}
