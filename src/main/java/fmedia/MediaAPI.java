package fmedia;

import java.net.HttpURLConnection;
import java.net.URI;

public class MediaAPI {

    public static byte[] downloadArtwork(String artUrl) {
        if (artUrl == null || artUrl.isEmpty()) return new byte[0];
        try {
            var url = URI.create(artUrl).toURL();
            var connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Zyrex/1.0");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            try (var in = connection.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
