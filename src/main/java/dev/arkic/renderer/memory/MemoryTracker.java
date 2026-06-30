package dev.arkic.renderer.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

public class MemoryTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Memory");
    
    private long gpuMemoryUsed = 0;
    private long gpuMemoryPeak = 0;
    private long totalBytesUploaded = 0;
    private long totalChunksUploaded = 0;
    
    private final Deque<Long> allocationHistory = new ArrayDeque<>();
    private static final int MAX_HISTORY = 1024;
    
    public void recordChunkUpload(int bytes) {
        totalBytesUploaded += bytes;
        totalChunksUploaded++;
        gpuMemoryUsed += bytes;
        gpuMemoryPeak = Math.max(gpuMemoryPeak, gpuMemoryUsed);
        
        allocationHistory.push((long) bytes);
        if (allocationHistory.size() > MAX_HISTORY) {
            allocationHistory.removeLast();
        }
    }
    
    public void recordEviction(int bytes) {
        gpuMemoryUsed = Math.max(0, gpuMemoryUsed - bytes);
    }
    
    public long getGpuMemoryUsed() {
        return gpuMemoryUsed;
    }
    
    public long getGpuMemoryPeak() {
        return gpuMemoryPeak;
    }
    
    public long getTotalBytesUploaded() {
        return totalBytesUploaded;
    }
    
    public long getTotalChunksUploaded() {
        return totalChunksUploaded;
    }
    
    public void reset() {
        gpuMemoryUsed = 0;
        gpuMemoryPeak = 0;
    }
    
    public void logSummary() {
        LOGGER.info("GPU memory: current={}MB peak={}MB | Total uploaded: {}MB in {} chunks",
            gpuMemoryUsed / (1024 * 1024),
            gpuMemoryPeak / (1024 * 1024),
            totalBytesUploaded / (1024 * 1024),
            totalChunksUploaded);
    }
}
