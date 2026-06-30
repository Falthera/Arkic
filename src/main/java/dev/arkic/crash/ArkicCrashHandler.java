package dev.arkic.crash;

import dev.arkic.Arkic;
import dev.arkic.renderer.ArkicRenderer;
import dev.arkic.renderer.backend.SodiumFallback;
import dev.arkic.renderer.gl.GlCapabilities;
import dev.arkic.renderer.buffer.GpuBufferAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArkicCrashHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Crash");
    private static final int MAX_CONSECUTIVE_CRASHES = 3;
    private static int crashCount = 0;
    private static long lastCrashTime = 0;
    
    static {
        Thread.setDefaultUncaughtExceptionHandler(new ArkicCrashHandler());
    }
    
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        long now = System.currentTimeMillis();
        if (now - lastCrashTime < 10000) {
            crashCount++;
        } else {
            crashCount = 1;
        }
        lastCrashTime = now;
        
        LOGGER.error("Unhandled exception in thread {}: {}", t.getName(), e.getMessage(), e);
        
        if (crashCount >= MAX_CONSECUTIVE_CRASHES) {
            LOGGER.error("Multiple crashes detected — disabling Arkic GPU pipeline");
            disableSafely();
        }
    }
    
    private static void disableSafely() {
        try {
            ArkicRenderer.shutdown();
            LOGGER.warn("Arkic has been disabled due to repeated crashes");
        } catch (Exception e) {
            LOGGER.error("Failed to disable Arkic cleanly", e);
        }
    }
    
    public static void reportRecoverableError(String context, Throwable e) {
        LOGGER.warn("Recoverable error in {}: {}", context, e.getMessage());
    }
    
    public static void reportGraphicsError(String context, int glError) {
        LOGGER.error("OpenGL error {} in {}: {}", glError, context, getGLErrorString(glError));
    }
    
    private static String getGLErrorString(int err) {
        return switch (err) {
            case 0x0500 -> "GL_INVALID_ENUM";
            case 0x0501 -> "GL_INVALID_VALUE";
            case 0x0502 -> "GL_INVALID_OPERATION";
            case 0x0503 -> "GL_STACK_OVERFLOW";
            case 0x0504 -> "GL_STACK_UNDERFLOW";
            case 0x0505 -> "GL_OUT_OF_MEMORY";
            case 0x0506 -> "GL_INVALID_FRAMEBUFFER_OPERATION";
            default -> "Unknown (0x" + Integer.toHexString(err) + ")";
        };
    }
}
