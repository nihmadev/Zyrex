package lol.zyrex.display.screens.clickgui.components.implement.category;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.display.screens.clickgui.components.implement.module.ModuleComponent;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.Zyrex;
import lol.zyrex.display.screens.clickgui.components.AbstractComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CategoryComponent extends AbstractComponent {
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private static final Set<ModuleComponent> globalModuleComponents = new HashSet<>();
    private final ModuleCategory category;
    private float scroll = 0;
    private float smoothedScroll = 0;
    private boolean scrollBarHovered = false;
    private boolean scrollBarDragging = false;
    private float scrollDragStartY = 0;
    private float scrollStartValue = 0;

    private static final Map<ModuleCategory, String> ICONS = new HashMap<>();
    private static final Map<ModuleCategory, String> DISPLAY_NAMES = new HashMap<>();
    static {
        ICONS.put(ModuleCategory.COMBAT, "\uF05B");
        ICONS.put(ModuleCategory.MOVEMENT, "\uF70C");
        ICONS.put(ModuleCategory.RENDER, "\uF06E");
        ICONS.put(ModuleCategory.PLAYER, "\uF007");
        ICONS.put(ModuleCategory.MISC, "\uF013");
        DISPLAY_NAMES.put(ModuleCategory.RENDER, "Visuals");
    }

    private void initializeModules() {
        List<Module> modules = Zyrex.getInstance()
                .getModuleRepository()
                .modules();
        for (Module module : modules) {
            if (!module.getCategory().equals(this.category)) continue;
            ModuleComponent newComponent = new ModuleComponent(module);
            if (globalModuleComponents.add(newComponent)) {
                moduleComponents.add(newComponent);
            }
        }
    }

    public ModuleCategory getCategory() { return category; }
    public List<ModuleComponent> getModuleComponents() { return moduleComponents; }

    public CategoryComponent(ModuleCategory category) {
        this.category = category;
        initializeModules();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (category == ModuleCategory.CONFIGS || category == ModuleCategory.AUTOBUY) return;

        String displayName = DISPLAY_NAMES.getOrDefault(category, category.getReadableName());
        String icon = ICONS.get(category);

        float headerHeight = 22;
        float moduleX = x + 3;
        float moduleW = width - 6;

        float totalModuleHeight = 0;
        for (ModuleComponent mc : moduleComponents) {
            if (mc.getModule().getCategory().equals(category)) {
                mc.width = moduleW;
                totalModuleHeight += mc.getComponentHeight();
            }
        }

        float contentAreaH = height - headerHeight;
        boolean needsScroll = totalModuleHeight > contentAreaH;

        MatrixStack matrix = context.getMatrices();

        blur.render(ShapeProperties.create(matrix, x, y, width, height).round(6).quality(20)
                .color(
                        new Color(10, 10, 12, 180).getRGB(),
                        new Color(0, 0, 0, 225).getRGB(),
                        new Color(6, 6, 8, 195).getRGB(),
                        new Color(0, 0, 0, 215).getRGB())
                .softness(4).thickness(0.25f)
                .outlineColor(new Color(60, 65, 75, 60).getRGB())
                .build());

        float nameH = Fonts.getSize(15, Fonts.Type.SEMI).getStringHeight(displayName);
        float nameBaseline = y + headerHeight / 2f + 3 - nameH / 4f;
        Fonts.getSize(15, Fonts.Type.SEMI).drawString(matrix, displayName, x + 9, nameBaseline, ColorAssist.getText());

        if (icon != null) {
            float iconH = Fonts.getSize(17, Fonts.Type.FA_SOLID).getStringHeight(icon);
            float iconBaseline = y + headerHeight / 2f + 3 - iconH / 4f;
            float iconX = x + width - 9 - Fonts.getSize(17, Fonts.Type.FA_SOLID).getStringWidth(icon);
            Fonts.getSize(17, Fonts.Type.FA_SOLID).drawString(matrix, icon, iconX, iconBaseline, ColorAssist.getText());
        }

        rectangle.render(ShapeProperties.create(matrix, x + 4, y + headerHeight, width - 8, 0.5f)
                .color(
                        new Color(55, 55, 70, 200).getRGB(),
                        new Color(55, 55, 70, 50).getRGB(),
                        new Color(55, 55, 70, 50).getRGB(),
                        new Color(55, 55, 70, 200).getRGB())
                .build());

        float scissorY = y + headerHeight + 2;
        float scissorH = contentAreaH;

        if (needsScroll) {
            Zyrex.getInstance().getScissorManager().push(matrix.peek().getPositionMatrix(), x, scissorY, width, scissorH);
        }

        float maxScroll = Math.max(0, totalModuleHeight - scissorH);
        smoothedScroll = Calculate.interpolateSmooth(3, smoothedScroll, scroll);

        float moduleY = y + headerHeight + 1 + smoothedScroll;
        for (ModuleComponent mc : moduleComponents) {
            if (!mc.getModule().getCategory().equals(category)) continue;
            int mh = mc.getComponentHeight();
            mc.x = moduleX;
            mc.y = moduleY;
            if (mc.y + mh > y + headerHeight && mc.y < y + height) {
                mc.render(context, mouseX, mouseY, delta);
            }
            moduleY += mh;
        }

        if (needsScroll) {
            Zyrex.getInstance().getScissorManager().pop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!Calculate.isHovered(mouseX, mouseY, x, y, width, height)) return super.mouseClicked(mouseX, mouseY, button);
        for (ModuleComponent mc : moduleComponents) {
            if (!mc.getModule().getCategory().equals(category)) continue;
            if (mc.isHover(mouseX, mouseY)) {
                if (mc.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) return true;
        for (ModuleComponent mc : moduleComponents) {
            if (!mc.getModule().getCategory().equals(category)) continue;
            if (mc.isHover(mouseX, mouseY)) return true;
        }
        return super.isHover(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        moduleComponents.forEach(mc -> mc.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) {
            float maxScroll = Math.max(0, getTotalModuleHeight() - (height - 22));
            scroll = MathHelper.clamp((float) (scroll + amount * 20), -maxScroll, 0);
        }
        moduleComponents.forEach(mc -> mc.mouseScrolled(mouseX, mouseY, amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private float getTotalModuleHeight() {
        float h = 0;
        for (ModuleComponent mc : moduleComponents) {
            if (mc.getModule().getCategory().equals(category)) {
                h += mc.getComponentHeight();
            }
        }
        return h;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        moduleComponents.forEach(mc -> mc.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        moduleComponents.forEach(mc -> mc.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }
}