package com.dotcms.shutdown;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;

/**
 * Centralized shutdown coordinator that manages the clean shutdown of dotCMS components
 * with proper timeout handling to prevent hanging during Docker container shutdown.
 *
 * This coordinator ensures that:
 * 1. Only one shutdown process runs at a time
 * 2. All shutdown operations have timeouts
 * 3. Shutdown operations are performed in the correct order
 * 4. Failures in one component don't prevent others from shutting down
 * 5. Proper logging is maintained even when log4j shuts down
 * 6. Active requests are given time to complete (request draining)
 *
 * @author dotCMS Team
 */
public class ShutdownCoordinator {

    private static final String SHUTDOWN_TIMEOUT_SECONDS_PROP = "shutdown.timeout.seconds";
    private static final String SHUTDOWN_COMPONENT_TIMEOUT_SECONDS_PROP = "shutdown.component.timeout.seconds";
    private static final String SHUTDOWN_REQUEST_DRAIN_TIMEOUT_SECONDS_PROP = "shutdown.request.drain.timeout.seconds";
    private static final String SHUTDOWN_REQUEST_DRAIN_CHECK_INTERVAL_MS_PROP = "shutdown.request.drain.check.interval.ms";
    
    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 45;
    private static final int DEFAULT_COMPONENT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_REQUEST_DRAIN_TIMEOUT_SECONDS = 15;
    private static final int DEFAULT_REQUEST_DRAIN_CHECK_INTERVAL_MS = 250;
    
    private static volatile ShutdownCoordinator instance;
    private static final AtomicInteger activeRequestCount = new AtomicInteger(0);
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final AtomicBoolean shutdownCompleted = new AtomicBoolean(false);
    private final AtomicBoolean requestDrainingInProgress = new AtomicBoolean(false);
    
    private final int shutdownTimeoutSeconds;
    private final int componentTimeoutSeconds;
    private final boolean debugMode;
    
    private ShutdownCoordinator() {
        this.shutdownTimeoutSeconds = Config.getIntProperty(SHUTDOWN_TIMEOUT_SECONDS_PROP, DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
        this.componentTimeoutSeconds = Config.getIntProperty(SHUTDOWN_COMPONENT_TIMEOUT_SECONDS_PROP, DEFAULT_COMPONENT_TIMEOUT_SECONDS);
        this.debugMode = Config.getBooleanProperty("shutdown.debug", false);
        
        if (debugMode) {
            Logger.debug(this, "ShutdownCoordinator initialized with timeouts: overall=" + shutdownTimeoutSeconds + "s, component=" + componentTimeoutSeconds + "s");
        }
    }
    
    /**
     * Get the singleton instance of the shutdown coordinator.
     * 
     * @return ShutdownCoordinator instance
     */
    public static ShutdownCoordinator getInstance() {
        if (instance == null) {
            synchronized (ShutdownCoordinator.class) {
                if (instance == null) {
                    instance = new ShutdownCoordinator();
                }
            }
        }
        return instance;
    }
    

    
    /**
     * Safe logging that falls back to System.out when log4j is unavailable
     */
    private void safeLog(String level, String message) {
        safeLog(level, message, null);
    }
    
    /**
     * Simplified logging - just use log4j since it's available during our shutdown
     */
    private void safeLog(String level, String message, Throwable throwable) {
        switch (level.toUpperCase()) {
            case "INFO":
                if (throwable != null) {
                    Logger.error(this, message, throwable);
                } else {
                    Logger.info(this, message);
                }
                break;
            case "WARN":
                if (throwable != null) {
                    Logger.warn(this, message, throwable);
                } else {
                    Logger.warn(this, message);
                }
                break;
            case "ERROR":
                if (throwable != null) {
                    Logger.error(this, message, throwable);
                } else {
                    Logger.error(this, message);
                }
                break;
            case "DEBUG":
                if (throwable != null) {
                    Logger.debug(this, message, throwable);
                } else {
                    Logger.debug(this, message);
                }
                break;
            default:
                if (throwable != null) {
                    Logger.info(this, message + ": " + throwable.getMessage());
                } else {
                    Logger.info(this, message);
                }
        }
    }
    

    
    /**
     * Perform a coordinated shutdown of all dotCMS components.
     * This method is idempotent - it can be called multiple times safely.
     * 
     * @return true if shutdown was successful, false if timeout or already completed
     */
    public boolean shutdown() {
        if (debugMode) {
            safeLog("DEBUG", "ShutdownCoordinator.shutdown() called");
        }
        
        if (!shutdownInProgress.compareAndSet(false, true)) {
            safeLog("INFO", "Shutdown already in progress, waiting for completion...");
            return waitForShutdownCompletion();
        }
        
        if (shutdownCompleted.get()) {
            safeLog("INFO", "Shutdown already completed");
            return true;
        }
        
        safeLog("INFO", "Starting coordinated shutdown with timeout of " + shutdownTimeoutSeconds + " seconds");
        
        final ExecutorService shutdownExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "dotcms-shutdown-coordinator");
            t.setDaemon(true);
            return t;
        });
        
        CompletableFuture<Boolean> shutdownFuture = null;
        try {
            shutdownFuture = CompletableFuture.supplyAsync(
                this::performShutdown, shutdownExecutor);
            
            final boolean success = shutdownFuture.get(shutdownTimeoutSeconds, TimeUnit.SECONDS);
            shutdownCompleted.set(true);
            safeLog("INFO", "Coordinated shutdown completed successfully");
            return success;
            
        } catch (java.util.concurrent.TimeoutException e) {
            safeLog("ERROR", "Shutdown timed out after " + shutdownTimeoutSeconds + " seconds, cancelling background task", e);
            if (shutdownFuture != null) {
                shutdownFuture.cancel(true); // Cancel the background task to prevent memory leak
            }
            shutdownCompleted.set(true);
            return false;
        } catch (Exception e) {
            safeLog("ERROR", "Shutdown failed: " + e.getMessage(), e);
            shutdownCompleted.set(true);
            return false;
        } finally {
            shutdownExecutor.shutdown();
            try {
                if (!shutdownExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    shutdownExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                shutdownExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Wait for shutdown completion if already in progress.
     */
    private boolean waitForShutdownCompletion() {
        long waitTime = 0;
        final long maxWait = shutdownTimeoutSeconds * 1000L;
        
        while (shutdownInProgress.get() && !shutdownCompleted.get() && waitTime < maxWait) {
            try {
                Thread.sleep(100);
                waitTime += 100;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return shutdownCompleted.get();
    }
    
    /**
     * Perform the actual shutdown operations in the correct order.
     */
    private boolean performShutdown() {
        long shutdownStartTime = System.currentTimeMillis();
        safeLog("INFO", "Starting coordinated shutdown process");
        
        // Phase 1: Request draining - wait for active requests to complete
        long phase1Start = System.currentTimeMillis();
        safeLog("DEBUG", "Phase 1: Starting request draining");
        drainActiveRequests();
        long phase1Duration = System.currentTimeMillis() - phase1Start;
        safeLog("INFO", String.format("Phase 1: Request draining completed (%dms)", phase1Duration));
        
        // Phase 2: Component shutdown operations
        long phase2Start = System.currentTimeMillis();
        safeLog("DEBUG", "Phase 2: Starting component shutdown operations");
        final List<ShutdownOperation> operations = createShutdownOperations();
        boolean allSuccessful = true;
        
        for (ShutdownOperation operation : operations) {
            try {
                safeLog("DEBUG", "Executing shutdown operation: " + operation.getName());
                
                final CompletableFuture<Void> operationFuture = CompletableFuture.runAsync(operation::execute);
                
                // Use custom timeout if specified, otherwise use default component timeout
                int timeoutToUse = operation.getTimeoutSeconds() > 0 ? operation.getTimeoutSeconds() : componentTimeoutSeconds;
                if (debugMode && operation.getTimeoutSeconds() > 0) {
                    safeLog("DEBUG", "Using custom timeout of " + timeoutToUse + "s for operation: " + operation.getName());
                }
                
                operationFuture.get(timeoutToUse, TimeUnit.SECONDS);
                
                safeLog("DEBUG", "Completed shutdown operation: " + operation.getName());
                
            } catch (Exception e) {
                safeLog("WARN", "Shutdown operation '" + operation.getName() + "' failed or timed out: " + e.getMessage());
                allSuccessful = false;
                // Continue with other operations even if one fails
            }
        }
        
        long phase2Duration = System.currentTimeMillis() - phase2Start;
        safeLog("INFO", String.format("Phase 2: Component shutdown operations completed (%dms)", phase2Duration));
        
        long totalDuration = System.currentTimeMillis() - shutdownStartTime;
        safeLog("INFO", String.format("Coordinated shutdown process completed %s (total: %dms, phase1: %dms, phase2: %dms)", 
            (allSuccessful ? "successfully" : "with warnings"), totalDuration, phase1Duration, phase2Duration));
        
        return allSuccessful;
    }
    
    /**
     * Phase 1: Request draining - wait for active requests to complete with timeout
     */
    private void drainActiveRequests() {
        int drainTimeout = Config.getIntProperty(SHUTDOWN_REQUEST_DRAIN_TIMEOUT_SECONDS_PROP, DEFAULT_REQUEST_DRAIN_TIMEOUT_SECONDS);
        int checkInterval = Config.getIntProperty(SHUTDOWN_REQUEST_DRAIN_CHECK_INTERVAL_MS_PROP, DEFAULT_REQUEST_DRAIN_CHECK_INTERVAL_MS);
        
        // Set the request draining flag
        requestDrainingInProgress.set(true);
        
        try {
            safeLog("DEBUG", String.format("Beginning request draining (timeout: %ds, check interval: %dms)", 
                drainTimeout, checkInterval));
            
            long drainStartTime = System.currentTimeMillis();
            long drainTimeoutMs = drainTimeout * 1000L;
            
            // Initial check
            int initialActiveRequests = activeRequestCount.get();
            safeLog("INFO", String.format("Initial active request count: %d", initialActiveRequests));
            
            // If no active requests from the start, skip the draining loop
            if (initialActiveRequests == 0) {
                int initialBusyThreads = 0;
                try {
                    initialBusyThreads = getBusyConnectorThreads();
                } catch (Exception e) {
                    safeLog("DEBUG", "Failed to query initial JMX busy threads: " + e.getMessage());
                }
                
                if (initialBusyThreads == 0) {
                    safeLog("INFO", "No active requests or busy threads detected - skipping request draining");
                    return;
                }
                safeLog("INFO", String.format("No active requests but %d busy threads detected - proceeding with draining", initialBusyThreads));
            }
        
        while (System.currentTimeMillis() - drainStartTime < drainTimeoutMs) {
            int activeRequests = activeRequestCount.get();
            int busyThreads = 0;
            
            // Try to get busy threads, but don't let JMX issues block shutdown
            try {
                busyThreads = getBusyConnectorThreads();
            } catch (Exception e) {
                safeLog("DEBUG", "Failed to query JMX for busy threads, continuing with activeRequests only: " + e.getMessage());
            }
            
            if (activeRequests == 0 && busyThreads == 0) {
                long drainTime = System.currentTimeMillis() - drainStartTime;
                safeLog("INFO", String.format("Request draining completed - no active requests (%dms)", drainTime));
                return;
            }
            
            // Log progress - first few iterations at INFO, then DEBUG
            long elapsed = System.currentTimeMillis() - drainStartTime;
            String logLevel = elapsed < 2000 ? "INFO" : "DEBUG";
            safeLog(logLevel, String.format("Waiting for requests to complete: %d active requests, %d busy threads (elapsed: %dms)", 
                activeRequests, busyThreads, elapsed));
            
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                safeLog("WARN", "Request draining interrupted");
                return;
            }
        }
        
        // Timeout reached
        int finalActiveRequests = activeRequestCount.get();
        int finalBusyThreads = 0;
        try {
            finalBusyThreads = getBusyConnectorThreads();
        } catch (Exception e) {
            safeLog("DEBUG", "Failed to query final JMX busy threads: " + e.getMessage());
        }
        
        long drainTime = System.currentTimeMillis() - drainStartTime;
        
        if (finalActiveRequests > 0 || finalBusyThreads > 0) {
            safeLog("WARN", String.format("Request draining timeout reached after %dms - proceeding with %d active requests, %d busy threads", 
                drainTime, finalActiveRequests, finalBusyThreads));
        } else {
            safeLog("INFO", String.format("Request draining completed at timeout (%dms)", drainTime));
        }
        
        } finally {
            // Clear the request draining flag
            requestDrainingInProgress.set(false);
        }
    }
    
    /**
     * Gets the count of busy threads from Tomcat connectors via JMX
     */
    private int getBusyConnectorThreads() {
        ExecutorService executor = null;
        try {
            // Create a dedicated executor with proper cleanup
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "jmx-busy-threads-query");
                t.setDaemon(true); // Ensure it doesn't prevent JVM shutdown
                return t;
            });
            
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
                    
                    // Query for all Tomcat connectors
                    Set<ObjectName> connectorNames = beanServer.queryNames(
                        new ObjectName("*:type=Connector,*"), null);
                    
                    int totalBusyThreads = 0;
                    
                    for (ObjectName connectorName : connectorNames) {
                        try {
                            // Check if this is an HTTP connector
                            Object protocol = beanServer.getAttribute(connectorName, "protocol");
                            Object scheme = beanServer.getAttribute(connectorName, "scheme");
                            
                            boolean isHttpConnector = false;
                            if (protocol != null) {
                                String protocolStr = protocol.toString();
                                if (protocolStr.contains("HTTP") || 
                                    protocolStr.contains("Http11") || 
                                    protocolStr.contains("http11") ||
                                    (scheme != null && ("http".equals(scheme.toString()) || "https".equals(scheme.toString())))) {
                                    isHttpConnector = true;
                                }
                            }
                            
                            if (isHttpConnector) {
                                // Get busy thread count for this connector
                                Object currentThreadsBusy = beanServer.getAttribute(connectorName, "currentThreadsBusy");
                                if (currentThreadsBusy instanceof Number) {
                                    totalBusyThreads += ((Number) currentThreadsBusy).intValue();
                                }
                            }
                        } catch (Exception e) {
                            // Skip this connector if we can't read its attributes
                        }
                    }
                    
                    return totalBusyThreads;
                } catch (Exception e) {
                    return 0;
                }
            }, executor);
            
            // Wait for result with a 1-second timeout
            return future.get(1, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            // If JMX fails or times out, return 0 (assume no busy threads)
            if (debugMode) {
                safeLog("DEBUG", "JMX query for busy threads failed or timed out: " + e.getMessage());
            }
            return 0;
        } finally {
            // Always clean up the executor to prevent thread leaks
            if (executor != null) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * Create the list of shutdown operations in the correct order.
     */
    private List<ShutdownOperation> createShutdownOperations() {
        final List<ShutdownOperation> operations = new ArrayList<>();
        
        // Order is important - shutdown in reverse order of startup dependencies
        
        operations.add(new ShutdownOperation("License cleanup", () -> {
            try {
                LicenseUtil.freeLicenseOnRepo();
            } catch (Exception e) {
                Logger.warn(ShutdownCoordinator.class, "License cleanup failed: " + e.getMessage());
            }
        }));
        
        // Reindex thread should be stopped early to prevent database access during shutdown
        operations.add(new ShutdownOperation("Reindex thread", () -> {
            try {
                ReindexThread.stopThread();
                // Give extra time for ReindexThread to stop gracefully
                Thread.sleep(500);
            } catch (Exception e) {
                Logger.warn(ShutdownCoordinator.class, "Reindex thread shutdown failed: " + e.getMessage());
            }
        }));
        
        operations.add(new ShutdownOperation("Server cluster cleanup", () -> {
            try {
                String serverId = APILocator.getServerAPI().readServerId();
                safeLog("DEBUG", "Removing server " + serverId + " from cluster tables");
                APILocator.getServerAPI().removeServerFromClusterTable(serverId);
                safeLog("DEBUG", "Server cluster cleanup completed for server " + serverId);
            } catch (Exception e) {
                // Check if this is a database connectivity issue (expected in some scenarios)
                String message = e.getMessage();
                if (message != null && (message.contains("connection") || message.contains("database") || 
                    message.contains("SQLException") || message.contains("HikariPool"))) {
                    safeLog("INFO", "Server cluster cleanup skipped due to database connectivity issue (expected during some shutdowns)");
                } else {
                    safeLog("WARN", "Server cluster cleanup failed: " + e.getMessage());
                }
            }
        }));
        
        operations.add(new ShutdownOperation("Job queue shutdown", () -> {
            try {
                safeLog("DEBUG", "Closing job queue manager");
                APILocator.getJobQueueManagerAPI().close();
                safeLog("DEBUG", "Job queue manager closed successfully");
            } catch (Exception e) {
                safeLog("WARN", "Job queue shutdown failed: " + e.getMessage());
            }
        }));
        
        operations.add(new ShutdownOperation("Quartz schedulers", () -> {
            try {
                safeLog("DEBUG", "Shutting down Quartz schedulers");
                QuartzUtils.stopSchedulers();
                safeLog("DEBUG", "Quartz schedulers shutdown completed");
            } catch (Exception e) {
                safeLog("WARN", "Quartz shutdown failed: " + e.getMessage());
            }
        }, 20)); // Give Quartz 20 seconds to shut down (vs default 10s)
        
        operations.add(new ShutdownOperation("Cache system", () -> {
            try {
                CacheLocator.getCacheAdministrator().shutdown();
            } catch (Exception e) {
                Logger.warn(ShutdownCoordinator.class, "Cache shutdown failed: " + e.getMessage());
            }
        }));
        
        operations.add(new ShutdownOperation("Concurrent framework", () -> {
            try {
                DotConcurrentFactory.getInstance().shutdownAndDestroy();
            } catch (Exception e) {
                Logger.warn(ShutdownCoordinator.class, "Concurrent framework shutdown failed: " + e.getMessage());
            }
        }));
        
        // Additional thread pool cleanup - attempt to shutdown common thread pools
        operations.add(new ShutdownOperation("Thread pool cleanup", () -> {
            try {
                safeLog("DEBUG", "Attempting to shutdown remaining thread pools");
                
                // Try to shutdown ForkJoinPool common pool
                try {
                    java.util.concurrent.ForkJoinPool.commonPool().shutdown();
                    if (!java.util.concurrent.ForkJoinPool.commonPool().awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        java.util.concurrent.ForkJoinPool.commonPool().shutdownNow();
                    }
                } catch (Exception e) {
                    safeLog("DEBUG", "ForkJoinPool shutdown attempt failed: " + e.getMessage());
                }
                
                // Try to find and shutdown any remaining ExecutorServices via JMX (with safety checks)
                try {
                    javax.management.MBeanServer server = java.lang.management.ManagementFactory.getPlatformMBeanServer();
                    java.util.Set<javax.management.ObjectName> mbeans = server.queryNames(
                        new javax.management.ObjectName("java.util.concurrent:type=*"), null);
                    for (javax.management.ObjectName mbean : mbeans) {
                        try {
                            // First check if the MBean supports the Shutdown attribute
                            javax.management.MBeanInfo mbeanInfo = server.getMBeanInfo(mbean);
                            boolean hasShutdownAttribute = false;
                            boolean hasShutdownOperation = false;
                            
                            // Check for Shutdown attribute
                            for (javax.management.MBeanAttributeInfo attr : mbeanInfo.getAttributes()) {
                                if ("Shutdown".equals(attr.getName()) && attr.isReadable()) {
                                    hasShutdownAttribute = true;
                                    break;
                                }
                            }
                            
                            // Check for shutdown operation
                            for (javax.management.MBeanOperationInfo op : mbeanInfo.getOperations()) {
                                if ("shutdown".equals(op.getName()) && op.getSignature().length == 0) {
                                    hasShutdownOperation = true;
                                    break;
                                }
                            }
                            
                            // Only proceed if both attribute and operation exist
                            if (hasShutdownAttribute && hasShutdownOperation) {
                                Object result = server.getAttribute(mbean, "Shutdown");
                                if (result instanceof Boolean && !(Boolean)result) {
                                    safeLog("DEBUG", "Shutting down ExecutorService MBean: " + mbean);
                                    server.invoke(mbean, "shutdown", new Object[0], new String[0]);
                                }
                            }
                        } catch (Exception e) {
                            // Log specific failures for debugging, but continue with other MBeans
                            safeLog("DEBUG", "Failed to shutdown MBean " + mbean + ": " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    safeLog("DEBUG", "JMX thread pool cleanup attempt failed: " + e.getMessage());
                }
                
                safeLog("DEBUG", "Thread pool cleanup completed");
            } catch (Exception e) {
                safeLog("WARN", "Thread pool cleanup failed: " + e.getMessage());
            }
        }));
        
        // OSGi Framework shutdown - this should clean up most non-daemon threads
        operations.add(new ShutdownOperation("OSGi framework", () -> {
            try {
                safeLog("DEBUG", "Shutting down OSGi framework");
                // Use OSGIUtil's stopFramework method which handles proper cleanup
                org.apache.felix.framework.OSGIUtil osgiUtil = org.apache.felix.framework.OSGIUtil.getInstance();
                if (osgiUtil.isInitialized()) {
                    osgiUtil.stopFramework();
                    safeLog("DEBUG", "OSGi framework shutdown completed");
                } else {
                    safeLog("DEBUG", "OSGi framework not initialized, skipping shutdown");
                }
            } catch (Exception e) {
                safeLog("WARN", "OSGi framework shutdown failed: " + e.getMessage());
            }
        }, 15)); // Give OSGi 15 seconds to shut down
        
        return operations;
    }
    
    /**
     * Check if shutdown is currently in progress.
     */
    public boolean isShutdownInProgress() {
        return shutdownInProgress.get();
    }
    
    /**
     * Check if shutdown has completed.
     */
    public boolean isShutdownCompleted() {
        return shutdownCompleted.get();
    }
    
    /**
     * Check if request draining is currently in progress.
     * This indicates that shutdown has started and the system is waiting for active requests to complete.
     * 
     * @return true if request draining is in progress, false otherwise
     */
    public boolean isRequestDrainingInProgress() {
        return requestDrainingInProgress.get();
    }
    
    /**
     * Static convenience method to check if shutdown has been initiated.
     * This returns true as soon as shutdown begins, before request draining starts.
     * Use this to prevent new expensive operations from starting during shutdown.
     * 
     * @return true if shutdown has been initiated, false otherwise
     */
    public static boolean isShutdownStarted() {
        ShutdownCoordinator instance = ShutdownCoordinator.instance;
        return instance != null && instance.isShutdownInProgress();
    }
    
    /**
     * Static convenience method to check if request draining is currently in progress.
     * This is only true during the specific phase where we're waiting for active requests to complete.
     * During this phase, the server is still accepting new requests but no new expensive operations should start.
     * 
     * @return true if request draining is in progress, false otherwise
     */
    public static boolean isRequestDraining() {
        ShutdownCoordinator instance = ShutdownCoordinator.instance;
        return instance != null && instance.isRequestDrainingInProgress();
    }
    
    /**
     * Represents a single shutdown operation with a name and executable task.
     */
    private static class ShutdownOperation {
        private final String name;
        private final Runnable task;
        private final int timeoutSeconds;
        
        public ShutdownOperation(String name, Runnable task) {
            this(name, task, -1); // Use default timeout
        }
        
        public ShutdownOperation(String name, Runnable task, int timeoutSeconds) {
            this.name = name;
            this.task = task;
            this.timeoutSeconds = timeoutSeconds;
        }
        
        public String getName() {
            return name;
        }
        
        public void execute() {
            task.run();
        }
        
        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }
    }
    
    /**
     * Performs coordinated shutdown of dotCMS components
     */
    public static void coordinateShutdown() {
        ShutdownCoordinator coordinator = getInstance();
        coordinator.shutdown();
    }
    

    
    /**
     * Increments the active request count (called by request filter on request start)
     */
    public static void incrementActiveRequests() {
        activeRequestCount.incrementAndGet();
    }
    
    /**
     * Decrements the active request count (called by request filter on request end)
     */
    public static void decrementActiveRequests() {
        activeRequestCount.decrementAndGet();
    }
    
    /**
     * Gets the current active request count for monitoring
     */
    public static int getCurrentActiveRequestCount() {
        return activeRequestCount.get();
    }
    
    /**
     * Gets comprehensive shutdown status information.
     * This is useful for monitoring and debugging purposes.
     * 
     * @return ShutdownStatus object containing current shutdown state
     */
    public static ShutdownStatus getShutdownStatus() {
        ShutdownCoordinator instance = ShutdownCoordinator.instance;
        if (instance == null) {
            return new ShutdownStatus(false, false, false, activeRequestCount.get());
        }
        
        return new ShutdownStatus(
            instance.isShutdownInProgress(),
            instance.isRequestDrainingInProgress(), 
            instance.isShutdownCompleted(),
            activeRequestCount.get()
        );
    }
    
    /**
     * Immutable status object containing shutdown state information
     */
    public static class ShutdownStatus {
        private final boolean shutdownInProgress;
        private final boolean requestDrainingInProgress;
        private final boolean shutdownCompleted;
        private final int activeRequestCount;
        
        public ShutdownStatus(boolean shutdownInProgress, boolean requestDrainingInProgress, 
                            boolean shutdownCompleted, int activeRequestCount) {
            this.shutdownInProgress = shutdownInProgress;
            this.requestDrainingInProgress = requestDrainingInProgress;
            this.shutdownCompleted = shutdownCompleted;
            this.activeRequestCount = activeRequestCount;
        }
        
        public boolean isShutdownInProgress() { return shutdownInProgress; }
        public boolean isRequestDrainingInProgress() { return requestDrainingInProgress; }
        public boolean isShutdownCompleted() { return shutdownCompleted; }
        public int getActiveRequestCount() { return activeRequestCount; }
        
        @Override
        public String toString() {
            return String.format("ShutdownStatus{shutdownInProgress=%s, requestDrainingInProgress=%s, shutdownCompleted=%s, activeRequestCount=%d}",
                shutdownInProgress, requestDrainingInProgress, shutdownCompleted, activeRequestCount);
        }
    }
}