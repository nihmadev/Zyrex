package lol.zyrex.display.screens.clickgui.components.implement.settings;

import net.minecraft.client.gui.DrawContext;

import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.theme.UITheme;
import lol.zyrex.display.screens.clickgui.components.implement.other.CheckComponent;

import static lol.zyrex.utils.display.font.Fonts.Type.DEFAULT;

public class CheckboxComponent extends AbstractSettingComponent {
    private final CheckComponent checkComponent = new CheckComponent();
    private final BooleanSetting setting;

    public CheckboxComponent(BooleanSetting setting) {
        super(setting);
        this.setting = setting;
        this.height = 16;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        height = 16;

        FontRenderer textFont = Fonts.getSize(14, DEFAULT);
        String name = setting.getName();
        float textH = textFont.getStringHeight(name);
        float textBaseline = y + height / 2f + 3 - textH / 4f;
        float textX = x + 4;
        textFont.drawString(context.getMatrices(), name, textX, textBaseline, UITheme.textColor().getRGB());

        ((CheckComponent) checkComponent.position(x + width - 19, y + 6.5f))
                .setRunnable(() -> setting.setValue(!setting.isValue()))
                .setState(setting.isValue())
                .render(context, mouseX, mouseY, delta);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        checkComponent.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
