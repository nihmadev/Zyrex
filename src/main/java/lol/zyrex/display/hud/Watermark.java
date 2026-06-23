package lol.zyrex.display.hud;

import lol.zyrex.utils.client.managers.api.draggable.AbstractDraggable;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.geometry.Render2D;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.display.color.ColorAssist;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import java.awt.Color;

public class Watermark extends AbstractDraggable {
    private int fpsCount = 0;
    private static final Identifier LOGO = Identifier.of("minecraft", "logo.png");

    public Watermark() {
        super("Watermark", 10, 10, 92, 16, true);
    }

    @Override
    public void tick() {
        fpsCount = mc.getCurrentFps();
    }

    @Override
    public void drawDraggable(DrawContext e) {
        MatrixStack matrix = e.getMatrices();
        float iconWidth = 12;
        String username = mc.getSession().getUsername();
        String fps = fpsCount + " FPS";
        String serverIp = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Singleplayer";

        float usernameWidth = Fonts.getSize(12, Fonts.Type.DEFAULT).getStringWidth(username);
        float fpsWidth = Fonts.getSize(12, Fonts.Type.BOLD).getStringWidth(fps);
        float serverIpWidth = Fonts.getSize(12, Fonts.Type.BOLD).getStringWidth(serverIp);
        float totalWidth = iconWidth + usernameWidth + fpsWidth + serverIpWidth + 26;

        setWidth((int) totalWidth + 14);

        blur.render(ShapeProperties.create(matrix, getX(), getY(), totalWidth + 14, getHeight() + 4)
                .round(5f).quality(12)
                .color(new Color(0, 0, 0, 150).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), totalWidth + 14, getHeight() + 4)
                .round(5f)
                .thickness(0.1f)
                .outlineColor(new Color(18, 19, 20, 35).getRGB())
                .color(
                        new Color(18, 19, 20, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(18, 19, 20, 75).getRGB())
                .build());

        float logoSize = 12;
        float logoBoxHeight = getHeight() + 4;
        float logoY = getY() + (logoBoxHeight - logoSize) / 2;
        Render2D.drawTexture(e, LOGO, getX() + 3f, logoY, logoSize, logoSize);

        float currentX = getX() + iconWidth + 7;

        rectangle.render(ShapeProperties.create(matrix, currentX, getY() + 6.5f, 0.5F, getHeight() - 8)
                .round(0)
                .color(ColorAssist.getText(0.5F))
                .build());

        currentX += 4;

        Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(matrix, username, currentX + 2.5f, getY() + 9f, new Color(255, 255, 255, 255).getRGB());

        currentX += usernameWidth + 9;

        rectangle.render(ShapeProperties.create(matrix, currentX, getY() + 6.5f, 0.5F, getHeight() - 8)
                .round(0f)
                .color(ColorAssist.getText(0.5F))
                .build());

        currentX += 5;

        Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(matrix, fps, currentX, getY() + 9f, new Color(255, 255, 255, 255).getRGB());

        currentX += fpsWidth + 5;

        rectangle.render(ShapeProperties.create(matrix, currentX, getY() + 6.5f, 0.5F, getHeight() - 8)
                .round(0)
                .color(ColorAssist.getText(0.5F))
                .build());

        currentX += 5;

        Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(matrix, serverIp, currentX, getY() + 9f, new Color(255, 255, 255, 255).getRGB());
    }
}
