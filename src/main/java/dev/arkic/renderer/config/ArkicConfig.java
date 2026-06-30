package dev.arkic.renderer.config;

import dev.arkic.renderer.gl.GlCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class ArkicConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Arkic/Config");
    private static final File CONFIG_FILE = new File(System.getProperty("user.home"), ".arkic/arkic.properties");
    private static final Properties properties = new Properties();
    
    public static boolean DEBUG_OPENGL = false;
    public static boolean ENABLE_GPU_CULLING = true;
    public static boolean ENABLE_SSBO_UPLOADS = true;
    public static boolean ENABLE_MULTI_DRAW_INDIRECT = true;
    public static boolean ENABLE_PERSISTENT_MAPPING = true;
    public static boolean VERBOSE_LOGGING = false;
    public static boolean DISABLE_ON_CRASH = true;
    public static int MAX_CHUNK_UPLOADS_PER_FRAME = 256;
    public static long MAX_SSBO_SIZE_BYTES = 512L * 1024 * 1024;
    public static int CHUNK_SECTION_SIZE = 1 * 1024 * 1024;
    public static boolean FORCE_COMPAT_MODE = false;
    public static boolean FORCE_ADVANCED_MODE = false;
    public static int RENDER_DISTANCE_OVERRIDE = -1;
    public static String FALLBACK_BACKEND = "sodium";
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        try {
            if (CONFIG_FILE.exists()) {
                try (InputStream is = new FileInputStream(CONFIG_FILE)) {
                    properties.load(is);
                }
                
                DEBUG_OPENGL = Boolean.parseBoolean(getProperty("debug-opengl", "false"));
                ENABLE_GPU_CULLING = Boolean.parseBoolean(getProperty("enable-gpu-culling", "true"));
                ENABLE_SSBO_UPLOADS = Boolean.parseBoolean(getProperty("enable-ssbo", "true"));
                ENABLE_MULTI_DRAW_INDIRECT = Boolean.parseBoolean(getProperty("enable-mdi", "true"));
                ENABLE_PERSISTENT_MAPPING = Boolean.parseBoolean(getProperty("enable-persistent-mapping", "true"));
                VERBOSE_LOGGING = Boolean.parseBoolean(getProperty("verbose-logging", "false"));
                DISABLE_ON_CRASH = Boolean.parseBoolean(getProperty("disable-on-crash", "true"));
                FORCE_COMPAT_MODE = Boolean.parseBoolean(getProperty("force-compat", "false"));
                FORCE_ADVANCED_MODE = Boolean.parseBoolean(getProperty("force-advanced", "false"));
                RENDER_DISTANCE_OVERRIDE = Integer.parseInt(getProperty("render-distance", "-1"));
                MAX_CHUNK_UPLOADS_PER_FRAME = Integer.parseInt(getProperty("max-uploads-per-frame", "256"));
                
                if (VERBOSE_LOGGING) {
                    LOGGER.info("Arkic config loaded from {}", CONFIG_FILE.getAbsolutePath());
                }
            } else {
                saveConfig();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load Arkic config, using defaults", e);
        }
    }
    
    private static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public static void saveConfig() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            properties.setProperty("debug-opengl", String.valueOf(DEBUG_OPENGL));
            properties.setProperty("enable-gpu-culling", String.valueOf(ENABLE_GPU_CULLING));
            properties.setProperty("enable-ssbo", String.valueOf(ENABLE_SSBO_UPLOADS));
            properties.setProperty("enable-mdi", String.valueOf(ENABLE_MULTI_DRAW_INDIRECT));
            properties.setProperty("enable-persistent-mapping", String.valueOf(ENABLE_PERSISTENT_MAPPING));
            properties.setProperty("verbose-logging", String.valueOf(VERBOSE_LOGGING));
            properties.setProperty("disable-on-crash", String.valueOf(DISABLE_ON_CRASH));
            properties.setProperty("force-compat", String.valueOf(FORCE_COMPAT_MODE));
            properties.setProperty("force-advanced", String.valueOf(FORCE_ADVANCED_MODE));
            properties.setProperty("render-distance", String.valueOf(RENDER_DISTANCE_OVERRIDE));
            properties.setProperty("max-uploads-per-frame", String.valueOf(MAX_CHUNK_UPLOADS_PER_FRAME));
            
            try (OutputStream os = new FileOutputStream(CONFIG_FILE)) {
                properties.store(os, "Arkic Configuration");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save Arkic config", e);
        }
    }
}
