package dev.arkic.renderer.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public final class GlCapabilities {
    public final int glMajor;
    public final int glMinor;
    public final String glVersion;
    public final String glVendor;
    public final String glRenderer;
    public final String glslVersion;
    
    public final boolean hasComputeShaders;
    public final boolean hasSSBOSupport;
    public final boolean hasMultiDrawIndirect;
    public final boolean hasAtomicCounters;
    public final boolean hasTextureFetch;
    public final boolean hasShaderImageLoadStore;
    public final boolean hasSparseBuffers;
    public final boolean hasArbBufferStorage;
    public final boolean hasArbDirectStateAccess;
    public final boolean hasArbTextureStorage;

    private GlCapabilities(int major, int minor, String version, String vendor, String renderer, String glsl) {
        this.glMajor = major;
        this.glMinor = minor;
        this.glVersion = version;
        this.glVendor = vendor;
        this.glRenderer = renderer;
        this.glslVersion = glsl;
        
        this.hasComputeShaders = isExtensionSupported("GL_ARB_compute_shader") || major >= 43;
        this.hasSSBOSupport = isExtensionSupported("GL_ARB_shader_storage_buffer_object") || major >= 43;
        this.hasMultiDrawIndirect = isExtensionSupported("GL_ARB_multi_draw_indirect") || major >= 43;
        this.hasAtomicCounters = isExtensionSupported("GL_ARB_atomic_counters") || major >= 42;
        this.hasTextureFetch = isExtensionSupported("GL_ARB_texture_multisample") || major >= 32;
        this.hasShaderImageLoadStore = isExtensionSupported("GL_ARB_shader_image_load_store") || major >= 42;
        this.hasSparseBuffers = isExtensionSupported("GL_ARB_sparse_buffer") || major >= 44;
        this.hasArbBufferStorage = isExtensionSupported("GL_ARB_buffer_storage");
        this.hasArbDirectStateAccess = isExtensionSupported("GL_ARB_direct_state_access") || major >= 45;
        this.hasArbTextureStorage = isExtensionSupported("GL_ARB_texture_storage") || major >= 42;
    }

    public static GlCapabilities detect() {
        String version = GL11.glGetString(GL11.GL_VERSION);
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        String glsl = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        
        int major, minor;
        try {
            String[] parts = version.split("\\.");
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1].split(" ")[0]);
        } catch (Exception e) {
            major = 0;
            minor = 0;
        }
        
        return new GlCapabilities(major, minor, version, vendor, renderer, glsl);
    }

    private static boolean isExtensionSupported(String ext) {
        String extensions = GL11.glGetString(GL11.GL_EXTENSIONS);
        return extensions != null && extensions.contains(ext);
    }

    public boolean supportsAdvancedBackend() {
        return (glMajor > 4 || (glMajor == 4 && glMinor >= 6)) &&
               hasComputeShaders && hasSSBOSupport && hasMultiDrawIndirect;
    }

    public boolean supportsComputeBackend() {
        return hasComputeShaders && hasSSBOSupport;
    }

    public boolean supportsCompatBackend() {
        return glMajor >= 3 || (glMajor == 3 && glMinor >= 3);
    }

    public boolean isIntelArc() {
        String gpu = glRenderer.toLowerCase(Locale.ROOT);
        return gpu.contains("intel") && (gpu.contains("arc") || gpu.contains("a770") || gpu.contains("a750"));
    }

    public boolean isIntelIntegrated() {
        String gpu = glRenderer.toLowerCase(Locale.ROOT);
        return gpu.contains("intel") && (gpu.contains("uhd") || gpu.contains("iris"));
    }

    public boolean isAMD() {
        String gpu = glRenderer.toLowerCase(Locale.ROOT);
        return gpu.contains("amd") || gpu.contains("radeon");
    }

    public boolean isNVIDIA() {
        String gpu = glRenderer.toLowerCase(Locale.ROOT);
        return gpu.contains("nvidia") || gpu.contains("geforce");
    }

    public String recommendedBackend() {
        if (supportsAdvancedBackend()) return "advanced";
        if (supportsComputeBackend()) return "compute";
        if (supportsCompatBackend()) return "compat";
        return "sodium_fallback";
    }
}
