package dev.arkic.renderer.render;

import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.gl.GlCapabilities;
import dev.arkic.renderer.shader.ShaderManager;
import dev.arkic.renderer.culling.ComputeCullPass;
import dev.arkic.diagnostics.ArkicBenchmark;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndirectDrawBatch {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Render/Indirect");
    
    private final GlCapabilities caps;
    private final ShaderManager shaderManager;
    private final GpuBufferAllocator bufferAllocator;
    private final ComputeCullPass cullPass;
    private final ArkicBenchmark benchmark;
    private int renderProgram = -1;
    private int chunkMeshSsbo = -1;
    private int textureArray = -1;
    
    public IndirectDrawBatch(GlCapabilities caps, ShaderManager shaders, GpuBufferAllocator allocator, ComputeCullPass cullPass, ArkicBenchmark benchmark) {
        this.caps = caps;
        this.shaderManager = shaders;
        this.bufferAllocator = allocator;
        this.cullPass = cullPass;
        this.benchmark = benchmark;
    }
    
    public boolean initialize() {
        if (!caps.hasMultiDrawIndirect) {
            LOGGER.warn("MultiDrawIndirect unavailable");
            return false;
        }
        
        renderProgram = shaderManager.createProgram(
            "arkic_render",
            "assets/arkic/shaders/render/vert.glsl",
            "assets/arkic/shaders/render/frag.glsl",
            null
        );
        
        if (renderProgram == -1) {
            LOGGER.error("Failed to create render program");
            return false;
        }
        
        shaderManager.bindBlockToBinding("arkic_render", "ChunkMeshes", 2);
        
        chunkMeshSsbo = GL43.glGenBuffers();
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, chunkMeshSsbo);
        GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, 256L * 1024 * 1024, GL43.GL_DYNAMIC_DRAW);
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        
        LOGGER.info("Indirect draw batch initialized");
        return true;
    }
    
    public void renderChunks(int chunkCount) {
        if (renderProgram == -1 || chunkCount <= 0) return;
        
        long t0 = System.nanoTime();
        
        shaderManager.useProgram("arkic_render");
        
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, bufferAllocator.getSsbo());
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, bufferAllocator.getIndirectBuffer());
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, chunkMeshSsbo);
        
        if (caps.hasMultiDrawIndirect) {
            GL43.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, bufferAllocator.getIndirectBuffer());
            GL43.glMultiDrawArraysIndirect(GL43.GL_QUADS, 0, chunkCount);
            GL43.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, 0);
            benchmark.addDrawCalls(chunkCount);
        }
        
        long t1 = System.nanoTime();
        benchmark.recordGpuTime(t0, t1);
    }
    
    public void close() {
        if (renderProgram != -1) {
            org.lwjgl.opengl.GL20.glDeleteProgram(renderProgram);
            renderProgram = -1;
        }
        if (chunkMeshSsbo != -1) {
            GL43.glDeleteBuffers(chunkMeshSsbo);
            chunkMeshSsbo = -1;
        }
    }
}
