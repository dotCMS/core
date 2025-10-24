package com.dotcms.shutdown;

import com.dotcms.cdi.CDIUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private static final String JMX_BUSY_THREADS_TIMEOUT_SECONDS_PROP = "shutdown.jmx.busy.threads.timeout.seconds";

    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 45;
    private static final int DEFAULT_COMPONENT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_REQUEST_DRAIN_TIMEOUT_SECONDS = 15;
    private static final int DEFAULT_REQUEST_DRAIN_CHECK_INTERVAL_MS = 250;
    private static final int DEFAULT_JMX_BUSY_THREADS_TIMEOUT_SECONDS = 1;

    private static volatile ShutdownCoordinator instance;
    private static final AtomicInteger activeRequestCount = new AtomicInteger(0);
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final AtomicBoolean shutdownCompleted = new AtomicBoolean(false);
    private final AtomicBoolean requestDrainingInProgress = new AtomicBoolean(false);
    
    private final int shutdownTimeoutSeconds;
    private final int componentTimeoutSeconds;
    private final int jmxBusyThreadsTimeoutSeconds;
    private final boolean debugMode;
    
    private ShutdownCoordinator() {
        this.shutdownTimeoutSeconds = Config.getIntProperty(SHUTDOWN_TIMEOUT_SECONDS_PROP, DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
        this.componentTimeoutSeconds = Config.getIntProperty(SHUTDOWN_COMPONENT_TIMEOUT_SECONDS_PROP, DEFAULT_COMPONENT_TIMEOUT_SECONDS);
        this.jmxBusyThreadsTimeoutSeconds = Config.getIntProperty(JMX_BUSY_THREADS_TIMEOUT_SECONDS_PROP, DEFAULT_JMX_BUSY_THREADS_TIMEOUT_SECONDS);
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
     * Perform a coordinated shutdown of all dotCMS components.
     * This method is idempotent - it can be called multiple times safely.
     * 
     * @return true if shutdown was successful, false if timeout or already completed
     */
    public boolean shutdown() {
        if (debugMode) {
            Logger.debug(this, "ShutdownCoordinator.shutdown() called");
        }
        
        if (!shutdownInProgress.compareAndSet(false, true)) {
            Logger.info(this, "Shutdown already in progress, waiting for completion...");
            return waitForShutdownCompletion();
        }
        
        if (shutdownCompleted.get()) {
            Logger.info(this, "Shutdown already completed");
            return true;
        }
        
        Logger.info(this, "Starting coordinated shutdown with timeout of " + shutdownTimeoutSeconds + " seconds");
        
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
            Logger.info(this, "Coordinated shutdown completed successfully");
            return success;
            
        } catch (java.util.concurrent.TimeoutException e) {
            Logger.error(this, "Shutdown timed out after " + shutdownTimeoutSeconds + " seconds, cancelling background task", e);
            if (shutdownFuture != null) {
                shutdownFuture.cancel(true); // Cancel the background task to prevent memory leak
            }
            shutdownCompleted.set(true);
            return false;
        } catch (Exception e) {
            Logger.error(this, "Shutdown failed: " + e.getMessage(), e);
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
        Logger.info(this, "Starting coordinated shutdown process");
        
        // Phase 1: Request draining - wait for active requests to complete
        long phase1Start = System.currentTimeMillis();
        Logger.debug(this, "Phase 1: Starting request draining");
        drainActiveRequests();
        long phase1Duration = System.currentTimeMillis() - phase1Start;
        Logger.info(this, String.format("Phase 1: Request draining completed (%dms)", phase1Duration));
        
        // Phase 2: Component shutdown operations
        long phase2Start = System.currentTimeMillis();
        Logger.debug(this, "Phase 2: Starting component shutdown operations");
        final List<ShutdownTask> tasks = createShutdownTasks();
        boolean allSuccessful = true;
        
        for (ShutdownTask task : tasks) {
            try {
                Logger.debug(this, "Executing shutdown task: " + task.getName());
                
                final CompletableFuture<Void> taskFuture = CompletableFuture.runAsync(() -> {
                    try {
                        task.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                
                // Use custom timeout if specified, otherwise use default component timeout
                int timeoutToUse = task.getTimeoutSeconds() > 0 ? task.getTimeoutSeconds() : componentTimeoutSeconds;
                if (debugMode && task.getTimeoutSeconds() > 0) {
                    Logger.debug(this, "Using custom timeout of " + timeoutToUse + "s for task: " + task.getName());
                }
                
                taskFuture.get(timeoutToUse, TimeUnit.SECONDS);
                
                Logger.debug(this, "Completed shutdown task: " + task.getName());
                
            } catch (Exception e) {
                Logger.warn(this, "Shutdown task '" + task.getName() + "' failed or timed out: " + e.getMessage());
                allSuccessful = false;
                // Continue with other tasks even if one fails
            }
        }
        
        long phase2Duration = System.currentTimeMillis() - phase2Start;
        Logger.info(this, String.format("Phase 2: Component shutdown operations completed (%dms)", phase2Duration));
        
        long totalDuration = System.currentTimeMillis() - shutdownStartTime;
        Logger.info(this, String.format("Coordinated shutdown process completed %s (total: %dms, phase1: %dms, phase2: %dms)", 
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
            Logger.debug(this, String.format("Beginning request draining (timeout: %ds, check interval: %dms)", 
                drainTimeout, checkInterval));
            
            long drainStartTime = System.currentTimeMillis();
            long drainTimeoutMs = drainTimeout * 1000L;
            
            // Initial check
            int initialActiveRequests = activeRequestCount.get();
            Logger.info(this, String.format("Initial active request count: %d", initialActiveRequests));
            
            // If no active requests from the start, skip the draining loop
            if (initialActiveRequests == 0) {
                int initialBusyThreads = 0;
                try {
                    initialBusyThreads = getBusyConnectorThreads();
                } catch (Exception e) {
                    Logger.debug(this, "Failed to query initial JMX busy threads: " + e.getMessage());
                }
                
                if (initialBusyThreads == 0) {
                    Logger.info(this, "No active requests or busy threads detected - skipping request draining");
                    return;
                }
                Logger.info(this, String.format("No active requests but %d busy threads detected - proceeding with draining", initialBusyThreads));
            }
        
        while (System.currentTimeMillis() - drainStartTime < drainTimeoutMs) {
            int activeRequests = activeRequestCount.get();
            int busyThreads = 0;
            
            // Try to get busy threads, but don't let JMX issues block shutdown
            try {
                busyThreads = getBusyConnectorThreads();
            } catch (Exception e) {
                Logger.debug(this, "Failed to query JMX for busy threads, continuing with activeRequests only: " + e.getMessage());
            }
            
            if (activeRequests == 0 && busyThreads == 0) {
                long drainTime = System.currentTimeMillis() - drainStartTime;
                Logger.info(this, String.format("Request draining completed - no active requests (%dms)", drainTime));
                return;
            }
            
            // Log progress - first few iterations at INFO, then DEBUG
            long elapsed = System.currentTimeMillis() - drainStartTime;
            final String message = String.format("Waiting for requests to complete: %d active requests, %d busy threads (elapsed: %dms)",
                    activeRequests, busyThreads, elapsed);
            if (elapsed < 2000) {
                Logger.info(this, message);
            } else {
                Logger.debug(this, message);
            }
            
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.warn(this, "Request draining interrupted");
                return;
            }
        }
        
        // Timeout reached
        int finalActiveRequests = activeRequestCount.get();
        int finalBusyThreads = 0;
        try {
            finalBusyThreads = getBusyConnectorThreads();
        } catch (Exception e) {
            Logger.debug(this, "Failed to query final JMX busy threads: " + e.getMessage());
        }
        
        long drainTime = System.currentTimeMillis() - drainStartTime;
        
        if (finalActiveRequests > 0 || finalBusyThreads > 0) {
            Logger.warn(this, String.format("Request draining timeout reached after %dms - proceeding with %d active requests, %d busy threads", 
                drainTime, finalActiveRequests, finalBusyThreads));
        } else {
            Logger.info(this, String.format("Request draining completed at timeout (%dms)", drainTime));
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
            
            // Wait for result with a timeout
            return future.get(jmxBusyThreadsTimeoutSeconds, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            // If JMX fails or times out, return 0 (assume no busy threads)
            if (debugMode) {
                Logger.debug(this, "JMX query for busy threads failed or timed out: " + e.getMessage());
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
     * Create the list of shutdown tasks in the correct order.
     */
    private List<ShutdownTask> createShutdownTasks() {
        List<ShutdownTask> tasks = CDIUtils.getBeans(ShutdownTask.class);
        
        // Always log ShutdownTask discovery in case of issues, but limit to warn level if debug is off
        Logger.info(this, "Found " + tasks.size() + " ShutdownTask implementations for shutdown coordination");
        for (ShutdownTask task : tasks) {
            Class<?> taskClass = task.getClass();
            String className = taskClass.getName();
            Class<?> actualClass = getActualClass(taskClass);
            
            if (actualClass != taskClass) {
                Logger.debug(this, "CDI proxy detected: " + className + " -> actual class: " + actualClass.getName());
            }
            
            ShutdownOrder order = actualClass.getAnnotation(ShutdownOrder.class);
            if (order != null) {
                if (debugMode) {
                    Logger.debug(this, "  " + className + " (order: " + order.value() + ")");
                }
            } else {
                Logger.warn(this, "  " + className + " is missing @ShutdownOrder annotation (actual class: " + actualClass.getName() + ")");
            }
        }
        
        return tasks.stream()
                .sorted(Comparator.comparingInt(task -> {
                    Class<?> taskClass = task.getClass();
                    Class<?> actualClass = getActualClass(taskClass);
                    
                    ShutdownOrder order = actualClass.getAnnotation(ShutdownOrder.class);
                    if (order == null) {
                        Logger.warn(this, "ShutdownTask " + taskClass.getName() + " (actual: " + actualClass.getName() + ") is missing @ShutdownOrder annotation, using default order 100");
                        return 100; // Default order for tasks without annotation
                    }
                    return order.value();
                }))
                .collect(Collectors.toList());
    }
    
    /**
     * Extract the actual implementation class from a CDI proxy.
     * CDI creates proxy classes that may not have the annotations of the actual implementation.
     */
    private Class<?> getActualClass(Class<?> clazz) {
        String className = clazz.getName();
        
        // Handle CDI proxies - get the actual implementation class
        if (className.contains("$Proxy") || className.contains("$$") || className.contains("_ClientProxy")) {
            Class<?> actualClass = clazz.getSuperclass();
            if (actualClass == Object.class) {
                // It's an interface proxy, try to get the actual class from interfaces
                Class<?>[] interfaces = clazz.getInterfaces();
                for (Class<?> iface : interfaces) {
                    if (iface != ShutdownTask.class && ShutdownTask.class.isAssignableFrom(iface)) {
                        return iface;
                    }
                }
            }
            return actualClass;
        }
        
        return clazz;
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
     * Checks if a given exception is likely related to a system shutdown.
     * This is the centralized method to determine if an error should be suppressed
     * or handled differently during the shutdown process.
     *
     * An exception is considered shutdown-related if:
     * 1. The system shutdown process has already been initiated.
     * 2. The exception is an SQLException with a SQLState indicating a connection error (class '08'),
     *    which often occurs when the database connection pool is shutting down before other components.
     *
     * @param throwable The exception to check.
     * @return true if the exception is likely due to a shutdown, false otherwise.
     */
    public static boolean isShutdownRelated(final Throwable throwable) {
        // If shutdown has started, most exceptions from background tasks are expected.
        if (isShutdownStarted()) {
            return true;
        }

        // Even if shutdown hasn't "officially" started, certain DB errors
        // can be early indicators. Check for connection-related SQL exceptions.
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof java.sql.SQLException) {
                final String sqlState = ((java.sql.SQLException) cause).getSQLState();
                // SQLState class '08' indicates a connection exception, which is
                // a common symptom of the database connection pool shutting down.
                if (sqlState != null && sqlState.startsWith("08")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }

        return false;
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