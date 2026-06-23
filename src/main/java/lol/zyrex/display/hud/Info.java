package lol.zyrex.display.hud;

import lol.zyrex.Zyrex;
import lol.zyrex.common.animation.Animation;
import lol.zyrex.common.animation.Direction;
import lol.zyrex.common.animation.implement.Decelerate;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.client.managers.api.draggable.AbstractDraggable;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.geometry.Render2D;
import lol.zyrex.utils.display.scissor.ScissorAssist;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.math.time.StopWatch;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Info extends AbstractDraggable {

    public static Info getInstance() {
        return Instance.getDraggable(Info.class);
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int H = 16;
    private static final int POPUP_ART = 40;
    private static final int POPUP_CTRL = 14;

    private final StopWatch trackChangeTimer = new StopWatch();
    private final Animation popupAnim = new Decelerate().setMs(400).setValue(1);
    private String lastTrackTitle = "";
    private boolean hovering = false;
    private int ping = 0;
    private boolean hasMedia = false;
    private final Animation contentFade = new Decelerate().setMs(300).setValue(1);
    private final Animation playBtnAnim = new Decelerate().setMs(180).setValue(1);
    private boolean wasPlaying = false;
    private boolean playStateInited = false;
    private String prevTitle = "";
    private String prevArtist = "";
    private String lastTrackArtist = "";
    private String prevPlayIcon = "\uF04B";
    private boolean lastMediaPlayState = false;

    // Popup geometry (updated every draw)
    private float ctrlCenterX, ctrlBaselineY;
    private int ctrlGap = 12;

    public Info() {
        super("Info", 0, 5, 60, H, false);
    }

    @Override
    public void tick() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            ping = entry != null ? entry.getLatency() : 0;
        }

        MediaPlayer mp = MediaPlayer.getInstance();
        hasMedia = mp != null && mp.playerName != null && mp.isCurrentPlaying();

        if (hasMedia) {
            String newTitle = mp.getCurrentTitle();
            String newArtist = mp.getCurrentArtist();

            boolean titleChanged = !newTitle.equals(lastTrackTitle) || !newArtist.equals(lastTrackArtist);
            if (titleChanged) {
                if (!lastTrackTitle.isEmpty()) {
                    prevTitle = lastTrackTitle;
                    prevArtist = lastTrackArtist;
                }
                lastTrackTitle = newTitle;
                lastTrackArtist = newArtist;
                contentFade.reset();
                contentFade.setDirection(Direction.FORWARDS);
                trackChangeTimer.reset();
                if (popupAnim.isDirection(Direction.BACKWARDS) || popupAnim.isFinished(Direction.BACKWARDS)) {
                    popupAnim.setDirection(Direction.FORWARDS);
                }
            }

            boolean curPlay = mp.isCurrentPlaying();
            if (!playStateInited) {
                wasPlaying = curPlay;
                lastMediaPlayState = curPlay;
                playStateInited = true;
            } else if (curPlay != lastMediaPlayState) {
                lastMediaPlayState = curPlay;
                if (wasPlaying != curPlay) {
                    prevPlayIcon = wasPlaying ? "\uF04C" : "\uF04B";
                    wasPlaying = curPlay;
                    playBtnAnim.reset();
                    playBtnAnim.setDirection(Direction.FORWARDS);
                }
            }
        } else {
            lastTrackTitle = "";
            lastTrackArtist = "";
            prevTitle = "";
            prevArtist = "";
            lastMediaPlayState = false;
        }

        boolean autoShow = !trackChangeTimer.finished(3500);
        if (!hovering && !autoShow && popupAnim.isDirection(Direction.FORWARDS)) {
            popupAnim.setDirection(Direction.BACKWARDS);
        }
        if (hovering && popupAnim.isDirection(Direction.BACKWARDS)) {
            popupAnim.setDirection(Direction.FORWARDS);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !hasMedia) return false;
        int mx = (int) mouseX;
        int my = (int) mouseY;

        float prevX = ctrlCenterX - ctrlGap - 7;
        float playX = ctrlCenterX - 7;
        float nextX = ctrlCenterX + ctrlGap - 7;
        float by = ctrlBaselineY - 3;
        float bs = 14;

        if (Calculate.isHovered(mx, my, prevX, by, bs, bs)) {
            sendMediaCommand("prev");
            return true;
        }
        if (Calculate.isHovered(mx, my, playX, by, bs, bs)) {
            prevPlayIcon = wasPlaying ? "\uF04C" : "\uF04B";
            wasPlaying = !wasPlaying;
            playBtnAnim.reset();
            playBtnAnim.setDirection(Direction.FORWARDS);
            sendMediaCommand("playPause");
            return true;
        }
        if (Calculate.isHovered(mx, my, nextX, by, bs, bs)) {
            sendMediaCommand("next");
            return true;
        }
        return false;
    }

    private void sendMediaCommand(String command) {
        String prop = System.getProperty("zyrex.media.path");
        if (prop == null || prop.isEmpty()) return;
        new Thread(() -> {
            try {
                Path controlPath = Path.of(prop + ".control");
                Files.writeString(controlPath, "{\"command\":\"" + command + "\"}", StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }, "MediaCommand").start();
    }

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        ScissorAssist scissor = Zyrex.getInstance().getScissorManager();

        int ww = window.getScaledWidth();
        int mouseX = (int) (mc.mouse.getX() * ww / window.getWidth());
        int mouseY = (int) (mc.mouse.getY() * window.getScaledHeight() / window.getHeight());

        MediaPlayer mp = MediaPlayer.getInstance();
        hasMedia = mp != null && mp.playerName != null && mp.isCurrentPlaying();
        String title = hasMedia ? mp.getCurrentTitle() : "";
        String artist = hasMedia ? mp.getCurrentArtist() : "";

        FontRenderer font = Fonts.getSize(12, Fonts.Type.DEFAULT);
        FontRenderer timeFont = Fonts.getSize(12, Fonts.Type.SEMI);

        String timeStr = LocalTime.now().format(TIME_FMT);
        float tw = timeFont.getStringWidth(timeStr) + 10;
        float sw = 16;

        String infoText = "";
        float trw;
        if (hasMedia && !title.isEmpty()) {
            trw = 56;
        } else {
            infoText = mc.getCurrentFps() + " FPS";
            trw = 40;
        }

        float cx = ww / 2f;
        float total = tw + trw + sw;
        float gx = cx - total / 2f;

        float tx = gx;
        float rx = tx + tw;
        float sx = rx + trw;
        float by = getY();

        setWidth(Math.max((int) trw, 1));

        // TIME
        timeFont.drawString(matrix, timeStr, Math.round(tx + tw / 2f - timeFont.getStringWidth(timeStr) / 2f),
                by + H / 2f + 3 - timeFont.getStringHeight(timeStr) / 4f, ColorAssist.getText());

        // INFO (track or fps)
        drawPill(matrix, rx, by, trw, H);
        scissor.push(matrix.peek().getPositionMatrix(), rx, by, trw, H);
        if (hasMedia && !title.isEmpty()) {
            font.drawStringWithScroll(matrix, title, Math.round(rx + 8),
                    by + H / 2f + 3 - font.getStringHeight(title) / 4f,
                    trw - 16, ColorAssist.getText());
        } else {
            font.drawString(matrix, infoText, Math.round(rx + 8),
                    by + H / 2f + 3 - font.getStringHeight(infoText) / 4f,
                    ColorAssist.getText());
        }
        scissor.pop();

        // SIGNAL
        drawSignal(matrix, sx, by, sw);

        // === POPUP GEOMETRY ===
        float gapArtTitle = 2;
        float gapTitleArtist = -1;
        float gapArtistCtrl = 0;
        float pad = 4;

        float puW = 116;
        float puH = pad + POPUP_ART + gapArtTitle + 13 + gapTitleArtist + (artist.isEmpty() ? 0 : 11 + gapArtistCtrl) + POPUP_CTRL + pad;
        float puX = cx - puW / 2;
        float puY = by + H + 3;

        float artY = puY + pad;
        float titleY = artY + POPUP_ART + gapArtTitle;
        float artistY = titleY + 13 + gapTitleArtist;
        float ctrlY = artist.isEmpty() ? titleY + 13 + 3 : artistY + 11 + gapArtistCtrl;

        ctrlGap = 12;
        ctrlCenterX = puX + puW / 2;
        FontRenderer icons = Fonts.getSize(14, Fonts.Type.ICONS);
        ctrlBaselineY = ctrlY + POPUP_CTRL / 2f + 3 - icons.getStringHeight("I") / 4f;

        // === HOVER ===
        float popVal = popupAnim.getOutput().floatValue();
        boolean hTrack = Calculate.isHovered(mouseX, mouseY, rx, by, trw, H);
        boolean hPopup = popVal > 0.05f && Calculate.isHovered(mouseX, mouseY, puX, puY, puW, puH * popVal);
        hovering = hTrack || hPopup;

        // === ANIM STATE ===
        float contentAlpha = Math.min(1f, contentFade.getOutput().floatValue());
        if (contentAlpha >= 0.99f) {
            prevTitle = "";
            prevArtist = "";
        }
        boolean playAnimating = !playBtnAnim.isFinished(Direction.FORWARDS);
        float playProg = Math.min(1f, playBtnAnim.getOutput().floatValue());

        // === DRAW POPUP ===
        if (popVal > 0.01f && hasMedia) {
            float curH = puH * popVal;
            float curve = 5 * popVal;

            Calculate.setAlpha(popVal, () -> {
                blur.render(ShapeProperties.create(matrix, puX, puY, puW, curH)
                        .round(curve).quality(12)
                        .color(new Color(0, 0, 0, 150).getRGB()).build());
                rectangle.render(ShapeProperties.create(matrix, puX, puY, puW, curH)
                        .round(curve).thickness(0.5f)
                        .outlineColor(new Color(18, 19, 20, 40).getRGB())
                        .color(new Color(18, 19, 20, 80).getRGB()).build());

                // POPUP SCISSOR — clip content to animated area
                scissor.push(matrix.peek().getPositionMatrix(), puX, puY, puW, curH);

                // ARTWORK
                float artX = puX + (puW - POPUP_ART) / 2;
                Calculate.setAlpha(contentAlpha, () -> {
                    rectangle.render(ShapeProperties.create(matrix, artX, artY, POPUP_ART, POPUP_ART)
                            .round(4).color(new Color(0, 0, 0, 40).getRGB()).build());
                    if (contentAlpha > 0.01f)
                        Render2D.drawTexture(context, mp.getArtwork(), artX, artY, POPUP_ART, POPUP_ART);
                });

                // TITLE (crossfade)
                FontRenderer tf = Fonts.getSize(13, Fonts.Type.SEMI);
                if (!prevTitle.isEmpty()) {
                    String clippedPrev = truncate(prevTitle, puW - 16, tf);
                    tf.drawString(matrix, clippedPrev,
                            Math.round(puX + (puW - tf.getStringWidth(clippedPrev)) / 2),
                            titleY, ColorAssist.getText(1f - contentAlpha));
                }
                String clippedTitle = truncate(title, puW - 16, tf);
                tf.drawString(matrix, clippedTitle,
                        Math.round(puX + (puW - tf.getStringWidth(clippedTitle)) / 2),
                        titleY, ColorAssist.getText(contentAlpha));

                // ARTIST
                if (!artist.isEmpty()) {
                    FontRenderer af = Fonts.getSize(11, Fonts.Type.DEFAULT);
                    if (!prevArtist.isEmpty()) {
                        String clippedPrevA = truncate(prevArtist, puW - 16, af);
                        af.drawString(matrix, clippedPrevA,
                                Math.round(puX + (puW - af.getStringWidth(clippedPrevA)) / 2),
                                artistY, ColorAssist.getText((1f - contentAlpha) * 0.6f));
                    }
                    String clippedArtist = truncate(artist, puW - 16, af);
                    af.drawString(matrix, clippedArtist,
                            Math.round(puX + (puW - af.getStringWidth(clippedArtist)) / 2),
                            artistY, ColorAssist.getText(contentAlpha * 0.6f));
                }

                // CONTROLS
                drawCtrls(matrix, puX, puW, ctrlY, mp, playAnimating, playProg, prevPlayIcon);

                scissor.pop();
            });
        }
    }

    private void drawPill(MatrixStack matrix, float x, float y, float w, float h) {
        blur.render(ShapeProperties.create(matrix, x, y, w, h)
                .round(4).quality(12).color(new Color(0, 0, 0, 130).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, w, h)
                .round(4).thickness(0.4f).outlineColor(new Color(18, 19, 20, 50).getRGB())
                .color(new Color(18, 19, 20, 70).getRGB()).build());
    }

    private String truncate(String text, float maxW, FontRenderer font) {
        if (font.getStringWidth(text) <= maxW) return text;
        for (int i = text.length(); i > 0; i--) {
            String s = text.substring(0, i) + "...";
            if (font.getStringWidth(s) <= maxW) return s;
        }
        return "...";
    }

    private void drawSignal(MatrixStack matrix, float x, float y, float w) {
        FontRenderer wifiFont = Fonts.getSize(12, Fonts.Type.FA_SOLID);
        float alpha;
        if (ping < 60) alpha = 1.0F;
        else if (ping < 120) alpha = 0.8F;
        else if (ping < 200) alpha = 0.55F;
        else if (ping < 300) alpha = 0.3F;
        else alpha = 0.15F;
        wifiFont.drawString(matrix, "\uF1EB", x + w / 2 - wifiFont.getStringWidth("\uF1EB") / 2, y + H / 2f + 3 - wifiFont.getStringHeight("\uF1EB") / 4f, ColorAssist.getText(alpha));
    }

    private void drawCtrls(MatrixStack matrix, float boxX, float boxW, float y, MediaPlayer mp, boolean playAnim, float playProg, String prevIcon) {
        FontRenderer icon = Fonts.getSize(14, Fonts.Type.ICONS);
        float iw = icon.getStringWidth("I");
        ctrlGap = 12;
        ctrlCenterX = boxX + boxW / 2;
        float by = y + POPUP_CTRL / 2f + 3 - icon.getStringHeight("I") / 4f;
        ctrlBaselineY = by;

        icon.drawString(matrix, "\uF049", ctrlCenterX - ctrlGap - iw / 2, by, ColorAssist.getText(0.85F));
        String currIcon = wasPlaying ? "\uF04C" : "\uF04B";
        if (playAnim) {
            icon.drawString(matrix, prevIcon, ctrlCenterX - iw / 2, by, ColorAssist.getText(0.85F * (1f - playProg)));
            icon.drawString(matrix, currIcon, ctrlCenterX - iw / 2, by, ColorAssist.getText(playProg));
        } else {
            icon.drawString(matrix, currIcon, ctrlCenterX - iw / 2, by, ColorAssist.getText());
        }
        icon.drawString(matrix, "\uF050", ctrlCenterX + ctrlGap - iw / 2, by, ColorAssist.getText(0.85F));
    }
}
