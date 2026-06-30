package dev.arkic.renderer.chunk;

import dev.arkic.renderer.buffer.GpuBufferAllocator;

import java.util.concurrent.ConcurrentHashMap;

public class ChunkManager {
    private final ConcurrentHashMap<String, ChunkHandle> chunks = new ConcurrentHashMap<>();
    private final GpuBufferAllocator bufferAllocator;
    private int chunkCounter = 0;
    
    public ChunkManager(GpuBufferAllocator allocator) {
        this.bufferAllocator = allocator;
    }
    
    public ChunkHandle registerChunk(int x, int y, int z) {
        String key = x + "," + y + "," + z;
        return chunks.computeIfAbsent(key, k -> {
            ChunkHandle h = new ChunkHandle(x, y, z);
            long offset = bufferAllocator.allocateChunkData(1024 * 1024); // 1MB per section
            h.ssboOffset = offset;
            return h;
        });
    }
    
    public ChunkHandle getChunk(int x, int y, int z) {
        return chunks.get(x + "," + y + "," + z);
    }
    
    public void markChunkDirty(int x, int y, int z) {
        ChunkHandle h = getChunk(x, y, z);
        if (h != null) h.markDirty();
    }
    
    public void removeChunk(int x, int y, int z) {
        String key = x + "," + y + "," + z;
        ChunkHandle h = chunks.remove(key);
        if (h != null && h.ssboOffset != -1) {
            bufferAllocator.evictRegion(h.ssboOffset, 1024 * 1024);
        }
    }
    
    public int chunkCount() {
        return chunks.size();
    }
    
    public Iterable<ChunkHandle> allChunks() {
        return chunks.values();
    }
    
    public void clear() {
        chunks.clear();
    }
}
