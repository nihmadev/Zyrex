package lol.zyrex.display.screens.clickgui.components.implement.settings.select;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.common.animation.Animation;
import lol.zyrex.common.animation.Direction;
import lol.zyrex.common.animation.implement.Decelerate;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.math.calc.Calculate;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static lol.zyrex.utils.display.font.Fonts.Type.DEFAULT;

public class SelectComponent extends AbstractSettingComponent {
    private final List<SelectedButton> selectedButtons = new ArrayList<>();
    private final SelectSetting setting;
    private boolean open;
    private final Animation heightAnimation = new Decelerate().setMs(200).setValue(0);

    public SelectComponent(SelectSetting setting) {
        super(setting);
        this.setting = setting;
        this.height = 16;
        heightAnimation.setDirection(Direction.BACKWARDS);
        for (String s : setting.getList()) {
            selectedButtons.add(new SelectedButton(setting, s));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        float baseHeight = 16;
        float listHeight = setting.getList().size() * 16;

        if (open) {
            heightAnimation.setDirection(Direction.FORWARDS);
            heightAnimation.setValue(listHeight);
        } else {
            heightAnimation.setDirection(Direction.BACKWARDS);
        }

        float animatedHeight = heightAnimation.getOutput().floatValue();
        this.height = (int) (baseHeight + animatedHeight);

        FontRenderer textFont = Fonts.getSize(14, DEFAULT);
        String name = setting.getName();
        float textH = textFont.getStringHeight(name);
        float textBaseline = y + baseHeight / 2f + 3 - textH / 4f;
        textFont.drawString(matrices, name, x + 4, textBaseline, 0xFFD4D6E1);

        if (!heightAnimation.isFinished(Direction.BACKWARDS)) {
            rectangle.render(ShapeProperties.create(matrices, x + 4, y + baseHeight, width - 8, animatedHeight)
                    .round(4).thickness(0.5f).outlineColor(new Color(18, 19, 20, 225).getRGB())
                    .color(ColorAssist.getClientColor(140)).build());

            float optionY = y + baseHeight;
            for (SelectedButton button : selectedButtons) {
                button.x = x + 4;
                button.y = optionY;
                button.width = width - 8;
                button.height = 16;
                if (optionY - (y + baseHeight) < animatedHeight) {
                    button.render(context, mouseX, mouseY, delta);
                }
                optionY += 16;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (Calculate.isHovered(mouseX, mouseY, x, y, width, 16)) {
                open = !open;
                return true;
            }
            if (open) {
                for (SelectedButton selectedButton : selectedButtons) {
                    if (selectedButton.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) return true;
        if (open) {
            for (SelectedButton button : selectedButtons) {
                if (Calculate.isHovered(mouseX, mouseY, button.x, button.y, button.width, button.height)) return true;
            }
        }
        return false;
    }
}