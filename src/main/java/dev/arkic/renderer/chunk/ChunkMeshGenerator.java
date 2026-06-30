package dev.arkic.renderer.chunk;

import dev.arkic.client.ArkicClient;
import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.upload.ChunkUploadManager;
import dev.arkic.renderer.config.ArkicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkMeshGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/ChunkMesh");
    
    private final ChunkManager chunkManager;
    private final GpuBufferAllocator bufferAllocator;
    private final ChunkUploadManager uploadManager;
    private final ConcurrentHashMap<String, MeshUploadTask> pendingUploads = new ConcurrentHashMap<>();
    
    public ChunkMeshGenerator(ChunkManager chunks, GpuBufferAllocator allocator, ChunkUploadManager uploads) {
        this.chunkManager = chunks;
        this.bufferAllocator = allocator;
        this.uploadManager = uploads;
    }
    
    public boolean uploadChunkMesh(ChunkHandle handle, ByteBuffer vertexData) {
        if (vertexData == null || vertexData.remaining() == 0) {
            return false;
        }
        
        int sizeBytes = vertexData.remaining();
        vertexData.rewind();
        
        long offset = bufferAllocator.allocateChunkData(sizeBytes);
        if (offset < 0) {
            LOGGER.warn("Failed to allocate GPU memory for chunk {}", handle.key());
            return false;
        }
        
        bufferAllocator.uploadSsbData(offset, vertexData);
        
        int vertexCount = sizeBytes / 16; // 4 floats per vertex (pos + color + uv)
        handle.setUploaded(offset, vertexCount, (int)(offset / 16));
        
        return true;
    }
    
    public void processPendingUploads() {
        for (MeshUploadTask task : pendingUploads.values()) {
            if (!task.completed) {
                uploadChunkMesh(task.handle, task.vertexData);
                task.completed = true;
            }
        }
        pendingUploads.clear();
    }
    
    public void scheduleUpload(ChunkHandle handle, ByteBuffer vertexData) {
        pendingUploads.put(handle.key(), new MeshUploadTask(handle, vertexData));
    }
    
    private static class MeshUploadTask {
        final ChunkHandle handle;
        final ByteBuffer vertexData;
        boolean completed = false;
        
        MeshUploadTask(ChunkHandle handle, ByteBuffer vertexData) {
            this.handle = handle;
            this.vertexData = vertexData;
        }
    }
}
