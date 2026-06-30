package dev.arkic.compat;

import dev.arkic.renderer.ArkicRenderer;
import dev.arkic.renderer.backend.SodiumFallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArkicSodiumCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Compat");
    
    public static void onChunkRebuildStart(int x, int y, int z) {
        if (ArkicRenderer.isActive()) {
            // In a full implementation, this would trigger an upload via ChunkUploadManager
        } else {
            // Let Sodium handle it
        }
    }
    
    public static void onChunkRebuildComplete(int x, int y, int z) {
        if (ArkicRenderer.isActive()) {
            // Signal chunk data is ready for GPU upload
        }
    }
    
    public static boolean shouldInterceptChunkRender() {
        return ArkicRenderer.isActive() && !SodiumFallback.isFallbackActive();
    }
}
