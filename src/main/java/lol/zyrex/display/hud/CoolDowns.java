package lol.zyrex.display.hud;

import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import lol.zyrex.utils.client.managers.api.draggable.AbstractDraggable;
import lol.zyrex.common.animation.Animation;
import lol.zyrex.common.animation.Direction;
import lol.zyrex.common.animation.implement.Decelerate;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.math.time.StopWatch;
import lol.zyrex.utils.client.chat.StringHelper;
import lol.zyrex.utils.display.geometry.Render2D;
import lol.zyrex.events.packet.PacketEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoolDowns extends AbstractDraggable {
    public static CoolDowns getInstance() {
        return Instance.getDraggable(CoolDowns.class);
    }

    public final List<CoolDown> list = new ArrayList<>();
    private long lastItemChange = 0;
    private int currentItemIndex = 0;
    private static final Item[] EXAMPLE_ITEMS = {
            Items.ENDER_EYE, Items.ENDER_PEARL, Items.SUGAR, Items.MACE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.TRIDENT, Items.CROSSBOW, Items.DRIED_KELP, Items.NETHERITE_SCRAP
    };

    public CoolDowns() {
        super("Cool Downs", 10, 40, 80, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
        list.stream().filter(c -> !Objects.requireNonNull(mc.player).getItemCooldownManager().isCoolingDown(c.item.getDefaultStack())).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastItemChange >= 1000) {
                currentItemIndex = (currentItemIndex + 1) % EXAMPLE_ITEMS.length;
                lastItemChange = currentTime;
            }
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        switch (e.getPacket()) {
            case CooldownUpdateS2CPacket c -> {
                Item item = Registries.ITEM.get(c.cooldownGroup());
                list.stream().filter(coolDown -> coolDown.item.equals(item)).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
                if (c.cooldown() != 0) {
                    list.add(new CoolDown(item, new StopWatch().setMs(-c.cooldown() * 50L), new Decelerate().setMs(150).setValue(1.0F)));
                }
            }
            case PlayerRespawnS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer fontCoolDown = Fonts.getSize(13, Fonts.Type.DEFAULT);
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

        font.drawString(matrix, "CoolDowns", getX() + 7, getY() + 6.5f, ColorAssist.getText());
        icon.drawString(matrix, "\uF017", getX() + getWidth() - 16f, getY() + 6.5f, new Color(225, 225, 255, 255).getRGB());

        float centerX = getX() + getWidth() / 2.0F;
        int offset = 23;
        int maxWidth = 110;

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            Item item = EXAMPLE_ITEMS[currentItemIndex];
            java.lang.String name = "Example CoolDowns";
            java.lang.String duration = "**:**";
            int textColor = ColorAssist.getText();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int color = new Color(225, 225, 255, 255).getRGB();
            float durationWidth = fontCoolDown.getStringWidth(duration);
            float durationBoxWidth = durationWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                Render2D.drawStack(matrix, item.getDefaultStack(), getX() + 4.5f, centerY - 3, false, 0.5F);
                rectangle.render(ShapeProperties.create(matrix, getX() + 15, centerY - 1, 0.5F, 6).color(ColorAssist.getOutline(1, 0.5F)).build());
                fontCoolDown.drawString(matrix, name, getX() + 19, centerY + 1, colorWithAlpha);
                fontCoolDown.drawString(matrix, duration, getX() + getWidth() - durationWidth - 8, centerY + 1, color);
            });
            int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        } else {
            for (CoolDown coolDown : list) {
                float animation = coolDown.anim.getOutput().floatValue();
                float centerY = getY() + offset;
                long elapsedTime = coolDown.time.elapsedTime();
                int time = 0;
                if (elapsedTime >= -2147483648L && elapsedTime <= 2147483647L) {
                    time = (int) (-elapsedTime / 1000);
                } else {
                    time = elapsedTime < 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                }
                java.lang.String name = coolDown.item.getDefaultStack().getName().getString();
                java.lang.String duration = StringHelper.getDuration(time);
                int textColor = ColorAssist.getText();
                int textAlpha = 255;
                int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
                int color = new Color(225, 225, 255, 255).getRGB();
                float durationWidth = fontCoolDown.getStringWidth(duration);
                float durationBoxWidth = durationWidth + 6;
                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    Render2D.drawStack(matrix, coolDown.item.getDefaultStack(), getX() + 4.5f, centerY - 3, false, 0.5F);
                    rectangle.render(ShapeProperties.create(matrix, getX() + 16, centerY - 1, 0.5F, 6).color(ColorAssist.getOutline(1, 0.5F)).build());
                    fontCoolDown.drawString(matrix, name, getX() + 20, centerY + 1, colorWithAlpha);
                    fontCoolDown.drawString(matrix, duration, getX() + getWidth() - durationWidth - 8, centerY + 1, color);
                });
                int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
                maxWidth = Math.max(width, maxWidth);
                offset += (int) (11 * animation);
            }
        }
        setWidth(maxWidth + 10);
        setHeight(offset);
    }

    public record CoolDown(Item item, StopWatch time, Animation anim) {}
}