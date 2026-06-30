package dev.arkic.renderer;

import dev.arkic.renderer.backend.*;
import dev.arkic.renderer.gl.GlCapabilities;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class ArkicRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Renderer");
    private static RenderBackend activeBackend;
    private static boolean initialized = false;

    public static boolean initialize() {
        if (initialized) return isActive();
        
        GlCapabilities caps = GlCapabilities.detect();
        LOGGER.info("OpenGL version: {} {}", caps.glVersion, caps.glVendor);
        LOGGER.info("GLSL version: {}", caps.glslVersion);
        LOGGER.info("Extensions: compute={}, ssbo={}, mdi={}, atomic={}, texFetch={}",
            caps.hasComputeShaders, caps.hasSSBOSupport,
            caps.hasMultiDrawIndirect, caps.hasAtomicCounters,
            caps.hasTextureFetch);

        activeBackend = selectBackend(caps);
        if (activeBackend == null) {
            LOGGER.warn("No suitable GPU backend found. Falling back to Sodium.");
            return false;
        }

        try {
            boolean ok = activeBackend.initialize(caps);
            if (ok) {
                initialized = true;
                LOGGER.info("Arkic backend ready: {}", activeBackend.name().toLowerCase(Locale.ROOT));
                return true;
            } else {
                LOGGER.error("Backend {} failed to initialize", activeBackend.name());
                activeBackend.close();
                activeBackend = null;
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Exception during backend init", e);
            if (activeBackend != null) activeBackend.close();
            activeBackend = null;
            return false;
        }
    }

    private static RenderBackend selectBackend(GlCapabilities caps) {
        if (caps.supportsAdvancedBackend()) {
            return new dev.arkic.renderer.backend.AdvancedBackend();
        } else if (caps.supportsComputeBackend()) {
            return new dev.arkic.renderer.backend.ComputeBackend();
        } else if (caps.supportsCompatBackend()) {
            return new dev.arkic.renderer.backend.CompatBackend();
        }
        return null;
    }

    public static boolean isActive() {
        return initialized && activeBackend != null;
    }

    public static RenderBackend getBackend() {
        return activeBackend;
    }

    public static boolean hasComputeShaders() {
        return activeBackend != null && activeBackend.capabilities().hasComputeShaders;
    }

    public static boolean hasSSBOSupport() {
        return activeBackend != null && activeBackend.capabilities().hasSSBOSupport;
    }

    public static boolean hasMultiDrawIndirect() {
        return activeBackend != null && activeBackend.capabilities().hasMultiDrawIndirect;
    }

    public static void renderWorld(float tickDelta) {
        if (!isActive()) return;
        try {
            activeBackend.renderWorld(tickDelta);
        } catch (Exception e) {
            LOGGER.error("Error during world render", e);
        }
    }

    public static void beginFrame() {
        if (!isActive()) return;
        activeBackend.beginFrame();
    }

    public static void endFrame() {
        if (!isActive()) return;
        activeBackend.endFrame();
    }

    public static void shutdown() {
        if (activeBackend != null) {
            activeBackend.close();
            activeBackend = null;
            initialized = false;
        }
    }
}
