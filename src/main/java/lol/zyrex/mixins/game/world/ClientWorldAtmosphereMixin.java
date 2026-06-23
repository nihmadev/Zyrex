package lol.zyrex.mixins.game.world;

import lol.zyrex.features.impl.render.Atmosphere;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldAtmosphereMixin {

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void onGetSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        Atmosphere atmosphere = Atmosphere.getInstance();
        if (atmosphere.isState()) {
            int color = atmosphere.getEffectiveSkyColor();
            if (color != 0) {
                cir.setReturnValue(color);
            }
        }
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void onGetCloudsColor(float tickDelta, CallbackInfoReturnable<Integer> cir) {
        Atmosphere atmosphere = Atmosphere.getInstance();
        if (atmosphere.isState()) {
            int color = atmosphere.getEffectiveCloudColor();
            if (color != 0) {
                cir.setReturnValue(color);
            }
        }
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void onGetStarBrightness(float tickDelta, CallbackInfoReturnable<Float> cir) {
        Atmosphere atmosphere = Atmosphere.getInstance();
        if (atmosphere.isState()) {
            float brightness = atmosphere.getEffectiveStarBrightness();
            cir.setReturnValue(cir.getReturnValue() * brightness);
        }
    }

}
