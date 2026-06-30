package dev.arkic.client;

import dev.arkic.Arkic;
import dev.arkic.crash.ArkicCrashHandler;
import dev.arkic.diagnostics.ArkicBenchmark;
import dev.arkic.diagnostics.ArkicDiagnostics;
import dev.arkic.renderer.ArkicRenderer;
import dev.arkic.renderer.backend.*;
import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.chunk.ChunkManager;
import dev.arkic.renderer.config.ArkicConfig;
import dev.arkic.renderer.culling.ComputeCullPass;
import dev.arkic.renderer.render.IndirectDrawBatch;
import dev.arkic.renderer.shader.ShaderManager;
import dev.arkic.renderer.upload.ChunkUploadManager;
import dev.arkic.renderer.gl.GlCapabilities;
import dev.arkic.sodium.SodiumInterop;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArkicClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArkicClient.class);
    
    public static GlCapabilities caps;
    public static GpuBufferAllocator bufferAllocator;
    public static ShaderManager shaderManager;
    public static ChunkManager chunkManager;
    public static ChunkUploadManager uploadManager;
    public static ComputeCullPass cullPass;
    public static IndirectDrawBatch indirectDraw;
    public static ArkicBenchmark benchmark;
    
    public static GlCapabilities getCaps() { return caps; }
    public static GpuBufferAllocator getBufferAllocator() { return bufferAllocator; }
    public static ShaderManager getShaderManager() { return shaderManager; }
    public static ChunkManager getChunkManager() { return chunkManager; }
    public static ChunkUploadManager getUploadManager() { return uploadManager; }
    public static ComputeCullPass getCullPass() { return cullPass; }
    public static ArkicBenchmark getBenchmark() { return benchmark; }
    
    @Override
    public void onInitializeClient() {
        Arkic.init();
        
        if (Arkic.isSafeMode()) {
            LOGGER.warn("Running in safe mode — limited functionality");
            return;
        }
        
        try {
            LOGGER.info("Initializing Arkic GPU backend...");
            
            caps = GlCapabilities.detect();
            ArkicDiagnostics.logOpenGLState();
            
            LOGGER.info("Recommended backend: {}", caps.recommendedBackend());
            
            bufferAllocator = new GpuBufferAllocator(caps);
            boolean allocatorOk = bufferAllocator.initialize();
            if (!allocatorOk) {
                ArkicCrashHandler.reportRecoverableError("GpuBufferAllocator", new RuntimeException("Init failed"));
            }
            
            shaderManager = new ShaderManager(caps);
            chunkManager = new ChunkManager(bufferAllocator);
            uploadManager = new ChunkUploadManager();
            cullPass = new ComputeCullPass(caps, bufferAllocator, shaderManager);
            indirectDraw = new IndirectDrawBatch(caps, shaderManager, bufferAllocator, cullPass, null);
            benchmark = new ArkicBenchmark(bufferAllocator, uploadManager);
            
            SodiumInterop.initialize();
            
            boolean success = ArkicRenderer.initialize();
            if (!success) {
                LOGGER.warn("Switched to Sodium fallback backend");
            }
            
            RenderBackend backend = ArkicRenderer.getBackend();
            if (backend instanceof AdvancedBackend ab) {
                ab.setSubsystems(bufferAllocator, uploadManager, chunkManager, cullPass, indirectDraw, benchmark);
            } else if (backend instanceof ComputeBackend cb) {
                cb.setSubsystems(bufferAllocator, uploadManager, chunkManager, cullPass, benchmark);
            } else if (backend instanceof CompatBackend co) {
                co.setSubsystems(bufferAllocator, uploadManager, chunkManager, benchmark);
            }
            
            LOGGER.info("Arkic initialization complete — backend: {}", 
                backend != null ? backend.name() : "none");
            ArkicDiagnostics.logBackendStatus(backend);
            
        } catch (Exception e) {
            LOGGER.error("Fatal error during Arkic initialization", e);
            ArkicCrashHandler.reportRecoverableError("Initialization", e);
        }
    }
}
