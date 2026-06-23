package lol.zyrex.mixins.client.screen.chat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import lol.zyrex.Zyrex;
import lol.zyrex.utils.client.managers.api.draggable.AbstractDraggable;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.features.impl.render.Hud;

import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen implements QuickImports {
    @Unique
    List<AbstractDraggable> draggables = Zyrex.getInstance().getDraggableRepository().draggable();

    protected ChatScreenMixin() {
        super(Text.empty());
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Hud hud = Hud.getInstance();
        draggables.stream().filter(draggable -> draggable.canDraw(hud, draggable) && draggable.isDragging())
                .reduce((first, second) -> second)
                .ifPresent(active -> draggables.forEach(draggable -> {if (active == draggable) draggable.render(context, mouseX, mouseY, delta);}));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        boolean handled = false;
        for (AbstractDraggable d : draggables) {
            if (d.mouseClicked(mouseX, mouseY, button)) {
                handled = true;
            }
        }
        if (handled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggables.forEach(draggable -> draggable.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
