package dev.arkic.diagnostics;

import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.upload.ChunkUploadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

public class ArkicBenchmark {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Benchmark");
    private static final DecimalFormat DF = new DecimalFormat("#,##0.0");
    
    private final GpuBufferAllocator bufferAllocator;
    private final ChunkUploadManager uploadManager;
    private long frameStartTime;
    private long gpuFrameTimeNs;
    private int drawCalls;
    private int triangles;
    
    private long totalFrames = 0;
    private long totalCpuTimeNs = 0;
    private long totalGpuTimeNs = 0;
    
    public ArkicBenchmark(GpuBufferAllocator allocator, ChunkUploadManager uploads) {
        this.bufferAllocator = allocator;
        this.uploadManager = uploads;
    }
    
    public void beginFrame() {
        frameStartTime = System.nanoTime();
        drawCalls = 0;
        triangles = 0;
        gpuFrameTimeNs = 0;
    }
    
    public void recordGpuTime(long startNs, long endNs) {
        gpuFrameTimeNs += (endNs - startNs);
    }
    
    public void addDrawCalls(int count) {
        drawCalls += count;
    }
    
    public void addTriangles(int count) {
        triangles += count;
    }
    
    public void endFrame() {
        long now = System.nanoTime();
        totalCpuTimeNs += (now - frameStartTime);
        totalGpuTimeNs += gpuFrameTimeNs;
        totalFrames++;
    }
    
    public void logFrame() {
        if (totalFrames % 600 == 0) {
            double avgCpuMs = totalFrames > 0 ? (totalCpuTimeNs / totalFrames) / 1_000_000.0 : 0;
            double avgGpuMs = totalFrames > 0 ? (totalGpuTimeNs / totalFrames) / 1_000_000.0 : 0;
            LOGGER.info(
                "Frames: {} | Avg CPU: {}ms | Avg GPU: {}ms | Draw calls: {} | Triangles: {} | SSBO usage: {}MB | Upload: {}B",
                totalFrames, DF.format(avgCpuMs), DF.format(avgGpuMs),
                drawCalls, triangles,
                bufferAllocator.getCurrentSsboUsage() / (1024 * 1024),
                uploadManager.getFrameBytesUploaded()
            );
        }
    }
    
    public double avgCpuFrameTimeMs() {
        return totalFrames > 0 ? (totalCpuTimeNs / totalFrames) / 1_000_000.0 : 0;
    }
    
    public double avgGpuTimeMs() {
        return totalFrames > 0 ? (totalGpuTimeNs / totalFrames) / 1_000_000.0 : 0;
    }
    
    public int currentDrawCalls() { return drawCalls; }
    public int currentTriangles() { return triangles; }
}
