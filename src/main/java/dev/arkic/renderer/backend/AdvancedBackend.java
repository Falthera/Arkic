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

public class AdvancedBackend extends RenderBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Backend/Advanced");
    
    private GpuBufferAllocator bufferAllocator;
    private ChunkUploadManager uploadManager;
    private ChunkManager chunkManager;
    private ComputeCullPass cullPass;
    private IndirectDrawBatch indirectDraw;
    private ArkicBenchmark benchmark;

    @Override
    public boolean initialize(GlCapabilities caps) {
        this.caps = caps;
        if (!caps.supportsAdvancedBackend()) {
            LOGGER.warn("Requirements not met for Advanced backend");
            return false;
        }
        
        int maxSSBOSize = org.lwjgl.opengl.GL45.glGetInteger(org.lwjgl.opengl.GL43.GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
        LOGGER.info("Max SSBO block size: {} MB", maxSSBOSize / (1024 * 1024));
        
        initialized = true;
        return true;
    }
    
    public void setSubsystems(GpuBufferAllocator allocator, ChunkUploadManager uploads, ChunkManager chunks, ComputeCullPass cull, IndirectDrawBatch draw, ArkicBenchmark bench) {
        this.bufferAllocator = allocator;
        this.uploadManager = uploads;
        this.chunkManager = chunks;
        this.cullPass = cull;
        this.indirectDraw = draw;
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
        
        if (indirectDraw != null && chunkCount > 0) {
            indirectDraw.renderChunks(chunkCount);
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
