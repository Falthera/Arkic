package dev.arkic.mixin;

import dev.arkic.sodium.SodiumInterop;
import dev.arkic.renderer.config.ArkicConfig;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionBufferBuilderPool.class)
public class ChunkBuilderMixin {
    
    @Inject(method = "scheduleRenderRebuild", at = @At("HEAD"))
    private void arkir$onScheduleRebuild(CallbackInfo ci) {
        if (ArkicConfig.VERBOSE_LOGGING) {
            System.out.println("[Arkic] Chunk rebuild scheduled");
        }
    }
}
