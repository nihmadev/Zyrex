package lol.zyrex.mixins.client.screen.ingame;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import lol.zyrex.features.impl.player.NoFire;

@Mixin(InGameOverlayRenderer.class)
public class NoFireOverlayMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        NoFire noFire = NoFire.getInstance();
        if (noFire.isState()) ci.cancel();
    }
}
