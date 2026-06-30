package dev.arkic.mixin;

import dev.arkic.renderer.ArkicRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    
    @Inject(method = "run", at = @At("RETURN"))
    private void arkir$onGameClose(CallbackInfo ci) {
        ArkicRenderer.shutdown();
    }
}
