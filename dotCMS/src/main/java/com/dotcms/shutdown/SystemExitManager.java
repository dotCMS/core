package com.dotcms.shutdown;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized manager for System.exit() calls that integrates with the coordinated shutdown system.
 * This ensures that all exit scenarios properly trigger coordinated shutdown when possible.
 */
public class SystemExitManager {
    
    private static final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    
    /**
     * Performs a coordinated system exit with proper cleanup.
     * 
     * @param exitCode The exit code (0 = success, non-zero = error)
     * @param reason The reason for the exit
     * @param performCoordinatedShutdown Whether to attempt coordinated shutdown
     */
    public static void exit(int exitCode, String reason, boolean performCoordinatedShutdown) {
        try {
            Logger.info(SystemExitManager.class, "System exit requested: " + reason + " (exit code: " + exitCode + ")");
            
            // Prevent multiple shutdown attempts - use atomic compareAndSet for thread safety
            if (!shutdownInProgress.compareAndSet(false, true)) {
                Logger.warn(SystemExitManager.class, "Shutdown already in progress, forcing immediate halt");
                Runtime.getRuntime().halt(exitCode);
                return;
            }
            
            if (performCoordinatedShutdown && isCoordinatedShutdownAvailable()) {
                Logger.info(SystemExitManager.class, "Attempting coordinated shutdown before exit");
                
                try {
                    ShutdownCoordinator coordinator = ShutdownCoordinator.getInstance();
                    boolean success = coordinator.shutdown();
                    
                    if (success) {
                        Logger.info(SystemExitManager.class, "Coordinated shutdown completed successfully");
                    } else {
                        Logger.warn(SystemExitManager.class, "Coordinated shutdown completed with warnings");
                    }
                } catch (Exception e) {
                    Logger.error(SystemExitManager.class, "Error during coordinated shutdown: " + e.getMessage(), e);
                }
                
                // Give brief time for cleanup, then force exit
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            Logger.info(SystemExitManager.class, "Forcing JVM halt with exit code: " + exitCode);
            Runtime.getRuntime().halt(exitCode);
            
        } catch (Exception e) {
            // Fallback to immediate halt if anything goes wrong
            System.err.println("Error in SystemExitManager: " + e.getMessage());
            e.printStackTrace();
            Runtime.getRuntime().halt(exitCode);
        }
    }
    
    /**
     * Performs a coordinated system exit for normal shutdown scenarios.
     */
    public static void coordinatedExit(int exitCode, String reason) {
        exit(exitCode, reason, true);
    }
    
    /**
     * Performs an immediate system exit for critical failures where coordinated shutdown
     * might not be safe or possible.
     */
    public static void immediateExit(int exitCode, String reason) {
        exit(exitCode, reason, false);
    }
    
    /**
     * Startup failure exit - typically during initialization when coordinated shutdown
     * components may not be available.
     */
    public static void startupFailureExit(String reason) {
        Logger.error(SystemExitManager.class, "Startup failure: " + reason);
        
        // During startup, coordinated shutdown components may not be initialized
        boolean allowCoordinatedShutdown = Config.getBooleanProperty("STARTUP_FAILURE_COORDINATED_SHUTDOWN", false);
        exit(1, "Startup failure: " + reason, allowCoordinatedShutdown);
    }
    
    /**
     * Database connection failure exit - critical infrastructure failure.
     */
    public static void databaseFailureExit(String reason) {
        Logger.error(SystemExitManager.class, "Database failure: " + reason);
        
        // Database failures are critical, but we can still attempt coordinated shutdown
        // since other components might be working
        boolean allowCoordinatedShutdown = Config.getBooleanProperty("DATABASE_FAILURE_COORDINATED_SHUTDOWN", true);
        exit(1, "Database failure: " + reason, allowCoordinatedShutdown);
    }
    
    /**
     * Cluster management exit - requested shutdown from cluster management.
     */
    public static void clusterManagementExit(int exitCode, String reason) {
        Logger.info(SystemExitManager.class, "Cluster management exit: " + reason);
        
        // Cluster management exits should use coordinated shutdown
        coordinatedExit(exitCode, "Cluster management: " + reason);
    }
    
    /**
     * Shutdown-on-startup exit - configured shutdown after startup completion.
     */
    public static void shutdownOnStartupExit(String reason) {
        Logger.info(SystemExitManager.class, "Shutdown-on-startup: " + reason);
        
        // This is a normal shutdown scenario, use coordinated shutdown
        coordinatedExit(0, "Shutdown-on-startup: " + reason);
    }
    
    /**
     * Check if coordinated shutdown is available and safe to use.
     */
    private static boolean isCoordinatedShutdownAvailable() {
        try {
            // Check if we're in a state where coordinated shutdown makes sense
            if (ShutdownCoordinator.isRequestDraining()) {
                return false; // Already shutting down
            }
            
            // Check if basic infrastructure is available
            return ShutdownCoordinator.getInstance() != null;
            
        } catch (Exception e) {
            Logger.debug(SystemExitManager.class, "Coordinated shutdown not available: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if shutdown is currently in progress.
     */
    public static boolean isShutdownInProgress() {
        return shutdownInProgress.get();
    }
} 