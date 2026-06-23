package lol.zyrex.mixins.game.world;

import lol.zyrex.features.impl.render.Atmosphere;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldWeatherMixin {

    @Inject(method = "getRainGradient(F)F", at = @At("RETURN"), cancellable = true)
    private void onGetRainGradient(float tickDelta, CallbackInfoReturnable<Float> cir) {
        Atmosphere atmosphere = Atmosphere.getInstance();
        if (atmosphere.isState()) {
            if (atmosphere.rainStrength.getValue() >= 0.0F) {
                cir.setReturnValue(atmosphere.rainStrength.getValue());
            }
        } else {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderGradient(F)F", at = @At("RETURN"), cancellable = true)
    private void onGetThunderGradient(float tickDelta, CallbackInfoReturnable<Float> cir) {
        Atmosphere atmosphere = Atmosphere.getInstance();
        if (atmosphere.isState()) {
            if (atmosphere.thunderStrength.getValue() >= 0.0F) {
                cir.setReturnValue(atmosphere.thunderStrength.getValue());
            }
        } else {
            cir.setReturnValue(0.0F);
        }
    }
}
