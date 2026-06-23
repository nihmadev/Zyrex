package lol.zyrex.display.screens.clickgui.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

import lol.zyrex.features.module.setting.implement.BindSetting;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.client.chat.StringHelper;

import static lol.zyrex.utils.display.font.Fonts.Type.DEFAULT;
import static lol.zyrex.utils.display.font.Fonts.Type.SEMI;

public class BindComponent extends AbstractSettingComponent {
    private final BindSetting setting;
    private boolean binding;

    public BindComponent(BindSetting setting) {
        super(setting);
        this.setting = setting;
        this.height = 16;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        java.lang.String bindName = StringHelper.getBindName(setting.getKey());
        java.lang.String name = binding ? "..." : bindName;
        float stringWidth = Fonts.getSize(13, SEMI).getStringWidth(name) - 2;

        height = 16;

        int outlineColor = ColorAssist.getOutline();
        int fillColor = binding ? ColorAssist.getGuiRectColor(1) : 0;

        rectangle.render(ShapeProperties.create(matrix, x + width - stringWidth - 19, y + 4.5f, stringWidth + 10, 11.5f)
                .round(3).softness(1).thickness(2).outlineColor(outlineColor)
                .color(fillColor, fillColor, fillColor, fillColor)
                .build());
        int bindingColor = ColorHelper.getArgb(255, 135, 136, 148);

        Fonts.getSize(13, SEMI).drawString(matrix, name, x + width - 14 - stringWidth - 1, y + 9.5f, bindingColor);

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
