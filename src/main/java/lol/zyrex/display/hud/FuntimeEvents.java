package lol.zyrex.display.hud;

import lol.zyrex.utils.client.managers.api.draggable.AbstractDraggable;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FuntimeEvents extends AbstractDraggable {

    private static final String API_URL = "https://api.funtime.su/method/events-info";
    private static final String SERVERS_URL = "https://api.funtime.su/method/servers-info";
    private static final String API_TOKEN = "8ea195f077a67ff4ba2b8b9204c4649c";
    private static final int MAX_EVENTS = 20;

    // Перевод ивентов на русский
    private static final Map<String, String> EVENT_NAMES = new HashMap<>();
    static {
        EVENT_NAMES.put("vulkan", "Вулкан");
        EVENT_NAMES.put("meteor_rain", "Метеоритный дождь");
        EVENT_NAMES.put("myst_beacon", "Маяк");
        EVENT_NAMES.put("beacon", "Маяк");
        EVENT_NAMES.put("deathchest", "Сундук смерти");
        EVENT_NAMES.put("death_chest", "Сундук смерти");
        EVENT_NAMES.put("vote", "Голосование");
        EVENT_NAMES.put("boss", "Босс");
        EVENT_NAMES.put("airdrop", "Аирдроп");
        EVENT_NAMES.put("air_drop", "Аирдроп");
        EVENT_NAMES.put("convoy", "Конвой");
        EVENT_NAMES.put("dragon", "Дракон");
        EVENT_NAMES.put("ender_dragon", "Эндер Дракон");
        EVENT_NAMES.put("wither", "Визер");
        EVENT_NAMES.put("raid", "Рейд");
        EVENT_NAMES.put("treasure", "Сокровище");
        EVENT_NAMES.put("meteor", "Метеорит");
        EVENT_NAMES.put("supply", "Снабжение");
        EVENT_NAMES.put("caravan", "Караван");
        EVENT_NAMES.put("invasion", "Вторжение");
        EVENT_NAMES.put("arena", "Арена");
        EVENT_NAMES.put("tournament", "Турнир");
        EVENT_NAMES.put("pvp", "PvP");
        EVENT_NAMES.put("hunt", "Охота");
        EVENT_NAMES.put("quest", "Квест");
        EVENT_NAMES.put("event", "Ивент");
        EVENT_NAMES.put("drop", "Дроп");
        EVENT_NAMES.put("chest", "Сундук");
        EVENT_NAMES.put("loot", "Лут");
        EVENT_NAMES.put("spawn", "Спавн");
        EVENT_NAMES.put("world_boss", "Мировой босс");
        EVENT_NAMES.put("miniboss", "Мини-босс");
        EVENT_NAMES.put("mini_boss", "Мини-босс");
        EVENT_NAMES.put("hell", "Адская резня");
        EVENT_NAMES.put("hellm", "Адская резня");
    }

    private List<EventInfo> events = new ArrayList<>();
    private List<String> allServers = new ArrayList<>();
    private long lastUpdate = 0;
    private long lastTick = 0;
    private long lastServersUpdate = 0;
    private static final long UPDATE_INTERVAL = 30000;
    private static final long SERVERS_UPDATE_INTERVAL = 300000;

    public static FuntimeEvents getInstance() {
        return Instance.getDraggable(FuntimeEvents.class);
    }

    public FuntimeEvents() {
        super("Funtime Events", 10, 100, 160, 23, true);
        fetchServers();
    }

    @Override
    public boolean visible() {
        if (!lol.zyrex.features.impl.render.Hud.getInstance().interfaceSettings.isSelected("Funtime Events")) {
            return false;
        }
        return !events.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTick >= 1000) {
            lastTick = currentTime;
            for (EventInfo event : events) {
                event.timeSecondsLeft--;
            }
            events.removeIf(e -> e.timeSecondsLeft <= 0);
        }
        if (currentTime - lastServersUpdate > SERVERS_UPDATE_INTERVAL) {
            lastServersUpdate = currentTime;
            fetchServers();
        }
        if (currentTime - lastUpdate > UPDATE_INTERVAL) {
            lastUpdate = currentTime;
            fetchEvents();
        }
    }

    private void fetchServers() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(SERVERS_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization-Token", API_TOKEN);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    parseServers(response.toString());
                }
                conn.disconnect();
            } catch (Exception ignored) {}
        });
    }

    private void parseServers(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("response")) {
                JsonArray arr = root.getAsJsonArray("response");
                List<String> servers = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++) {
                    servers.add(arr.get(i).getAsString());
                }
                allServers = servers;
            }
        } catch (Exception ignored) {}
    }

    private void fetchEvents() {
        if (allServers.isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            try {
                List<EventInfo> newEvents = new ArrayList<>();
                for (int i = 0; i < allServers.size(); i += 10) {
                    List<String> batch = allServers.subList(i, Math.min(i + 10, allServers.size()));
                    String servers = String.join(",", batch);
                    String urlStr = API_URL + "?event-type=system&server-type=" + servers;
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization-Token", API_TOKEN);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                        reader.close();
                        parseEventsFromJson(response.toString(), newEvents);
                    }
                    conn.disconnect();
                }
                
                // Фильтруем и сортируем
                List<EventInfo> filtered = newEvents.stream()
                        .filter(e -> e.timeSecondsLeft > 0 && !e.phase.equals("RUNNING"))
                        .sorted((a, b) -> Integer.compare(a.timeSecondsLeft, b.timeSecondsLeft))
                        .limit(MAX_EVENTS)
                        .collect(Collectors.toList());
                
                // Обновляем только если есть новые данные
                if (!filtered.isEmpty()) {
                    events = filtered;
                }
            } catch (Exception ignored) {}
        });
    }

    private void parseEventsFromJson(String json, List<EventInfo> eventsList) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray responseArray = root.getAsJsonArray("response");
            for (int i = 0; i < responseArray.size(); i++) {
                JsonObject serverObj = responseArray.get(i).getAsJsonObject();
                String server = serverObj.has("server") ? serverObj.get("server").getAsString() : "";
                if (serverObj.has("events")) {
                    JsonArray eventsArray = serverObj.getAsJsonArray("events");
                    for (int j = 0; j < eventsArray.size(); j++) {
                        JsonObject eventObj = eventsArray.get(j).getAsJsonObject();
                        String id = eventObj.has("id") ? eventObj.get("id").getAsString() : "";
                        int timeLeft = eventObj.has("time-seconds-left") ? eventObj.get("time-seconds-left").getAsInt() : 0;
                        String phase = eventObj.has("phase") ? eventObj.get("phase").getAsString() : "";
                        String loot = eventObj.has("loot") ? eventObj.get("loot").getAsString() : "";
                        if (!id.isEmpty() && timeLeft > 0 && !phase.equals("RUNNING")) {
                            String name = translateEvent(id);
                            eventsList.add(new EventInfo(name, server, timeLeft, phase, loot));
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private String translateEvent(String id) {
        if (EVENT_NAMES.containsKey(id.toLowerCase())) {
            return EVENT_NAMES.get(id.toLowerCase());
        }
        // Автоперевод: первая буква заглавная
        return id.substring(0, 1).toUpperCase() + id.substring(1).toLowerCase().replace("_", " ");
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(11, Fonts.Type.DEFAULT);
        FontRenderer fontBold = Fonts.getSize(12, Fonts.Type.BOLD);
        FontRenderer icon = Fonts.getSize(14, Fonts.Type.ICONS);
        FontRenderer small = Fonts.getSize(10, Fonts.Type.DEFAULT);

        int headerHeight = 16;
        int rowHeight = 12;
        int visibleEvents = Math.min(events.size(), MAX_EVENTS);
        int bodyHeight = visibleEvents > 0 ? visibleEvents * rowHeight + 4 : rowHeight + 4;
        int totalHeight = headerHeight + bodyHeight;

        // Заголовок
        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), headerHeight)
                .round(4, 0, 4, 0).quality(12)
                .color(new Color(0, 0, 0, 150).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), headerHeight)
                .round(4, 0, 4, 0)
                .thickness(0.1f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(
                        new Color(18, 19, 20, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(18, 19, 20, 75).getRGB())
                .build());

        fontBold.drawString(matrix, "Events", getX() + 7, getY() + 5.5f, ColorAssist.getText());
        icon.drawString(matrix, "\uF073", getX() + getWidth() - 12f, getY() + 6.5f, new Color(225, 225, 255, 255).getRGB());

        // Тело
        blur.render(ShapeProperties.create(matrix, getX(), getY() + headerHeight, getWidth(), bodyHeight)
                .round(0, 4, 0, 4).quality(12)
                .color(new Color(0, 0, 0, 150).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, getX(), getY() + headerHeight, getWidth(), bodyHeight)
                .round(0, 4, 0, 4)
                .thickness(0.1f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(
                        new Color(18, 19, 20, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(18, 19, 20, 75).getRGB())
                .build());

        float yOffset = getY() + headerHeight + 3;
        int maxWidth = 160;

        if (events.isEmpty()) {
            if (PlayerInteractionHelper.isChat(mc.currentScreen)) {
                font.drawString(matrix, "Ожидание...", getX() + 8, yOffset + 1, new Color(150, 150, 150, 200).getRGB());
            }
        } else {
            for (int i = 0; i < Math.min(events.size(), MAX_EVENTS); i++) {
                EventInfo event = events.get(i);
                
                font.drawString(matrix, event.name, getX() + 6, yOffset + 1, new Color(255, 255, 255, 220).getRGB());

                // Сервер - светлый и приятный
                String serverShort = formatServer(event.server);
                float serverWidth = small.getStringWidth(serverShort);
                small.drawString(matrix, serverShort, getX() + getWidth() - serverWidth - 38, yOffset + 2, new Color(180, 200, 255, 200).getRGB());

                // Время - любое
                String time = formatTime(event.timeSecondsLeft);
                float timeWidth = font.getStringWidth(time);
                font.drawString(matrix, time, getX() + getWidth() - timeWidth - 6, yOffset + 1, new Color(200, 200, 200, 255).getRGB());

                float rowWidth = font.getStringWidth(event.name) + serverWidth + timeWidth + 50;
                maxWidth = (int) Math.max(rowWidth, maxWidth);
                
                yOffset += rowHeight;
            }
        }

        setWidth(maxWidth);
        setHeight(totalHeight);
    }

    private String formatServer(String server) {
        // anarchy307 -> an307, anarchy23 -> an23 (1.21.4+)
        String short_ = server.replace("anarchy", "an");
        // Проверяем: если после "an" идёт 1-2 цифры (не 3), добавляем приписку
        if (short_.matches("an\\d{1,2}$")) {
            return short_ + " (1.21.4+)";
        }
        return short_;
    }

    private String formatTime(int seconds) {
        if (seconds <= 0) return "Скоро!";
        int hours = seconds / 3600;
        int mins = (seconds % 3600) / 60;
        int secs = seconds % 60;
        if (hours > 0) return String.format("%d:%02d:%02d", hours, mins, secs);
        if (mins > 0) return String.format("%d:%02d", mins, secs);
        return String.format("0:%02d", secs);
    }

    private static class EventInfo {
        String name;
        String server;
        int timeSecondsLeft;
        String phase;
        String loot;

        EventInfo(String name, String server, int timeSecondsLeft, String phase, String loot) {
            this.name = name;
            this.server = server;
            this.timeSecondsLeft = timeSecondsLeft;
            this.phase = phase;
            this.loot = loot;
        }
    }
}
