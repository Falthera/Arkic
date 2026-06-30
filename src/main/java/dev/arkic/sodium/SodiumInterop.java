package dev.arkic.sodium;

import dev.arkic.renderer.ArkicRenderer;
import dev.arkic.renderer.backend.SodiumFallback;
import dev.arkic.renderer.chunk.ChunkHandle;
import dev.arkic.renderer.chunk.ChunkManager;
import dev.arkic.renderer.upload.ChunkUploadManager;
import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.config.ArkicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class SodiumInterop {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/SodiumInterop");
    private static boolean integrationActive = false;
    
    public static void initialize() {
        if (!ArkicRenderer.isActive()) {
            LOGGER.info("Sodium fallback mode — Arkic integrations disabled");
            return;
        }
        
        integrationActive = true;
        LOGGER.info("Sodium interoperability initialized");
    }
    
    public static boolean onBuildChunkMesh(ChunkHandle handle, ByteBuffer vertexData, int vertexCount) {
        if (!integrationActive || SodiumFallback.isFallbackActive()) {
            return false;
        }
        
        try {
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to upload chunk mesh for {}", handle.key(), e);
            return false;
        }
    }
    
    public static void onChunkVisible(int x, int y, int z, boolean visible) {
        ChunkHandle handle = ArkicClient.getChunkManager().getChunk(x, y, z);
        if (handle != null) {
            handle.visible = visible;
        }
    }
    
    public static int getActiveChunkCount() {
        return ArkicClient.getChunkManager() != null ? 
            ArkicClient.getChunkManager().chunkCount() : 0;
    }
    
    public static boolean isIntegrationActive() {
        return integrationActive && !SodiumFallback.isFallbackActive();
    }
}
