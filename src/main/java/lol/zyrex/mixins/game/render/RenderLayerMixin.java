package lol.zyrex.mixins.game.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import lol.zyrex.utils.features.chams.ChamsTracker;

@Mixin(RenderLayer.class)
public class RenderLayerMixin {

    @WrapOperation(
        method = "draw",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferRenderer;drawWithGlobalProgram(Lnet/minecraft/client/render/BuiltBuffer;)V")
    )
    private void wrapDrawWithGlobalProgram(BuiltBuffer buffer, Operation<Void> original) {
        if (ChamsTracker.noDepth) {
            int prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
            original.call(buffer);
            RenderSystem.depthFunc(prevDepthFunc);
        } else {
            original.call(buffer);
        }
    }
}
