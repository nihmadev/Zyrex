package lol.zyrex.display.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fmedia.MediaAPI;
import lol.zyrex.Zyrex;
import lol.zyrex.features.impl.render.Hud;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.client.chat.StringHelper;
import lol.zyrex.utils.client.managers.api.draggable.AbstractDraggable;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.geometry.Render2D;
import lol.zyrex.utils.display.scissor.ScissorAssist;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.math.time.StopWatch;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

public class MediaPlayer extends AbstractDraggable {

    public static MediaPlayer getInstance() {
        return Instance.getDraggable(MediaPlayer.class);
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile String currentTitle = "Media Player";
    private volatile String currentArtist = "";
    private volatile long currentDurationMicros = 0;
    private volatile long currentPositionMicros = 0;
    private volatile boolean currentPlaying = false;
    private final Identifier artwork = Identifier.of("textures/music.png");
    private final StopWatch lastMedia = new StopWatch();
    public volatile String playerName;
    private float widthDuration;
    private volatile byte[] lastArtworkHash = new byte[0];
    private volatile String lastArtUrl = "";

    private static final int INFO_HEIGHT = 42;
    private static final int CONTROLS_GAP = 3;
    private static final int CONTROLS_HEIGHT = 20;
    private static final int TOTAL_HEIGHT = INFO_HEIGHT + CONTROLS_GAP + CONTROLS_HEIGHT;
    private final AtomicBoolean polling = new AtomicBoolean(false);
    private final Path mediaFilePath;

    public Identifier getArtwork() { return artwork; }
    public String getCurrentTitle() { return currentTitle; }
    public String getCurrentArtist() { return currentArtist; }
    public boolean isCurrentPlaying() { return currentPlaying; }
    public long getCurrentDurationMicros() { return currentDurationMicros; }
    public long getCurrentPositionMicros() { return currentPositionMicros; }

    public MediaPlayer() {
        super("Media Player", 10, 120, 100, TOTAL_HEIGHT, true);
        String prop = System.getProperty("zyrex.media.path");
        Path p = null;
        if (prop != null && !prop.isEmpty()) {
            try { p = Path.of(prop); } catch (Exception ignored) {}
        }
        mediaFilePath = p;
    }

    @Override
    public boolean visible() {
        return !lastMedia.finished(2000) || PlayerInteractionHelper.isChat(mc.currentScreen) || (playerName != null && currentPlaying);
    }

    @Override
    public void tick() {
        if (!Hud.getInstance().isState() || !Hud.getInstance().interfaceSettings.isSelected("Media Player") || mc.player == null) return;
        if (mediaFilePath != null) {
            tickFile();
        }
    }

    private void tickFile() {
        if (mc.player.age % 40 != 0) return;
        if (!polling.compareAndSet(false, true)) return;
        executorService.submit(() -> {
            try {
                byte[] bytes = Files.readAllBytes(mediaFilePath);
                String json = new String(bytes, StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                String title = obj.get("title").getAsString();
                if (title.isEmpty()) {
                    playerName = null;
                    currentPlaying = false;
                    return;
                }
                playerName = obj.get("playerName").getAsString();
                currentTitle = title;
                currentArtist = obj.get("artist").getAsString();
                currentDurationMicros = obj.get("durationMicros").getAsLong();
                currentPositionMicros = obj.get("positionMicros").getAsLong();
                currentPlaying = obj.get("playing").getAsBoolean();
                lastMedia.reset();

                String artUrl = obj.get("artUrl").getAsString();
                if (!artUrl.isEmpty() && !artUrl.equals(lastArtUrl)) {
                    lastArtUrl = artUrl;
                    byte[] raw = MediaAPI.downloadArtwork(artUrl);
                    if (raw.length > 0 && !Arrays.equals(lastArtworkHash, raw)) {
                        byte[] finalRaw = raw.clone();
                        lastArtworkHash = finalRaw;
                        mc.execute(() -> {
                            try {
                                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(finalRaw));
                                if (bi == null) return;
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write(bi, "png", baos);
                                NativeImage img = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
                                NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                                mc.getTextureManager().registerTexture(artwork, tex);
                            } catch (Exception e) {
                                System.out.println("[MediaPlayer] Art error: " + e);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("[MediaPlayer] File error: " + e);
            } finally {
                polling.set(false);
            }
        });
    }

    private void downloadArtwork(String artUrl) {
        try {
            byte[] raw = MediaAPI.downloadArtwork(artUrl);
            if (raw.length == 0 || Arrays.equals(lastArtworkHash, raw)) return;
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(raw));
            if (bi == null) return;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            byte[] pngBytes = baos.toByteArray();
            lastArtworkHash = raw.clone();
            NativeImage img = NativeImage.read(new ByteArrayInputStream(pngBytes));
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            mc.execute(() -> mc.getTextureManager().registerTexture(artwork, tex));
        } catch (Exception e) {
            System.out.println("[MediaPlayer] Art error: " + e);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY) && isCanDrag()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        ScissorAssist scissor = Zyrex.getInstance().getScissorManager();
        FontRenderer big = Fonts.getSize(14, Fonts.Type.DEFAULT);
        FontRenderer mini = Fonts.getSize(12, Fonts.Type.DEFAULT);
        FontRenderer icon = Fonts.getSize(17, Fonts.Type.ICONS);

        float wx = getX();
        float wy = getY();
        float ww = getWidth() + 15;

        int sizeArtwork = 32;
        int maxDurationWidth = (int) (ww - sizeArtwork - 12);
        int duration = (int) (currentDurationMicros / 1_000_000);
        int position = MathHelper.clamp((int) (currentPositionMicros / 1_000_000), 0, duration);

        if (playerName != null && currentPlaying) {
            position = Math.min((int) ((currentPositionMicros / 1_000_000) + lastMedia.elapsedTime() / 1000.0), duration);
        }

        String timeDuration = StringHelper.getDuration(duration);
        String timePosition = StringHelper.getDuration(position);
        widthDuration = MathHelper.clamp(Calculate.interpolateSmooth(1, widthDuration, duration > 0 ? (float) position / duration * maxDurationWidth : 0), 1, maxDurationWidth);

        // === MAIN INFO SECTION ===
        blur.render(ShapeProperties.create(matrix, wx, wy, ww, INFO_HEIGHT)
                .round(5).quality(12)
                .color(new Color(0, 0, 0, 150).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, wx, wy, ww, INFO_HEIGHT)
                .round(5)
                .thickness(0.1f)
                .outlineColor(new Color(18, 19, 20, 35).getRGB())
                .color(
                        new Color(18, 19, 20, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(18, 19, 20, 75).getRGB())
                .build());

        float textAreaX = wx + sizeArtwork + 8;
        float textAreaW = ww - sizeArtwork - 10;

        scissor.push(matrix.peek().getPositionMatrix(), textAreaX, wy, textAreaW, INFO_HEIGHT);
        big.drawStringWithScroll(matrix, currentTitle, textAreaX, wy + 6, 56, ColorAssist.getText());
        mini.drawStringWithScroll(matrix, currentArtist, textAreaX, wy + 15, 56, ColorAssist.getText(0.75F));
        scissor.pop();

        Render2D.drawTexture(context, artwork, wx + 4, wy + 4, sizeArtwork, 3, sizeArtwork, sizeArtwork, sizeArtwork, ColorAssist.getRect(1));

        float progressY = wy + 26;
        rectangle.render(ShapeProperties.create(matrix, textAreaX, progressY, maxDurationWidth, 3f).round(0.85F).color(ColorAssist.getRectDarker(0.75F)).build());
        rectangle.render(ShapeProperties.create(matrix, textAreaX, progressY, widthDuration, 3f).softness(4).round(1).color(ColorAssist.roundClientColor(0.2F)).build());
        rectangle.render(ShapeProperties.create(matrix, textAreaX, progressY, widthDuration, 3f).round(0.85F).color(ColorAssist.roundClientColor(1)).build());

        float timeY = wy + 32;
        mini.drawString(matrix, timePosition, textAreaX, timeY, ColorAssist.getText());
        mini.drawString(matrix, timeDuration, wx + ww - 4 - mini.getStringWidth(timeDuration), timeY, ColorAssist.getText());

        // === CONTROLS POPUP ===
        float cy = wy + INFO_HEIGHT + CONTROLS_GAP;
        int gap = 14;
        float iconW = icon.getStringWidth("I");
        float cw = Math.min(ww - 20, 3 * iconW + 2 * gap + 16);
        float cx = wx + (ww - cw) / 2;

        blur.render(ShapeProperties.create(matrix, cx, cy, cw, CONTROLS_HEIGHT)
                .round(5f).quality(12)
                .color(new Color(0, 0, 0, 150).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, cx, cy, cw, CONTROLS_HEIGHT)
                .round(5f)
                .thickness(0.1f)
                .outlineColor(new Color(18, 19, 20, 35).getRGB())
                .color(
                        new Color(18, 19, 20, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(18, 19, 20, 75).getRGB())
                .build());

        float ctrX = cx + cw / 2;
        float buttonY = cy + CONTROLS_HEIGHT / 2 + 3 - icon.getStringHeight("I") / 4f;
        icon.drawString(matrix, "\uF049", ctrX - gap - iconW / 2, buttonY, ColorAssist.rgba(225, 225, 225, 230));
        icon.drawString(matrix, currentPlaying ? "\uF04B" : "\uF04C", ctrX - iconW / 2, buttonY, ColorAssist.rgba(225, 225, 225, 230));
        icon.drawString(matrix, "\uF050", ctrX + gap - iconW / 2, buttonY, ColorAssist.rgba(225, 225, 225, 230));
    }
}
