package lol.zyrex.mixins.optimization;

import lol.zyrex.optimization.OptimizationManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Оптимизация памяти - автоматическая очистка
 */
@Mixin(MinecraftClient.class)
public class MemoryOptimizationMixin {
    
    private int tickCounter = 0;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        tickCounter++;
        // Каждые 100 тиков (5 секунд) проверяем память
        if (tickCounter >= 100) {
            tickCounter = 0;
            OptimizationManager.getInstance().tick();
        }
    }
}
