package dev.arkic.renderer.buffer;

import dev.arkic.renderer.gl.GlCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

public class GpuBufferAllocator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/BufferAllocator");
    
    private static final long MAX_SSBO_SIZE = 512L * 1024 * 1024; // 512 MB
    private static final long MAX_INDIRECT_BUFFER_SIZE = 128L * 1024 * 1024; // 128 MB
    
    private final GlCapabilities caps;
    private final boolean usePersistentMapping;
    
    private int ssbo = -1;
    private final AtomicLong ssboCursor = new AtomicLong(0);
    private ByteBuffer ssboMapped;
    private boolean ssboMappedPersistently = false;
    
    private int indirectBuffer = -1;
    private int indirectCounterBuffer = -1;
    private final AtomicLong indirectCursor = new AtomicLong(0);
    
    private final Deque<Long> freeSsbOffsets = new ArrayDeque<>();
    private final Deque<Long> freeSsbSizes = new ArrayDeque<>();
    
    private long totalAllocatedBytes;
    private long peakAllocatedBytes;
    private long allocationCount;
    private long failedAllocations;
    
    public GpuBufferAllocator(GlCapabilities caps) {
        this.caps = caps;
        this.usePersistentMapping = caps.hasArbBufferStorage;
    }
    
    public boolean initialize() {
        if (caps.hasSSBOSupport) {
            ssbo = GL43.glGenBuffers();
            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
            
            int size = (int)Math.min(MAX_SSBO_SIZE, GL43.glGetInteger(GL43.GL_MAX_SHADER_STORAGE_BLOCK_SIZE));
            if (usePersistentMapping) {
                int flags = GL45.GL_MAP_WRITE_BIT | 
                            GL45.GL_MAP_PERSISTENT_BIT | 
                            GL45.GL_MAP_COHERENT_BIT;
                GL45.glBufferStorage(GL43.GL_SHADER_STORAGE_BUFFER, size, flags);
                ssboMapped = GL45.glMapBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, 0, size, flags);
                ssboMappedPersistently = true;
                LOGGER.info("SSBO allocated with persistent mapping ({} MB)", size / (1024 * 1024));
            } else {
                GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, size, GL43.GL_DYNAMIC_DRAW);
            }
            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }
        
        indirectBuffer = GL43.glGenBuffers();
        GL43.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, indirectBuffer);
        int indirectSize = (int)MAX_INDIRECT_BUFFER_SIZE;
        GL43.glBufferData(GL43.GL_DRAW_INDIRECT_BUFFER, indirectSize, GL43.GL_DYNAMIC_DRAW);
        GL43.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, 0);
        
        indirectCounterBuffer = GL43.glGenBuffers();
        GL43.glBindBuffer(GL43.GL_ATOMIC_COUNTER_BUFFER, indirectCounterBuffer);
        GL43.glBufferData(GL43.GL_ATOMIC_COUNTER_BUFFER, 4, GL43.GL_DYNAMIC_DRAW);
        GL43.glBindBuffer(GL43.GL_ATOMIC_COUNTER_BUFFER, 0);
        
        LOGGER.info("GpuBufferAllocator initialized");
        return true;
    }
    
    public long allocateChunkData(int sizeBytes) {
        long offset;
        if (!freeSsbOffsets.isEmpty() && freeSsbSizes.peek() >= sizeBytes) {
            offset = freeSsbOffsets.pop();
            freeSsbSizes.pop();
        } else {
            offset = ssboCursor.getAndAdd(sizeBytes);
            if (offset + sizeBytes > MAX_SSBO_SIZE) {
                // Defragment or fallback — for now, wrap around with eviction
                ssboCursor.set(0);
                offset = ssboCursor.getAndAdd(sizeBytes);
                failedAllocations++;
                LOGGER.warn("SSBO wrap-around — eviction needed");
            }
        }
        
        totalAllocatedBytes += sizeBytes;
        allocationCount++;
        peakAllocatedBytes = Math.max(peakAllocatedBytes, totalAllocatedBytes);
        return offset;
    }
    
    public long allocateIndirectDraw(long sizeBytes) {
        long offset = indirectCursor.getAndAdd(sizeBytes);
        if (offset + sizeBytes > MAX_INDIRECT_BUFFER_SIZE) {
            indirectCursor.set(0);
            offset = indirectCursor.getAndAdd(sizeBytes);
        }
        return offset;
    }
    
    public void uploadSsbData(long offset, ByteBuffer data) {
        if (ssbo == -1 || !caps.hasSSBOSupport) return;
        
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
        if (usePersistentMapping && ssboMapped != null) {
            int pos = data.position();
            ssboMapped.position((int)offset);
            ssboMapped.put(data);
            data.position(pos);
            if (!ssboMappedPersistently) {
                GL45.glFlushMappedBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, offset, data.remaining());
            }
        } else {
            GL43.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, offset, data);
        }
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }
    
    public void uploadIndirectData(long offset, ByteBuffer data) {
        if (indirectBuffer == -1) return;
        GL43.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, indirectBuffer);
        GL43.glBufferSubData(GL43.GL_DRAW_INDIRECT_BUFFER, offset, data);
        GL43.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, 0);
    }
    
    public void resetCounter() {
        if (indirectCounterBuffer != -1) {
            GL43.glBindBuffer(GL43.GL_ATOMIC_COUNTER_BUFFER, indirectCounterBuffer);
            GL43.glBufferSubData(GL43.GL_ATOMIC_COUNTER_BUFFER, 0, new int[]{0});
            GL43.glBindBuffer(GL43.GL_ATOMIC_COUNTER_BUFFER, 0);
        }
        indirectCursor.set(0);
    }
    
    public void resetFrame() {
        totalAllocatedBytes = 0;
        indirectCursor.set(0);
    }
    
    public int getSsbo() { return ssbo; }
    public int getIndirectBuffer() { return indirectBuffer; }
    public int getIndirectCounterBuffer() { return indirectCounterBuffer; }
    public boolean hasPersistentMapping() { return usePersistentMapping; }
    public long getCurrentSsboUsage() { return ssboCursor.get(); }
    public long getFrameBytesUploaded() { return totalAllocatedBytes; }
    public long getPeakAllocatedBytes() { return peakAllocatedBytes; }
    public long getAllocationCount() { return allocationCount; }
    public long getFailedAllocations() { return failedAllocations; }
    
    public void evictRegion(long offset, long size) {
        freeSsbOffsets.push(offset);
        freeSsbSizes.push(size);
    }
    
    public void close() {
        if (indirectCounterBuffer != -1) {
            GL43.glDeleteBuffers(indirectCounterBuffer);
            indirectCounterBuffer = -1;
        }
        if (indirectBuffer != -1) {
            GL43.glDeleteBuffers(indirectBuffer);
            indirectBuffer = -1;
        }
        if (ssbo != -1) {
            if (ssboMappedPersistently && ssboMapped != null) {
                GL45.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
                ssboMapped = null;
            }
            GL43.glDeleteBuffers(ssbo);
            ssbo = -1;
        }
    }
}
