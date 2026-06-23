package lol.zyrex.mixins.game.render;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import lol.zyrex.features.impl.player.NoShaking;

@Mixin(GameRenderer.class)
public class NoShakingGameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoShaking noShaking = NoShaking.getInstance();
        if (noShaking.isState() && noShaking.modeSetting.isSelected("Hurt Tilt")) ci.cancel();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoShaking noShaking = NoShaking.getInstance();
        if (noShaking.isState() && noShaking.modeSetting.isSelected("View Bob")) ci.cancel();
    }
}
