package dev.arkic.diagnostics;

import dev.arkic.renderer.ArkicRenderer;
import dev.arkic.renderer.backend.RenderBackend;
import dev.arkic.renderer.buffer.GpuBufferAllocator;
import dev.arkic.renderer.config.ArkicConfig;
import dev.arkic.renderer.upload.ChunkUploadManager;
import dev.arkic.renderer.gl.GlCapabilities;
import dev.arkic.crash.ArkicCrashHandler;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

public class ArkicProfiler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Profiler");
    private static final DecimalFormat DF = new DecimalFormat("#,##0.0");
    private static final DecimalFormat DF2 = new DecimalFormat("#,##0.00");
    
    private static long lastLogTime = 0;
    private static long lastFrameCount = 0;
    private static long lastBenchmarkLog = 0;
    
    public static void logPeriodicStatus() {
        long now = System.currentTimeMillis();
        
        if (now - lastLogTime < 10000 && !ArkicConfig.VERBOSE_LOGGING) return;
        lastLogTime = now;
        
        RenderBackend backend = ArkicRenderer.getBackend();
        
        StringBuilder sb = new StringBuilder("\n=== Arkic Status ===\n");
        sb.append("Backend: ").append(backend != null ? backend.name() : "Sodium Fallback").append("\n");
        sb.append("Initialized: ").append(ArkicRenderer.isActive()).append("\n");
        
        GlCapabilities caps = dev.arkic.client.ArkicClient.getCaps();
        if (caps != null) {
            sb.append("GPU: ").append(caps.glRenderer).append("\n");
            sb.append("OpenGL: ").append(caps.glVersion).append("\n");
            sb.append("Backend recommended: ").append(caps.recommendedBackend()).append("\n");
        }
        
        if (backend != null && backend instanceof dev.arkic.renderer.backend.AdvancedBackend ab) {
            GpuBufferAllocator allocator = dev.arkic.client.ArkicClient.getBufferAllocator();
            if (allocator != null) {
                sb.append("SSBO usage: ").append(allocator.getCurrentSsboUsage() / (1024*1024)).append(" MB\n");
                sb.append("Frame uploads: ").append(allocator.getFrameBytesUploaded()).append(" bytes\n");
            }
            
            ChunkUploadManager uploads = dev.arkic.client.ArkicClient.getUploadManager();
            if (uploads != null) {
                sb.append("Upload queue: ").append(uploads.getQueueDepth()).append(" tasks\n");
            }
        }
        
        int glError = GL11.glGetError();
        if (glError != GL11.GL_NO_ERROR) {
            sb.append("GL Error: ").append(ArkicCrashHandler.getGLErrorString(glError)).append("\n");
        }
        
        sb.append("===================\n");
        LOGGER.info(sb.toString());
    }
    
    public static void logBenchmarkResults() {
        long now = System.currentTimeMillis();
        if (now - lastBenchmarkLog < 60000) return;
        lastBenchmarkLog = now;
        
        RenderBackend backend = ArkicRenderer.getBackend();
        if (backend == null) return;
        
        LOGGER.info("=== Benchmark ===");
        LOGGER.info("Backend: {}", backend.name());
        LOGGER.info("Frame count: {}", backend.frameCounter.get());
        LOGGER.info("================");
    }
    
    public static void checkGlErrors(String stage) {
        if (!ArkicConfig.DEBUG_OPENGL) return;
        
        int err = GL11.glGetError();
        if (err != GL11.GL_NO_ERROR) {
            LOGGER.warn("GL Error at {}: {}", stage, ArkicCrashHandler.getGLErrorString(err));
        }
    }
}
