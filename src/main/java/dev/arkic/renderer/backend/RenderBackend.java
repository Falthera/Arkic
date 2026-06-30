package dev.arkic.renderer.backend;

import dev.arkic.renderer.gl.GlCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public abstract class RenderBackend implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Backend");
    
    protected final AtomicLong frameCounter = new AtomicLong(0);
    protected GlCapabilities caps;
    protected boolean initialized = false;

    public abstract boolean initialize(GlCapabilities caps);
    
    public abstract GlCapabilities capabilities();
    
    public abstract void renderWorld(float tickDelta);
    
    public abstract void beginFrame();
    
    public abstract void endFrame();
    
    public abstract void close();
    
    public String name() {
        return this.getClass().getSimpleName().replace("Backend", "").toUpperCase();
    }
    
    public final boolean isInitialized() {
        return initialized;
    }
    
    protected void checkGLError(String location) {
        int err = org.lwjgl.opengl.GL11.glGetError();
        if (err != org.lwjgl.opengl.GL11.GL_NO_ERROR) {
            LOGGER.warn("GL error {} at {}", err, location);
        }
    }
}
