package lol.zyrex.mixins.game.world;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import lol.zyrex.features.impl.render.Atmosphere;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {

    private static final Identifier MOON_TEXTURE = Identifier.of("textures/environment/moon_phases.png");

    @ModifyExpressionValue(method = "renderSun", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/SkyRendering;SUN_TEXTURE:Lnet/minecraft/util/Identifier;"))
    private Identifier onSunTexture(Identifier original) {
        Atmosphere atmosphere = Atmosphere.getInstance();
        if (atmosphere.isState() && atmosphere.presetSetting.getSelected().equals("BloodMoon")) {
            return MOON_TEXTURE;
        }
        return original;
    }
}
