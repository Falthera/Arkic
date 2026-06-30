package dev.arkic.renderer.backend;

import dev.arkic.renderer.gl.GlCapabilities;
import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.upload.ChunkUploadManager;
import dev.arkic.renderer.culling.ComputeCullPass;
import dev.arkic.renderer.render.IndirectDrawBatch;
import dev.arkic.renderer.chunk.ChunkManager;
import dev.arkic.diagnostics.ArkicBenchmark;
import org.lwjgl.opengl.GL43;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeBackend extends RenderBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Backend/Compute");
    
    private GpuBufferAllocator bufferAllocator;
    private ChunkUploadManager uploadManager;
    private ChunkManager chunkManager;
    private ComputeCullPass cullPass;
    private ArkicBenchmark benchmark;

    @Override
    public boolean initialize(GlCapabilities caps) {
        this.caps = caps;
        if (!caps.supportsComputeBackend()) {
            LOGGER.warn("Requirements not met for Compute backend");
            return false;
        }
        
        int maxWorkGroups = org.lwjgl.opengl.GL43.glGetInteger(org.lwjgl.opengl.GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS);
        LOGGER.info("Max work group invocations: {}", maxWorkGroups);
        
        initialized = true;
        return true;
    }
    
    public void setSubsystems(GpuBufferAllocator allocator, ChunkUploadManager uploads, ChunkManager chunks, ComputeCullPass cull, ArkicBenchmark bench) {
        this.bufferAllocator = allocator;
        this.uploadManager = uploads;
        this.chunkManager = chunks;
        this.cullPass = cull;
        this.benchmark = bench;
    }
    
    @Override
    public void renderWorld(float tickDelta) {
        if (!initialized) return;
        long t0 = System.nanoTime();
        
        int chunkCount = chunkManager != null ? chunkManager.chunkCount() : 0;
        
        if (cullPass != null && chunkCount > 0) {
            cullPass.dispatch(chunkCount);
        }
        
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
