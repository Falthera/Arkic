package dev.arkic.mixin;

import dev.arkic.renderer.ArkicRenderer;
import dev.arkic.sodium.SodiumInterop;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.WorldRenderer.class)
public class WorldRendererMixin {
    
    @Inject(method = "render", at = @At("HEAD"))
    private void arkir$onWorldRenderHead(
        MatrixStack matrices, float tickDelta, long limitTime, 
        boolean renderBlockOutline, Camera camera, 
        GameRenderer gameRenderer, MatrixStack matrixStack, 
        boolean renderShadows, CallbackInfo ci) {
        if (ArkicRenderer.isActive()) {
            ArkicRenderer.beginFrame();
        }
    }
    
    @Inject(method = "render", at = @At("RETURN"))
    private void arkir$onWorldRenderTail(
        MatrixStack matrices, float tickDelta, long limitTime, 
        boolean renderBlockOutline, Camera camera, 
        GameRenderer gameRenderer, MatrixStack matrixStack, 
        boolean renderShadows, CallbackInfo ci) {
        if (ArkicRenderer.isActive()) {
            ArkicRenderer.endFrame();
        }
    }
}
