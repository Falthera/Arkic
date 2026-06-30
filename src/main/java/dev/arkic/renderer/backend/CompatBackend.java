package dev.arkic.renderer.backend;

import dev.arkic.renderer.gl.GlCapabilities;
import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.upload.ChunkUploadManager;
import dev.arkic.renderer.chunk.ChunkManager;
import dev.arkic.diagnostics.ArkicBenchmark;
import org.lwjgl.opengl.GL43;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompatBackend extends RenderBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Backend/Compat");
    
    private GpuBufferAllocator bufferAllocator;
    private ChunkUploadManager uploadManager;
    private ChunkManager chunkManager;
    private ArkicBenchmark benchmark;

    @Override
    public boolean initialize(GlCapabilities caps) {
        this.caps = caps;
        if (!caps.supportsCompatBackend()) {
            LOGGER.warn("Requirements not met for Compat backend");
            return false;
        }
        
        LOGGER.info("Running in Compat mode — no compute shaders, optimized standard pipeline");
        initialized = true;
        return true;
    }
    
    public void setSubsystems(GpuBufferAllocator allocator, ChunkUploadManager uploads, ChunkManager chunks, ArkicBenchmark bench) {
        this.bufferAllocator = allocator;
        this.uploadManager = uploads;
        this.chunkManager = chunks;
        this.benchmark = bench;
    }
    
    @Override
    public void renderWorld(float tickDelta) {
        if (!initialized) return;
        long t0 = System.nanoTime();
        
        int chunkCount = chunkManager != null ? chunkManager.chunkCount() : 0;
        
        
        long t1 = System.nanoTime();
        if (benchmark != null) benchmark.recordCpuTime(t0, t1);
    }

    @Override
    public void beginFrame() {
        frameCounter.incrementAndGet();
        if (bufferAllocator != null) bufferAllocator.resetFrame();
        if (uploadManager != null) uploadManager.beginFrame();
        if (benchmark != null) benchmark.beginFrame();
    }

    @Override
    public void endFrame() {
        if (benchmark != null) benchmark.logFrame();
    }
}
