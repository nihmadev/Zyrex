package lol.zyrex.mixins.game.world;

import lol.zyrex.features.impl.render.Atmosphere;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherRendering.class)
public class WeatherRenderingMixin {

    @Inject(method = "renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderPrecipitation(CallbackInfo ci) {
        Atmosphere atmosphere = Atmosphere.getInstance();
        if (atmosphere.isState() && atmosphere.rainStrength.getValue() >= 0.0F) {
            ci.cancel();
        }
    }

    @Inject(method = "addParticlesAndSound", at = @At("HEAD"), cancellable = true)
    private void onAddParticlesAndSound(ClientWorld world, Camera camera, int ticks, ParticlesMode particlesMode, CallbackInfo ci) {
        Atmosphere atmosphere = Atmosphere.getInstance();
        if (atmosphere.isState() && atmosphere.rainStrength.getValue() >= 0.0F) {
            ci.cancel();
        }
    }
}
