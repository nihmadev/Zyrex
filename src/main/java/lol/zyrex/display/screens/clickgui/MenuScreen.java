package lol.zyrex.display.screens.clickgui;

import lombok.Getter;
import lol.zyrex.features.impl.misc.SelfDestruct;
import lol.zyrex.features.module.ModuleCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import lol.zyrex.common.animation.Easy.Direction;
import lol.zyrex.common.animation.Easy.EaseBackIn;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.client.sound.SoundManager;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.display.screens.clickgui.components.AbstractComponent;
import lol.zyrex.display.screens.clickgui.components.implement.other.CategoryContainerComponent;
import lol.zyrex.display.screens.clickgui.components.implement.other.SearchComponent;
import lol.zyrex.display.screens.clickgui.components.implement.settings.TextComponent;
import lol.zyrex.utils.math.calc.Calculate;

import java.util.ArrayList;
import java.util.List;

import static lol.zyrex.common.animation.Easy.Direction.BACKWARDS;
import static lol.zyrex.common.animation.Easy.Direction.FORWARDS;

public class MenuScreen extends Screen implements QuickImports {
    public static MenuScreen INSTANCE = new MenuScreen();
    private final List<AbstractComponent> components = new ArrayList<>();
    private final CategoryContainerComponent categoryContainerComponent = new CategoryContainerComponent();
    private final SearchComponent searchComponent = new SearchComponent();

    @Getter
    private ModuleCategory category = ModuleCategory.COMBAT;
    public final EaseBackIn animation = new EaseBackIn(325, 1f, 1.5f);
    public int x, y, width, height;

    public void initialize() {
        animation.setDirection(FORWARDS);
        categoryContainerComponent.initializeCategoryComponents();
        components.add(categoryContainerComponent);
    }

    public MenuScreen() {
        super(Text.of("MenuScreen"));
        initialize();
    }

    @Override
    public void tick() {
        close();
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Calculate.scale(context.getMatrices(), window.getScaledWidth() / 2f, window.getScaledHeight() / 2f, getScaleAnimation(), () -> {
            components.forEach(component -> component.render(context, mouseX, mouseY, delta));
            windowManager.render(context, mouseX, mouseY, delta);
        });
        super.render(context, mouseX, mouseY, delta);

        String description = categoryContainerComponent.getHoveredModuleDescription(mouseX, mouseY);
        if (description != null) {
            FontRenderer font = Fonts.getSize(14);
            float textWidth = font.getStringWidth(description);
            float x = (window.getScaledWidth() - textWidth) / 2f;
            float panelHeight = 210;
            float startY = (window.getScaledHeight() - panelHeight) / 2;
            float descY = Math.min(startY + panelHeight + 8, window.getScaledHeight() - 55);
            font.drawString(context.getMatrices(), description, x, descY, 0xFFD4D6E1);
        }
    }

    public void openGui() {
        if (SelfDestruct.unhooked) return;

        animation.setDirection(Direction.FORWARDS);
        animation.reset();
        mc.setScreen(this);
        SoundManager.playSound(SoundManager.OPEN_GUI);
    }

    public void closeGui() {
        if (SelfDestruct.unhooked) return;

        SoundManager.playSound(SoundManager.CLOSE_GUI);
        animation.setDirection(BACKWARDS);
    }

    public float getScaleAnimation() {
        return (float) animation.getOutput();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean windowHandled = windowManager.mouseClicked(mouseX, mouseY, button);
        if (!windowHandled) {
            for (AbstractComponent component : components) {
                component.mouseClicked(mouseX, mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (AbstractComponent component : components) {
            component.mouseReleased(mouseX, mouseY, button);
        }
        windowManager.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        boolean windowHandled = windowManager.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        if (!windowHandled) {
            for (AbstractComponent component : components) {
                component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        boolean windowHandled = windowManager.mouseScrolled(mouseX, mouseY, vertical);
        if (!windowHandled) {
            for (AbstractComponent component : components) {
                component.mouseScrolled(mouseX, mouseY, vertical);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && shouldCloseOnEsc()) {
            SoundManager.playSound(SoundManager.CLOSE_GUI);
            animation.setDirection(BACKWARDS);
            return true;
        }
        if (!windowManager.keyPressed(keyCode, scanCode, modifiers)) {
            for (AbstractComponent component : components) {
                component.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!windowManager.charTyped(chr, modifiers)) {
            for (AbstractComponent component : components) {
                component.charTyped(chr, modifiers);
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (animation.finished(BACKWARDS)) {
            TextComponent.typing = false;
            super.close();
        }
    }
}
