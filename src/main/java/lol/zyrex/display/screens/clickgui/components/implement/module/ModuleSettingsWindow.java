package lol.zyrex.display.screens.clickgui.components.implement.module;

import lol.zyrex.display.screens.clickgui.MenuScreen;
import lol.zyrex.display.screens.clickgui.components.AbstractComponent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.setting.SettingComponentAdder;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import lol.zyrex.display.screens.clickgui.components.implement.window.AbstractWindow;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.display.scissor.ScissorAssist;
import lol.zyrex.Zyrex;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModuleSettingsWindow extends AbstractWindow {
    private final List<AbstractSettingComponent> components = new ArrayList<>();
    public final Module module;

    public ModuleSettingsWindow(Module module) {
        this.module = module;
        new SettingComponentAdder().addSettingComponent(module.settings(), components);
        draggable(true);
    }

    @Override
    public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        ScissorAssist scissorManager = Zyrex.getInstance().getScissorManager();
        height = window.getScaledHeight() - 40;

        blur.render(ShapeProperties.create(matrix, x, y, width, height).round(8).quality(64)
                .color(new Color(0, 0, 0, 200).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(8)
                .softness(22)
                .thickness(0.5f)
                .outlineColor(new Color(18, 19, 20, 225).getRGB())
                .color(
                        new Color(18, 19, 20, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(18, 19, 20, 175).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, x, y + 19, width, 1f)
                .color(new Color(55, 55, 70, 250).getRGB(), new Color(55, 55, 70, 15).getRGB(), new Color(55, 55, 70, 250).getRGB(), new Color(55, 55, 70, 15).getRGB())
                .build());

        String titleText = module.getVisibleName() + " Settings";
        FontRenderer titleFont = Fonts.getSize(14, Fonts.Type.SEMI);
        FontRenderer closeFont = Fonts.getSize(16, Fonts.Type.FA_SOLID);
        float titleH = titleFont.getStringHeight(titleText);
        float closeH = closeFont.getStringHeight("\uF00D");
        float headerBaseline = y + 9.5f + 3 - titleH / 4f;
        titleFont.drawString(context.getMatrices(), titleText,
                x + 10, headerBaseline, new Color(225, 225, 225, 225).getRGB());

        closeFont.drawString(context.getMatrices(), "\uF00D", x + width - 20, headerBaseline + (titleH - closeH) / 4f, new Color(128, 128, 128).getRGB());

        boolean isLimitedHeight;
        int preTotalHeight = 0;
        float currentY = (float) (y + 19 + smoothedScroll);
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            component.x = x;
            component.y = currentY;
            component.width = width;
            currentY += component.height;
            preTotalHeight += component.height;
        }
        isLimitedHeight = preTotalHeight + 30 > height;
        if (isLimitedHeight) scissorManager.push(matrix.peek().getPositionMatrix(), x, y + 23, width, height - 24);
        int actualTotalHeight = 0;
        for (int i = components.size() - 1; i >= 0; i--) {
            AbstractSettingComponent component = components.get(i);
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            component.render(context, mouseX, mouseY, delta);
            actualTotalHeight += component.height;
        }
        if (isLimitedHeight) scissorManager.pop();
        int maxScroll = (int) Math.max(0, actualTotalHeight - (height - 28));
        scroll = MathHelper.clamp(scroll, -maxScroll, 0);
        smoothedScroll = MathHelper.lerp(0.1F, smoothedScroll, scroll);

        if (isLimitedHeight) {
            float viewableHeight = height - 30;
            float scrollbarHeight = Math.max(20, (viewableHeight / actualTotalHeight) * viewableHeight);
            float scrollPercent = (float) (-smoothedScroll / (float) maxScroll);
            float scrollbarY = y + 30 + (scrollPercent * (viewableHeight - scrollbarHeight));

            float scrollbarX = x + width - 6;
            float scrollbarWidth = 3;

            rectangle.render(ShapeProperties.create(matrix, scrollbarX, y + 30, scrollbarWidth, viewableHeight - 6)
                    .round(1)
                    .color(new Color(100, 100, 100, 60).getRGB())
                    .build());

            rectangle.render(ShapeProperties.create(matrix, scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight - 6)
                    .round(1.5f)
                    .color(new Color(115, 115, 115, 200).getRGB())
                    .build());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (Calculate.isHovered(mouseX, mouseY, x + width - 24, y + 0.5f, 18, 18)) {
                startCloseAnimation();
                return true;
            }
            if (Calculate.isHovered(mouseX, mouseY, x, y, width, 19)) {
                dragging = true;
                dragX = (int) (x - mouseX);
                dragY = (int) (y - mouseY);
                return true;
            }
        }
        boolean isAnyComponentHovered = components.stream().anyMatch(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
        if (isAnyComponentHovered) {
            components.forEach(abstractComponent -> {
                if (abstractComponent.isHover(mouseX, mouseY)) {
                    abstractComponent.mouseClicked(mouseX, mouseY, button);
                }
            });
            return true;
        }
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            if (Calculate.isHovered(mouseX, mouseY, component.x, component.y, component.width, component.height)) {
                component.mouseClicked(mouseX, mouseY, button);
            }
        }
        return true;
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        components.forEach(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
        for (AbstractComponent abstractComponent : components) {
            if (abstractComponent.isHover(mouseX, mouseY)) return true;
        }
        return super.isHovered(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        components.forEach(abstractComponent -> abstractComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        boolean scrolled = getComponentHeight() + 5 > height && Calculate.isHovered(mouseX, mouseY, x, y, width, height);
        if (scrolled) scroll += amount * 20;
        components.forEach(abstractComponent -> abstractComponent.mouseScrolled(mouseX, mouseY, amount));
        return scrolled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        components.forEach(abstractComponent -> abstractComponent.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        components.forEach(abstractComponent -> abstractComponent.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    public int getComponentHeight() {
        float offsetY = 0;
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            offsetY += component.height;
        }
        return (int) (offsetY + 25);
    }
}
