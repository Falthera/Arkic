package dev.arkic.renderer.backend;

import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.gl.GlCapabilities;
import dev.arkic.renderer.upload.ChunkUploadManager;
import dev.arkic.diagnostics.ArkicBenchmark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SodiumFallback {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Fallback");
    
    public static boolean isFallbackActive() {
        return !dev.arkic.renderer.ArkicRenderer.isActive();
    }
    
    public static void logFallbackMessage() {
        LOGGER.warn("Active backend: Sodium fallback — Arkic GPU pipeline unavailable");
    }
    
    public static void renderFallbackFrame(float tickDelta) {
        if (isFallbackActive()) {
            logFallbackMessage();
        }
    }
}
