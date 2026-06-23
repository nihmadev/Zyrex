package lol.zyrex.display.screens.clickgui.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

import lol.zyrex.features.module.setting.implement.BindSetting;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.client.chat.StringHelper;

import java.awt.*;

import static lol.zyrex.utils.display.font.Fonts.Type.DEFAULT;
import static lol.zyrex.utils.display.font.Fonts.Type.SEMI;

public class IconBindComponent extends AbstractSettingComponent {
    private final BindSetting setting;
    private boolean binding;

    public IconBindComponent(BindSetting setting) {
        super(setting);
        this.setting = setting;
        this.height = 20;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        java.lang.String bindName = StringHelper.getBindName(setting.getKey());
        java.lang.String name = binding ? "(" + bindName + ") ..." : bindName;
        float stringWidth = Fonts.getSize(13, SEMI).getStringWidth(name) - 2;

        height = 20;

        Color outlineColor = binding ? new Color(255, 200, 50, 255) : new Color(200, 200, 200, 255);
        Color fillColor = binding ? new Color(61, 67, 71, 160) : new Color(61, 67, 71, 80);

        rectangle.render(ShapeProperties.create(matrix, x + width - stringWidth - 17, y + 5.5f, stringWidth + 10, 12)
                .round(3f)
                .outlineColor(outlineColor.getRGB())
                .color(
                        fillColor.getRGB(),
                        fillColor.getRGB(),
                        fillColor.getRGB(),
                        fillColor.getRGB())
                .build());
        int bindingColor = ColorHelper.getArgb(255, 135, 136, 148);

        Fonts.getSize(13, SEMI).drawString(matrix, name, x + width - 12 - stringWidth - 1, y + 11, bindingColor);

        FontRenderer textFont = Fonts.getSize(14, DEFAULT);
        String nameText = setting.getName();
        float textH = textFont.getStringHeight(nameText);
        float textBaseline = y + height / 2f + 3 - textH / 4f;
        float textX = x + 4;
        textFont.drawString(context.getMatrices(), nameText, textX, textBaseline, 0xFFD4D6E1);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) {
                binding = !binding;
            } else {
                binding = false;
            }
        }

        if (binding && button > 1) {
            setting.setKey(button);
            binding = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode;
        if (binding) {
            setting.setKey(key);
            binding = false;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
