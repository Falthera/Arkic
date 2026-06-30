package dev.arkic.renderer.culling;

import dev.arkic.renderer.gl.GlCapabilities;
import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.shader.ShaderManager;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeCullPass {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Culling");
    
    private final GlCapabilities caps;
    private final GpuBufferAllocator bufferAllocator;
    private final ShaderManager shaderManager;
    private int cullProgram = -1;
    private int visibilityBuffer = -1;
    private int chunkMetaBuffer = -1;
    private int cameraBuffer = -1;
    
    public ComputeCullPass(GlCapabilities caps, GpuBufferAllocator allocator, ShaderManager shaders) {
        this.caps = caps;
        this.bufferAllocator = allocator;
        this.shaderManager = shaders;
    }
    
    public boolean initialize() {
        if (!caps.hasComputeShaders) {
            LOGGER.warn("Compute shaders unavailable, GPU culling disabled");
            return false;
        }
        
        cullProgram = shaderManager.createProgram(
            "arkic_cull",
            null,
            null,
            shaderSource_computeCull()
        );
        
        if (cullProgram == -1) {
            LOGGER.error("Failed to create culling compute program");
            return false;
        }
        
        // Bind SSBO block for chunk metadata
        shaderManager.bindBlockToBinding("arkic_cull", "ChunkMeta", 0);
        shaderManager.bindBlockToBinding("arkic_cull", "DrawCommands", 1);
        
        // Create visibility buffer (atomic counter target)
        visibilityBuffer = GL43.glGenBuffers();
        GL43.glBindBuffer(GL43.GL_ATOMIC_COUNTER_BUFFER, visibilityBuffer);
        GL43.glBufferData(GL43.GL_ATOMIC_COUNTER_BUFFER, 4, GL43.GL_DYNAMIC_DRAW);
        GL43.glBindBuffer(GL43.GL_ATOMIC_COUNTER_BUFFER, 0);
        
        cameraBuffer = GL43.glGenBuffers();
        GL43.glBindBuffer(GL43.GL_UNIFORM_BUFFER, cameraBuffer);
        GL43.glBufferData(GL43.GL_UNIFORM_BUFFER, 128, GL43.GL_DYNAMIC_DRAW);
        GL43.glBindBuffer(GL43.GL_UNIFORM_BUFFER, 0);
        
        LOGGER.info("Compute culling pass initialized");
        return true;
    }
    
    public void dispatch(int chunkCount) {
        if (cullProgram == -1) return;
        
        shaderManager.useProgram("arkic_cull");
        
        // Bind chunk metadata SSBO (binding 0)
        int ssbo = bufferAllocator.getSsbo();
        if (ssbo != -1) {
            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, ssbo);
        }
        
        // Bind indirect draw buffer (binding 1)
        int indirect = bufferAllocator.getIndirectBuffer();
        if (indirect != -1) {
            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, indirect);
        }
        
        // Reset counter
        bufferAllocator.resetCounter();
        GL43.glBindBufferBase(GL43.GL_ATOMIC_COUNTER_BUFFER, 0, bufferAllocator.getIndirectCounterBuffer());
        
        // Dispatch compute shader — one work group per chunk
        int workGroups = Math.max(1, (chunkCount + 63) / 64);
        GL43.glDispatchCompute(workGroups, 1, 1);
        
        // Memory barrier to ensure writes are visible before draw
        GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | 
                            GL43.GL_COMMAND_BARRIER_BIT | 
                            GL43.GL_ATOMIC_COUNTER_BARRIER_BIT);
    }
    
    public void uploadCamera(float[] viewMatrix, float[] projMatrix, float[] frustum) {
        ByteBuffer buf = BufferUtils.createByteBuffer(128);
        buf.asFloatBuffer().put(viewMatrix).position(0)
          .limit(16).put(projMatrix).position(0)
          .limit(16).put(frustum).position(0)
          .limit(48);
        
        GL43.glBindBuffer(GL43.GL_UNIFORM_BUFFER, cameraBuffer);
        GL43.glBufferSubData(GL43.GL_UNIFORM_BUFFER, 0, buf);
        GL43.glBindBufferBase(GL43.GL_UNIFORM_BUFFER, 0, cameraBuffer);
        GL43.glBindBuffer(GL43.GL_UNIFORM_BUFFER, 0);
    }
    
    private String shaderSource_computeCull() {
        return "#version 430 core\n" +
            "\n" +
            "layout(local_size_x = 64) in;\n" +
            "\n" +
            "struct ChunkMeta {\n" +
            "    vec4  offset;\n" +
            "    vec4  boundsMin;\n" +
            "    vec4  boundsMax;\n" +
            "    uint  visible;\n" +
            "    uint  materialMask;\n" +
            "    uint  drawOffset;\n" +
            "    uint  drawCount;\n" +
            "};\n" +
            "\n" +
            "layout(std430, binding = 0) buffer ChunkMetaBlock {\n" +
            "    ChunkMeta chunks[];\n" +
            "};\n" +
            "\n" +
            "layout(std430, binding = 1) buffer IndirectBlock {\n" +
            "    uint counts[];\n" +
            "};\n" +
            "\n" +
            "layout(std430, binding = 2) buffer CameraBlock {\n" +
            "    mat4 viewProjection;\n" +
            "    vec4 frustum[6];\n" +
            "};\n" +
            "\n" +
            "uniform uint totalChunks;\n" +
            "uniform uint chunkSize;\n" +
            "\n" +
            "bool frustumIntersectsAABB(vec3 boxMin, vec3 boxMax, vec4 frustum[6]) {\n" +
            "    for (int i = 0; i < 6; i++) {\n" +
            "        vec3 axis = vec3(frustum[i].x, frustum[i].y, frustum[i].z);\n" +
            "        float d = frustum[i].w;\n" +
            "        vec3 p = dot(axis, boxMin + axis * (boxMax - boxMin)) >= d ? boxMax : boxMin;\n" +
            "        if (dot(axis, p) < d) return false;\n" +
            "    }\n" +
            "    return true;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    uint id = gl_GlobalInvocationID.x;\n" +
            "    if (id >= totalChunks) return;\n" +
            "    \n" +
            "    ChunkMeta cm = chunks[id];\n" +
            "    \n" +
            "    bool visible = true;\n" +
            "    if (totalChunks > 96) {\n" +
            "        visible = frustumIntersectsAABB(cm.boundsMin.xyz, cm.boundsMax.xyz, frustum);\n" +
            "    }\n" +
            "    \n" +
            "    if (visible && cm.drawCount > 0) {\n" +
            "        uint slot = atomicCounterIncrement(0);\n" +
            "        uint offset = cm.drawOffset;\n" +
            "        counts[slot * 4 + 0] = cm.drawCount;\n" +
            "        counts[slot * 4 + 1] = 1u;\n" +
            "        counts[slot * 4 + 2] = 0u;\n" +
            "        counts[slot * 4 + 3] = 0u;\n" +
            "    }\n" +
            "    \n" +
            "    cm.visible = visible ? 1u : 0u;\n" +
            "    chunks[id] = cm;\n" +
            "}\n";
    }
    
    public void close() {
        if (cullProgram != -1) {
            org.lwjgl.opengl.GL20.glDeleteProgram(cullProgram);
            cullProgram = -1;
        }
        if (visibilityBuffer != -1) {
            GL43.glDeleteBuffers(visibilityBuffer);
            visibilityBuffer = -1;
        }
        if (cameraBuffer != -1) {
            GL43.glDeleteBuffers(cameraBuffer);
            cameraBuffer = -1;
        }
    }
}
