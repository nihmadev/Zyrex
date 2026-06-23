package lol.zyrex.mixins.player.input;

import lol.zyrex.commands.defaults.BindCommand;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import lol.zyrex.utils.client.managers.event.EventManager;
import lol.zyrex.events.keyboard.KeyEvent;
import lol.zyrex.display.screens.clickgui.MenuScreen;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Final
    @Shadow
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {

        if (key != GLFW.GLFW_KEY_UNKNOWN && window == client.getWindow().getHandle()) {
            if (action == 0 && key == BindCommand.ClickGuiManager.getClickGuiKey()) {
                if (client.currentScreen == null) {
                    MenuScreen.INSTANCE.openGui();
                } else if (client.currentScreen == MenuScreen.INSTANCE) {
                    MenuScreen.INSTANCE.closeGui();
                }
            }

            EventManager.callEvent(new KeyEvent(client.currentScreen, InputUtil.Type.KEYSYM, key, action));
        }
    }
}