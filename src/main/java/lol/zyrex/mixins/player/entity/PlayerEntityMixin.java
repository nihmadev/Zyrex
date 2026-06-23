package lol.zyrex.mixins.player.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import lol.zyrex.utils.client.managers.event.EventManager;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.events.block.PushEvent;
import lol.zyrex.events.player.KeepSprintEvent;
import lol.zyrex.events.player.PlayerTravelEvent;
import lol.zyrex.events.player.SwimmingEvent;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements QuickImports {

    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    public void isPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
        PushEvent event = new PushEvent(PushEvent.Type.WATER);
        EventManager.callEvent(event);
        if (event.isCancelled()) cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V", shift = At.Shift.AFTER))
    public void attackHook(CallbackInfo callbackInfo) {
        EventManager.callEvent(new KeepSprintEvent());
    }

    @ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookFixRotation(float original) {
        return TurnsConnection.INSTANCE.getMoveRotation().getYaw();
    }

    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d travelHook(Vec3d vec3d) {
        SwimmingEvent event = new SwimmingEvent(vec3d);
        EventManager.callEvent(event);
        return event.getVector();
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
        if (mc.player == null)
            return;

        PlayerTravelEvent event = new PlayerTravelEvent(movementInput, true);
        EventManager.callEvent(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
    private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
        if (mc.player == null)
            return;

        PlayerTravelEvent event = new PlayerTravelEvent(movementInput, false);
        EventManager.callEvent(event);
        if (event.isCancelled()) {

        }
    }

}