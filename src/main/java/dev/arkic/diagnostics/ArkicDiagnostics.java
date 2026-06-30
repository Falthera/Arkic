package dev.arkic.diagnostics;

import dev.arkic.renderer.backend.RenderBackend;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArkicDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Diagnostics");
    
    public static void logOpenGLState() {
        StringBuilder sb = new StringBuilder("\n=== Arkic OpenGL State ===\n");
        sb.append("GL_VERSION: ").append(GL11.glGetString(GL11.GL_VERSION)).append("\n");
        sb.append("GL_VENDOR: ").append(GL11.glGetString(GL11.GL_VENDOR)).append("\n");
        sb.append("GL_RENDERER: ").append(GL11.glGetString(GL11.GL_RENDERER)).append("\n");
        sb.append("GL_SHADING_LANGUAGE_VERSION: ").append(GL11.glGetString(0x8B8C)).append("\n");
        
        sb.append("MAX_TEXTURE_SIZE: ").append(GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE)).append("\n");
        sb.append("MAX_VERTEX_ATTRIBS: ").append(GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS)).append("\n");
        sb.append("MAX_VARYING_VECTORS: ").append(GL11.glGetInteger(GL20.GL_MAX_VARYING_VECTORS)).append("\n");
        sb.append("MAX_UNIFORM_BLOCK_SIZE: ").append(GL11.glGetInteger(GL43.GL_MAX_UNIFORM_BLOCK_SIZE)).append("\n");
        sb.append("MAX_SHADER_STORAGE_BLOCK_SIZE: ").append(GL11.glGetInteger(GL43.GL_MAX_SHADER_STORAGE_BLOCK_SIZE)).append("\n");
        sb.append("MAX_COMPUTE_WORK_GROUP_INVOCATIONS: ").append(GL11.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS)).append("\n");
        sb.append("MAX_COMPUTE_WORK_GROUP_SIZE_X: ").append(GL11.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE_X)).append("\n");
        sb.append("MAX_COMPUTE_SHARED_MEMORY_SIZE: ").append(GL11.glGetInteger(GL43.GL_MAX_COMPUTE_SHARED_MEMORY_SIZE)).append("\n");
        
        sb.append("==========================\n");
        LOGGER.info(sb.toString());
    }
    
    public static void logBackendStatus(RenderBackend backend) {
        if (backend == null) {
            LOGGER.warn("Backend: no active backend (Sodium fallback active)");
            return;
        }
        LOGGER.info("Active backend: {} | Initialized: {}", backend.name(), backend.isInitialized());
    }
}
