package dev.arkic.renderer.upload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkUploadManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/UploadManager");
    
    private final ThreadPoolExecutor uploadExecutor;
    private final LinkedBlockingQueue<UploadTask> queue = new LinkedBlockingQueue<>();
    private final CountDownLatch shutdownLatch;
    private volatile boolean running = true;
    
    private final AtomicLong bytesUploadedCurrentFrame = new AtomicLong(0);
    private final AtomicLong bytesUploadedTotal = new AtomicLong(0);
    private final AtomicLong chunksUploadedTotal = new AtomicLong(0);
    
    private static class UploadTask {
        final long offset;
        final int sizeBytes;
        final ByteBuffer data;
        final Runnable onComplete;
        
        UploadTask(long offset, int sizeBytes, ByteBuffer data, Runnable onComplete) {
            this.offset = offset;
            this.sizeBytes = sizeBytes;
            this.data = data;
            this.onComplete = onComplete;
        }
    }
    
    public ChunkUploadManager() {
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        this.uploadExecutor = new ThreadPoolExecutor(
            threads, threads,
            30L, TimeUnit.SECONDS,
            queue,
            r -> {
                Thread t = new Thread(r, "arkic-upload-" + t.getId());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.shutdownLatch = new CountDownLatch(1);
        
        Thread processor = new Thread(this::processUploads, "arkic-upload-processor");
        processor.setDaemon(true);
        processor.start();
    }
    
    private void processUploads() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                UploadTask task = queue.poll(100, TimeUnit.MILLISECONDS);
                if (task == null) continue;
                
                // Perform STREAMING upload to pre-allocated region
                // In the real implementation this would interact with GpuBufferAllocator
                bytesUploadedCurrentFrame.addAndGet(task.sizeBytes);
                bytesUploadedTotal.addAndGet(task.sizeBytes);
                chunksUploadedTotal.incrementAndGet();
                
                if (task.onComplete != null) task.onComplete.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        shutdownLatch.countDown();
    }
    
    public void scheduleUpload(long offset, int sizeBytes, ByteBuffer data, Runnable onComplete) {
        if (!running) return;
        UploadTask task = new UploadTask(offset, sizeBytes, data.duplicate(), onComplete);
        uploadExecutor.execute(() -> {
            queue.offer(task);
        });
    }
    
    public void beginFrame() {
        bytesUploadedCurrentFrame.set(0);
    }
    
    public long getFrameBytesUploaded() {
        return bytesUploadedCurrentFrame.get();
    }
    
    public long getTotalBytesUploaded() {
        return bytesUploadedTotal.get();
    }
    
    public long getTotalChunksUploaded() {
        return chunksUploadedTotal.get();
    }
    
    public int getQueueDepth() {
        return queue.size();
    }
    
    public void shutdown() {
        running = false;
        uploadExecutor.shutdown();
        try {
            shutdownLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
