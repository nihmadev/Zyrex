package lol.zyrex.display.screens.clickgui.components.implement.settings.select;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.display.screens.clickgui.components.AbstractComponent;
import lol.zyrex.utils.math.calc.Calculate;

import java.awt.*;

import static lol.zyrex.utils.display.font.Fonts.Type.DEFAULT;
import static lol.zyrex.utils.display.font.Fonts.Type.FA_SOLID;

public class SelectedButton extends AbstractComponent {
    private final SelectSetting setting;
    private final String text;

    public SelectedButton(SelectSetting setting, String text) {
        this.setting = setting;
        this.text = text;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        boolean selected = setting.isSelected(text);
        boolean hovered = Calculate.isHovered(mouseX, mouseY, x, y, width, height);

        if (hovered) {
            rectangle.render(ShapeProperties.create(matrix, x, y, width, height)
                    .round(4)
                    .color(new Color(58, 58, 60, 80).getRGB()).build());
        }

        FontRenderer textFont = Fonts.getSize(14, DEFAULT);
        FontRenderer checkFont = Fonts.getSize(14, FA_SOLID);
        float textH = textFont.getStringHeight(text);
        float baseline = y + height / 2f + 3 - textH / 4f;

        textFont.drawString(matrix, text, x + 4, baseline, new Color(225, 225, 225, 225).getRGB());

        if (selected) {
            float checkH = checkFont.getStringHeight("\uF00C");
            float checkBaseline = y + height / 2f + 3 - checkH / 4f;
            float checkX = x + width - 4 - checkFont.getStringWidth("\uF00C");
            checkFont.drawString(matrix, "\uF00C", checkX, checkBaseline, new Color(225, 225, 225, 255).getRGB());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            setting.setSelected(text);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}