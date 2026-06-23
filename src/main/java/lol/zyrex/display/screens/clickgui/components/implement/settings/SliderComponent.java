package lol.zyrex.display.screens.clickgui.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.math.calc.Calculate;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static lol.zyrex.utils.display.font.Fonts.Type.DEFAULT;
import static lol.zyrex.utils.display.font.Fonts.Type.BOLD;

public class SliderComponent extends AbstractSettingComponent {
    private final SliderSettings setting;
    private boolean dragging;
    private double animation;
    private float trackX, trackWidth;

    public SliderComponent(SliderSettings setting) {
        super(setting);
        this.setting = setting;
        this.height = 28;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        height = 28;

        String value = String.valueOf(setting.getValue());
        FontRenderer valueFont = Fonts.getSize(14, BOLD);
        float valueWidth = valueFont.getStringWidth(value);

        trackX = x + 4;
        trackWidth = width - 8 - valueWidth - 8;

        FontRenderer textFont = Fonts.getSize(14, DEFAULT);
        String name = setting.getName();
        float textH = textFont.getStringHeight(name);
        float textBaseline = y + 8 + 3 - textH / 4f;
        textFont.drawString(matrix, name, x + 4, textBaseline, 0xFFD4D6E1);

        valueFont.drawString(matrix, value, x + width - 4 - valueWidth, y + 24, ColorAssist.getText());

        changeValue(getDifference(mouseX, matrix));
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dragging = Calculate.isHovered(mouseX, mouseY, trackX, y + 20, trackWidth, 8) && button == 0;
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private float getDifference(int mouseX, MatrixStack matrix) {
        float sliderStartX = trackX;
        float sliderEndX = trackX + trackWidth;

        float percentValue = trackWidth * (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float difference = MathHelper.clamp(mouseX - sliderStartX, 0, trackWidth);
        float trackY = y + 22;

        animation = Calculate.interpolate(animation, percentValue);

        rectangle.render(ShapeProperties.create(matrix, sliderStartX, trackY, trackWidth, 3).round(1)
                .color(0x2D2E414D).build());

        rectangle.render(ShapeProperties.create(matrix, sliderStartX, trackY, (float) animation, 3).round(1)
                .color(new Color(55, 55, 60, 155).getRGB(), new Color(155, 155, 160, 155).getRGB(), new Color(255, 255, 255, 155).getRGB(), new Color(255, 255, 250, 155).getRGB()).build());

        float v = MathHelper.clamp(sliderStartX + (float) animation, sliderStartX, sliderEndX);
        float thumbCenter = v;
        rectangle.render(ShapeProperties.create(matrix, thumbCenter - 3.5f, trackY - 2.5f, 7, 7)
                .round(3).color(ColorAssist.getMainGuiColor()).build());

        rectangle.render(ShapeProperties.create(matrix, thumbCenter - 3f, trackY - 1.5f, 6, 6)
                .round(3).thickness(2).softness(0).color(new Color(61, 67, 71, 255).getRGB(),
                        new Color(71, 77, 81, 255).getRGB(),
                        new Color(81, 87, 91, 255).getRGB(),
                        new Color(91, 97, 101, 255).getRGB()).build());

        return difference;
    }


    private void changeValue(float difference) {
        BigDecimal bd = BigDecimal.valueOf((difference / trackWidth) * (setting.getMax() - setting.getMin()) + setting.getMin())
                .setScale(2, RoundingMode.HALF_UP);

        if (dragging) {
            float value = difference == 0 ? setting.getMin() : bd.floatValue();
            if (setting.isInteger()) value = (int) value;
            setting.setValue(value);
        }
    }
}