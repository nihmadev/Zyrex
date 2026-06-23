package lol.zyrex.mixins.client;

import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTickCounter.Dynamic.class)
public interface IRenderTickCounter {
    @Accessor("tickTime")
    float getTickTime();

    @Mutable
    @Accessor("tickTime")
    void setTickTime(float tickTime);
}
