package dev.arkic.renderer.shader;

import dev.arkic.renderer.gl.GlCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ShaderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/ShaderManager");
    
    private final GlCapabilities caps;
    private final Map<String, Integer> programs = new HashMap<>();
    private final Map<String, Integer> shaders = new HashMap<>();
    private final Map<String, String> shaderCache = new HashMap<>();
    
    public ShaderManager(GlCapabilities caps) {
        this.caps = caps;
    }
    
    public int createProgram(String name, String vertPath, String fragPath, String computePath) {
        int prog = org.lwjgl.opengl.GL20.glCreateProgram();
        int vertId = -1, fragId = -1, compId = -1;
        
        if (vertPath != null) {
            String src = loadShader(vertPath);
            if (src != null) vertId = compileShader(org.lwjgl.opengl.GL20.GL_VERTEX_SHADER, name + "_vert", src);
        }
        if (fragPath != null) {
            String src = loadShader(fragPath);
            if (src != null) fragId = compileShader(org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER, name + "_frag", src);
        }
        if (computePath != null) {
            String src = loadShader(computePath);
            if (src != null) compId = compileShader(org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER, name + "_comp", src);
        }
        
        if (vertId != -1) org.lwjgl.opengl.GL20.glAttachShader(prog, vertId);
        if (fragId != -1) org.lwjgl.opengl.GL20.glAttachShader(prog, fragId);
        if (compId != -1) org.lwjgl.opengl.GL20.glAttachShader(prog, compId);
        
        org.lwjgl.opengl.GL20.glLinkProgram(prog);
        
        int linked = org.lwjgl.opengl.GL20.glGetProgrami(prog, org.lwjgl.opengl.GL20.GL_LINK_STATUS);
        if (linked == 0) {
            String log = org.lwjgl.opengl.GL20.glGetProgramInfoLog(prog);
            LOGGER.error("Failed to link program {}: {}", name, log);
            org.lwjgl.opengl.GL20.glDeleteProgram(prog);
            return -1;
        }
        
        programs.put(name, prog);
        LOGGER.info("Compiled shader program: {}", name);
        return prog;
    }
    
    private int compileShader(int type, String debugName, String source) {
        int shader = org.lwjgl.opengl.GL20.glCreateShader(type);
        org.lwjgl.opengl.GL20.glShaderSource(shader, source);
        org.lwjgl.opengl.GL20.glCompileShader(shader);
        
        int compiled = org.lwjgl.opengl.GL20.glGetShaderi(shader, org.lwjgl.opengl.GL20.GL_COMPILE_STATUS);
        if (compiled == 0) {
            String log = org.lwjgl.opengl.GL20.glGetShaderInfoLog(shader);
            LOGGER.error("Failed to compile {}: {}", debugName, log);
            org.lwjgl.opengl.GL20.glDeleteShader(shader);
            return -1;
        }
        
        shaders.put(debugName, shader);
        return shader;
    }
    
    private String loadShader(String path) {
        if (shaderCache.containsKey(path)) {
            return shaderCache.get(path);
        }
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            String src = sb.toString();
            shaderCache.put(path, src);
            return src;
        } catch (Exception e) {
            LOGGER.error("Failed to load shader: {}", path, e);
            return null;
        }
    }
    
    public void useProgram(String name) {
        Integer id = programs.get(name);
        if (id != null) org.lwjgl.opengl.GL20.glUseProgram(id);
    }
    
    public int getProgram(String name) {
        return programs.getOrDefault(name, -1);
    }
    
    public int getUniformLocation(String programName, String uniform) {
        Integer prog = programs.get(programName);
        if (prog == null) return -1;
        return org.lwjgl.opengl.GL20.glGetUniformLocation(prog, uniform);
    }
    
    public int getAttribLocation(String programName, String attrib) {
        Integer prog = programs.get(programName);
        if (prog == null) return -1;
        return org.lwjgl.opengl.GL20.glGetAttribLocation(prog, attrib);
    }
    
    public int getBlockIndex(String programName, String blockName) {
        Integer prog = programs.get(programName);
        if (prog == null) return -1;
        return org.lwjgl.opengl.GL43.glGetProgramResourceIndex(prog, org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BLOCK, blockName);
    }
    
    public void bindBlockToBinding(String programName, String blockName, int binding) {
        Integer prog = programs.get(programName);
        if (prog == null) return;
        int index = org.lwjgl.opengl.GL43.glGetProgramResourceIndex(prog, org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BLOCK, blockName);
        if (index != -1) {
            org.lwjgl.opengl.GL43.glShaderStorageBlockBinding(prog, index, binding);
        }
    }
    
    public void close() {
        for (int id : shaders.values()) {
            org.lwjgl.opengl.GL20.glDeleteShader(id);
        }
        shaders.clear();
        for (int id : programs.values()) {
            org.lwjgl.opengl.GL20.glDeleteProgram(id);
        }
        programs.clear();
        shaderCache.clear();
    }
}
