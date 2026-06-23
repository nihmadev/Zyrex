package lol.zyrex.display.hud;

import lol.zyrex.common.animation.Direction;
import lol.zyrex.features.impl.render.Hud;
import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import lol.zyrex.utils.client.managers.api.draggable.AbstractDraggable;
import lol.zyrex.features.module.Module;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.client.chat.StringHelper;
import lol.zyrex.Zyrex;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.*;

public class HotKeys extends AbstractDraggable {
    private List<Module> keysList = new ArrayList<>();
    private long lastKeyChange = 0;
    private java.lang.String currentRandomKey = "NONE";

    public HotKeys() {
        super("Hot Keys", 300, 40, 80, 23, true);
    }

    @Override
    public boolean visible() {
        return Hud.getInstance().interfaceSettings.isSelected("Hot Keys") && (!keysList.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen));
    }

    @Override
    public void tick() {
        keysList = Zyrex.getInstance().getModuleProvider().getModules().stream()
                .filter(module -> module.isState() && module.getKey() != -1)
                .toList();
        if (keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKeyChange >= 1000) {
                List<java.lang.String> availableKeys = List.of("A", "B", "C", "D", "E");
                currentRandomKey = availableKeys.get(new Random().nextInt(availableKeys.size()));
                lastKeyChange = currentTime;
            }
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer icon = Fonts.getSize(14, Fonts.Type.ICONS);

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 15.5F)
                .round(4,0,4,0).quality(12)
                .color(new Color(0, 0, 0, 150).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 15.5F)
                .round(4,0,4,0)
                .thickness(0.1f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(
                        new Color(18, 19, 20, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(18, 19, 20, 75).getRGB())
                .build());

        blur.render(ShapeProperties.create(matrix, getX(), getY() + 15.5F, getWidth(), getHeight() - 16)
                .round(0,4,0,4).quality(12)
                .color(new Color(0, 0, 0, 150).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, getX(), getY() + 15.5F, getWidth(), getHeight() - 16)
                .round(0,4,0,4)
                .thickness(0.1f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(
                        new Color(18, 19, 20, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(0, 2, 5, 75).getRGB(),
                        new Color(18, 19, 20, 75).getRGB())
                .build());

        font.drawString(matrix, getName(), getX() + 7, getY() + 6.5f, ColorAssist.getText());
        icon.drawString(matrix, "\uF11C", getX() + getWidth() - 16f, getY() + 6.5f, new Color(225, 225, 255, 255).getRGB());

        float centerX = getX() + getWidth() / 2F;
        int offset = 23;
        int maxWidth = 80;

        if (keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            java.lang.String name = "Example Module";
            java.lang.String bind = "[" + currentRandomKey + "]";
            int textColor = ColorAssist.getText();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int color = new Color(225, 225, 255, 255).getRGB();
            float bindWidth = fontModule.getStringWidth(bind);
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                fontModule.drawString(matrix, name, getX() + 5, centerY + 1, colorWithAlpha);
                fontModule.drawString(matrix, bind, getX() + getWidth() - bindWidth - 8, centerY + 1, color);
            });
            int width = (int) fontModule.getStringWidth(name + bind) + 20;
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        } else {
            for (Module module : keysList) {
                java.lang.String bind = "[" + StringHelper.getBindName(module.getKey()) + "]";
                float centerY = getY() + offset;
                float animation = module.getAnimation().getOutput().floatValue();
                int textColor = ColorAssist.getText();
                int textAlpha = 255;
                int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
                int color = new Color(225, 225, 255, 255).getRGB();
                float bindWidth = fontModule.getStringWidth(bind);
                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    fontModule.drawString(matrix, module.getName(), getX() + 5, centerY + 1, colorWithAlpha);
                    fontModule.drawString(matrix, bind, getX() + getWidth() - bindWidth - 8, centerY + 1, color);
                });
                float width = fontModule.getStringWidth(module.getName() + bind) + 20;
                maxWidth = (int) Math.max(width, maxWidth);
                offset += (int) (animation * 11);
            }
        }
        setWidth(maxWidth + 10);
        setHeight(offset);
    }
}