package lol.zyrex.display.screens.clickgui.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import lol.zyrex.features.module.setting.implement.ColorSetting;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.display.screens.clickgui.components.implement.window.AbstractWindow;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.display.screens.clickgui.components.implement.window.implement.settings.color.ColorWindow;

import java.awt.*;

import static lol.zyrex.utils.display.font.Fonts.Type.DEFAULT;

public class ColorComponent extends AbstractSettingComponent {
    private final ColorSetting setting;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.setting = setting;
        this.height = 20;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        height = 20;

        FontRenderer textFont = Fonts.getSize(14, DEFAULT);
        String name = setting.getName();
        float textH = textFont.getStringHeight(name);
        float textBaseline = y + height / 2f + 3 - textH / 4f;
        float textX = x + 4;
        textFont.drawString(matrix, name, textX, textBaseline, 0xFFD4D6E1);

        rectangle.render(ShapeProperties.create(matrix, x + width - 18, y + 10, 7, 7)
                .round(3.5F).color(setting.getColor()).build());

        rectangle.render(ShapeProperties.create(matrix, x + width - 18, y + 10, 7, 7)
                .round(3.5F).thickness(2).softness(1).outlineColor(ColorAssist.getText()).color(0x0FFFFFF).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x + width - 15, y + 10, 7, 7) && button == 0) {
            AbstractWindow existingWindow = null;

            for (AbstractWindow window : windowManager.getWindows()) {
                if (window instanceof ColorWindow) {
                    existingWindow = window;
                    break;
                }
            }

            if (existingWindow != null) {
                windowManager.delete(existingWindow);
            } else {
                AbstractWindow colorWindow = new ColorWindow(setting)
                        .position((int) (mouseX - 110), (int) (mouseY- 20))
                        .size(100, 155)
                        .draggable(true);

                windowManager.add(colorWindow);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
