package lol.zyrex.display.screens.clickgui.components.implement.module;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.setting.SettingComponentAdder;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.display.screens.clickgui.components.AbstractComponent;
import lol.zyrex.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.common.animation.Animation;
import lol.zyrex.common.animation.Direction;
import lol.zyrex.common.animation.implement.Decelerate;
import lol.zyrex.Zyrex;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static lol.zyrex.utils.display.font.Fonts.Type.*;

@Getter
public class ModuleComponent extends AbstractComponent {
    private final List<AbstractSettingComponent> components = new ArrayList<>();
    private final Module module;
    private boolean expanded;
    private final Animation heightAnimation = new Decelerate().setMs(250).setValue(0);

    public ModuleComponent(Module module) {
        this.module = module;
        new SettingComponentAdder().addSettingComponent(module.settings(), components);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float baseH = 16;

        boolean active = module.isState();
        Color textColor = active ? new Color(225, 225, 225, 255) : new Color(140, 140, 140, 255);

        float nameH = Fonts.getSize(14, DEFAULT).getStringHeight(module.getVisibleName());
        float nameBaseline = y + baseH / 2f + 3 - nameH / 4f;

        float textOffsetX = x + 4;
        if (expanded) {
            float chevronH = Fonts.getSize(14, FA_SOLID).getStringHeight("\uF053");
            float chevronBaseline = y + baseH / 2f + 3 - chevronH / 4f;
            Fonts.getSize(14, FA_SOLID).drawString(context.getMatrices(), "\uF053", x + 4, chevronBaseline, new Color(225, 225, 225, 180).getRGB());
            textOffsetX = x + 4 + Fonts.getSize(14, FA_SOLID).getStringWidth("\uF053") + 4;
        }

        Fonts.getSize(14, DEFAULT).drawString(context.getMatrices(), module.getVisibleName(), textOffsetX, nameBaseline, textColor.getRGB());

        if (active) {
            float checkH = Fonts.getSize(14, FA_SOLID).getStringHeight("\uF00C");
            float checkBaseline = y + baseH / 2f + 3 - checkH / 4f;
            float checkX = x + width - 4 - Fonts.getSize(14, FA_SOLID).getStringWidth("\uF00C");
            Fonts.getSize(14, FA_SOLID).drawString(context.getMatrices(), "\uF00C", checkX, checkBaseline, new Color(225, 225, 225, 225).getRGB());
        }

        float settingsH = 0;
        for (AbstractSettingComponent s : components) {
            if (s.getSetting().getVisible() != null && !s.getSetting().getVisible().get()) continue;
            settingsH += s.height;
        }

        if (expanded) {
            heightAnimation.setDirection(Direction.FORWARDS);
            heightAnimation.setValue(settingsH);
        } else {
            heightAnimation.setDirection(Direction.BACKWARDS);
        }

        float animatedH = heightAnimation.getOutput().floatValue();
        this.height = (int) (baseH + animatedH);

        if (settingsH > 0) {
            Zyrex.getInstance().getScissorManager().push(
                    context.getMatrices().peek().getPositionMatrix(),
                    x, y + baseH, width, animatedH
            );

            float settingY = y + baseH;
            for (AbstractSettingComponent s : components) {
                if (s.getSetting().getVisible() != null && !s.getSetting().getVisible().get()) continue;
                s.x = x + 4;
                s.y = settingY;
                s.width = width - 8;
                s.render(context, mouseX, mouseY, delta);
                settingY += s.height;
            }

            Zyrex.getInstance().getScissorManager().pop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && Calculate.isHovered(mouseX, mouseY, x, y, width, 16) && !module.settings().isEmpty()) {
            expanded = !expanded;
            return true;
        }

        if (expanded) {
            for (AbstractSettingComponent s : components) {
                if (s.getSetting().getVisible() != null && !s.getSetting().getVisible().get()) continue;
                if (Calculate.isHovered(mouseX, mouseY, s.x, s.y, s.width, s.height)) {
                    s.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
        }

        if (button == 0 && Calculate.isHovered(mouseX, mouseY, x, y, width, 16)) {
            module.switchState();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return Calculate.isHovered(mouseX, mouseY, x, y, width, height);
    }

    @Override
    public void tick() {
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        components.forEach(s -> s.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!expanded) return super.mouseScrolled(mouseX, mouseY, amount);
        components.forEach(s -> s.mouseScrolled(mouseX, mouseY, amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (expanded) {
            components.forEach(s -> s.keyPressed(keyCode, scanCode, modifiers));
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (expanded) {
            components.forEach(s -> s.charTyped(chr, modifiers));
        }
        return super.charTyped(chr, modifiers);
    }

    public int getComponentHeight() {
        return Math.max(16, (int) height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleComponent that = (ModuleComponent) o;
        return module.equals(that.module);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module);
    }
}
