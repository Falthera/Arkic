package dev.arkic;

import dev.arkic.crash.ArkicCrashHandler;
import dev.arkic.renderer.backend.SodiumFallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Arkic {
    public static final String MOD_ID = "arkic";
    public static final String MOD_NAME = "Arkic";
    public static final String VERSION = "0.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static boolean initialized = false;
    private static boolean safeMode = false;
    
    public static void init() {
        if (initialized) return;
        
        LOGGER.info("{} v{} initializing — GPU-driven rendering backend for Sodium", MOD_NAME, VERSION);
        
        if (Boolean.getBoolean("arkic.safemode")) {
            LOGGER.warn("Safe mode enabled via system property");
            safeMode = true;
        }
        
        initialized = true;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static boolean isSafeMode() {
        return safeMode;
    }
    
    public static void enterSafeMode(String reason) {
        safeMode = true;
        LOGGER.warn("Entering safe mode: {}", reason);
    }
    
    public static void exitSafeMode() {
        safeMode = false;
        LOGGER.info("Exiting safe mode");
    }
}
