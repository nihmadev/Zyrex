package lol.zyrex.mixins.player.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.utils.display.stencil.Stencil;
import lol.zyrex.utils.features.skeleton.BoneData;
import lol.zyrex.utils.features.skeleton.SkeletonBoneStorage;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.model.ModelPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL11;
import lol.zyrex.utils.client.managers.event.EventManager;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.events.render.EntityColorEvent;
import lol.zyrex.utils.features.chams.ChamsGlowLayer;
import lol.zyrex.utils.features.chams.ChamsTracker;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.Optional;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin implements QuickImports {
    @Shadow @Nullable protected abstract RenderLayer getRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline);

    @Unique
    private boolean chamsActive;
    @Unique
    private boolean chamsTranslucent;
    @Unique
    private boolean chamsNoDepth;
    @Unique
    private boolean chamsOutline;
    @Unique
    private boolean chamsGlow;
    @Unique
    private int chamsColor;
    @Unique
    private LivingEntity currentEntity;
    @Unique
    private VertexConsumerProvider vertexConsumerProvider;
    @Unique
    private Identifier glowTextureId;

    @Unique
    private void ensureGlowTexture() {
        if (glowTextureId == null) {
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
            image.setColorArgb(0, 0, 0xFFFFFFFF);
            image.setColorArgb(1, 0, 0xFFF0F0F0);
            image.setColorArgb(0, 1, 0xFFF0F0F0);
            image.setColorArgb(1, 1, 0xFFFFFFFF);
            glowTextureId = Identifier.of("chams_glow");
            mc.getTextureManager().registerTexture(glowTextureId, new NativeImageBackedTexture(image));
        }
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("HEAD"))
    private void trackEntity(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        currentEntity = entity;
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void chamsBeforeRender(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        vertexConsumerProvider = vertexConsumers;
        EntityColorEvent event = new EntityColorEvent(-1, state.invisibleToPlayer, currentEntity);
        EventManager.callEvent(event);
        chamsActive = event.isCancelled() || event.getColor() != -1 || event.isOutline() || event.isGlow();
        chamsTranslucent = event.isCancelled();
        chamsNoDepth = !event.isDepthTest();
        chamsOutline = event.isOutline();
        chamsGlow = event.isGlow();
        chamsColor = event.getColor();

        if (chamsNoDepth || chamsOutline || chamsGlow) {
            if (vertexConsumers instanceof VertexConsumerProvider.Immediate imm) {
                imm.draw();
            }
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void chamsAfterRender(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (chamsActive && !chamsOutline) {
            ChamsTracker.noDepth = chamsNoDepth;
            if (vertexConsumers instanceof VertexConsumerProvider.Immediate imm) {
                imm.draw();
            }
            ChamsTracker.noDepth = false;
        }
    }

    @Unique
    private void extractBonePositions(EntityModel<?> model, MatrixStack matrices) {
        if (currentEntity == null) return;

        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        String[] names = { "head", "body", "right_arm", "left_arm", "right_leg", "left_leg" };

        for (String name : names) {
            Optional<ModelPart> optional = model.getPart(name);
            if (optional.isPresent()) {
                ModelPart part = optional.get();
                Vector4f pos = new Vector4f(part.pivotX / 16f, part.pivotY / 16f, part.pivotZ / 16f, 1f);
                mat.transform(pos);
                SkeletonBoneStorage.store(
                        currentEntity.getUuid(),
                        name,
                        new BoneData(
                                new Vec3d(pos.x() + cameraPos.x, pos.y() + cameraPos.y, pos.z() + cameraPos.z),
                                part.pitch,
                                part.yaw,
                                part.roll
                        )
                );
            }
        }
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        if (chamsActive && chamsGlow) {
            ensureGlowTexture();
            return ChamsGlowLayer.getLayer(glowTextureId);
        }
        if (chamsActive && chamsOutline) {
            return this.getRenderLayer(state, showBody, translucent, showOutline);
        }
        if (chamsActive && chamsTranslucent && !translucent) translucent = true;
        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int l, @Local(ordinal = 0, argsOnly = true) LivingEntityRenderState renderState) {
        if (!chamsActive) {
            instance.render(matrixStack, vertexConsumer, i, j, l);
        } else if (chamsOutline) {
            Stencil.push();
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);

            instance.render(matrixStack, vertexConsumer, i, j, -1);

            GL11.glDisable(GL11.GL_CULL_FACE);

            ChamsTracker.noDepth = chamsNoDepth;
            if (vertexConsumerProvider instanceof VertexConsumerProvider.Immediate imm) {
                imm.draw();
            }
            ChamsTracker.noDepth = false;

            GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

            ensureGlowTexture();
            RenderLayer outlineLayer = ChamsGlowLayer.getLayer(glowTextureId);
            VertexConsumer outlineConsumer = vertexConsumerProvider.getBuffer(outlineLayer);
            int baseColor = chamsColor & 0x00FFFFFF;

            matrixStack.push();
            matrixStack.scale(1.08f, 1.08f, 1.08f);
            instance.render(matrixStack, outlineConsumer, 0xF000F0, j, baseColor | (0x1E << 24));
            matrixStack.pop();

            matrixStack.push();
            matrixStack.scale(1.055f, 1.055f, 1.055f);
            instance.render(matrixStack, outlineConsumer, 0xF000F0, j, baseColor | (0x47 << 24));
            matrixStack.pop();

            matrixStack.push();
            matrixStack.scale(1.03f, 1.03f, 1.03f);
            instance.render(matrixStack, outlineConsumer, 0xF000F0, j, baseColor | (0x8C << 24));
            matrixStack.pop();

            matrixStack.push();
            matrixStack.scale(1.01f, 1.01f, 1.01f);
            instance.render(matrixStack, outlineConsumer, 0xF000F0, j, baseColor | (0xD9 << 24));
            matrixStack.pop();

            GL11.glDepthMask(false);
            ChamsTracker.noDepth = chamsNoDepth;
            if (vertexConsumerProvider instanceof VertexConsumerProvider.Immediate imm) {
                imm.draw();
            }
            ChamsTracker.noDepth = false;
            GL11.glDepthMask(true);

            Stencil.pop();
        } else {
            instance.render(matrixStack, vertexConsumer, 0xF000F0, j, chamsColor);
        }

        if (currentEntity != null) {
            extractBonePositions(instance, matrixStack);
        }
    }

    @ModifyExpressionValue(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F")
    )
    private float lerpAngleDegreesHook(
            float original,
            @Local(ordinal = 0, argsOnly = true) LivingEntity entity,
            @Local(ordinal = 0, argsOnly = true) float delta
    ) {
        TurnsConnection controller = TurnsConnection.INSTANCE;
        Aura aura = Aura.getInstance();

        if (entity.equals(mc.player) && controller.getPreviousRotation().getYaw() != mc.player.getYaw() && controller.getFakeRotation().getYaw() != mc.player.getYaw() && !(mc.currentScreen instanceof HandledScreen)) {
            boolean isLony = Aura.fakeRotate;

            float prevYaw = isLony ? controller.getFakeRotation().getYaw() : controller.getPreviousRotation().getYaw();
            float currentYaw = isLony ? controller.getFakeRotation().getYaw() : controller.getRotation().getYaw();

            if (Aura.getInstance().getTarget() == null) {
                prevYaw = controller.getPreviousRotation().getYaw();
                currentYaw = controller.getRotation().getYaw();
            }

            return MathHelper.lerp(delta, prevYaw, currentYaw);
        }

        return original;
    }

    @ModifyExpressionValue(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F")
    )
    private float getLerpedPitchHook(
            float original,
            @Local(ordinal = 0, argsOnly = true) LivingEntity entity,
            @Local(ordinal = 0, argsOnly = true) float delta
    ) {
        TurnsConnection controller = TurnsConnection.INSTANCE;
        Aura aura = Aura.getInstance();

        if (entity.equals(mc.player) && controller.getPreviousRotation().getPitch() != mc.player.getPitch() && controller.getFakeRotation().getPitch() != mc.player.getPitch() && !(mc.currentScreen instanceof HandledScreen)) {
            boolean isLony = Aura.fakeRotate;

            float prevPitch = isLony ? controller.getFakeRotation().getPitch() : controller.getPreviousRotation().getPitch();
            float currentPitch = isLony ? controller.getFakeRotation().getPitch() : controller.getRotation().getPitch();

            if (Aura.getInstance().getTarget() == null ) {
                prevPitch = controller.getPreviousRotation().getPitch();
                currentPitch = controller.getRotation().getPitch();
            }


            return MathHelper.lerp(delta, prevPitch, currentPitch);
        }

        return original;
    }
}
