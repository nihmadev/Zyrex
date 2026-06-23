package lol.zyrex.display.screens.clickgui.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;

import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.display.screens.clickgui.components.implement.category.CategoryComponent;
import lol.zyrex.display.screens.clickgui.components.implement.settings.TextComponent;
import lol.zyrex.display.screens.clickgui.components.implement.module.ModuleComponent;
import lol.zyrex.display.screens.clickgui.components.implement.module.ModuleDescriptions;
import lol.zyrex.utils.interactions.inv.InventoryFlowManager;
import lol.zyrex.display.screens.clickgui.components.AbstractComponent;

import java.util.ArrayList;
import java.util.List;

@Setter
@Accessors(chain = true)
public class CategoryContainerComponent extends AbstractComponent {
    private final List<CategoryComponent> categoryComponents = new ArrayList<>();

    public void initializeCategoryComponents() {
        categoryComponents.clear();
        for (ModuleCategory category : ModuleCategory.values()) {
            if (category == ModuleCategory.AUTOBUY) continue;
            categoryComponents.add(new CategoryComponent(category));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        List<CategoryComponent> panels = new ArrayList<>();
        for (CategoryComponent cc : categoryComponents) {
            ModuleCategory cat = cc.getCategory();
            if (cat != ModuleCategory.CONFIGS && cat != ModuleCategory.AUTOBUY) {
                panels.add(cc);
            }
        }

        float panelWidth = 130;
        float panelHeight = 210;
        float gap = 8;
        float totalWidth = panels.size() * panelWidth + (panels.size() - 1) * gap;
        float startX = (window.getScaledWidth() - totalWidth) / 2;
        float startY = (window.getScaledHeight() - panelHeight) / 2;

        panels.get(0).x = startX;
        for (int i = 1; i < panels.size(); i++) {
            panels.get(i).x = panels.get(i - 1).x + panelWidth + gap;
        }

        for (CategoryComponent cc : panels) {
            cc.x = cc.x;
            cc.y = startY;
            cc.width = panelWidth;
            cc.height = panelHeight;
            cc.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void tick() {
        if (TextComponent.typing || SearchComponent.typing) InventoryFlowManager.unPressMoveKeys();
        else InventoryFlowManager.updateMoveKeys();
        categoryComponents.forEach(AbstractComponent::tick);
        super.tick();
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseScrolled(mouseX, mouseY, amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    public String getHoveredModuleDescription(double mouseX, double mouseY) {
        for (CategoryComponent cc : categoryComponents) {
            for (ModuleComponent mc : cc.getModuleComponents()) {
                if (mc.isHover(mouseX, mouseY)) {
                    return ModuleDescriptions.getDescription(mc.getModule());
                }
            }
        }
        return null;
    }
}
