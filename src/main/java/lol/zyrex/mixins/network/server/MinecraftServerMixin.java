package lol.zyrex.mixins.network.server;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import lol.zyrex.Zyrex;
import lol.zyrex.utils.client.managers.file.exception.FileProcessingException;
import lol.zyrex.utils.client.logs.Logger;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "shutdown", at = @At("HEAD"))
    public void shutdown(CallbackInfo ci) {
        if (Zyrex.getInstance().isInitialized()) {
            try {
                Zyrex.getInstance().getFileController().saveFiles();
            } catch (FileProcessingException e) {
                Logger.error("Error occurred while saving files: " + e.getMessage());
            }
        }
    }
}
