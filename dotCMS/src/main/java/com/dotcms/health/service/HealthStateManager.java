package com.dotcms.health.service;

import com.dotcms.health.api.HealthCheck;
import com.dotcms.health.config.HealthCheckConfig;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthResponse;
import com.dotcms.health.model.HealthStatus;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.util.ReleaseInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Manages health check state asynchronously to ensure probe endpoints remain fast and non-blocking.
 * This class maintains cached health check results and updates them in the background.
 * 
 * Kubernetes Health Check Strategy:
 * - LIVENESS CHECKS: Only core application health (JVM, memory, servlet container)
 * - READINESS CHECKS: Core health + external dependencies (database, cache, services)
 * - PRINCIPLE: Liveness failures trigger restarts, readiness failures remove from load balancer
 * 
 * Uses centralized HealthCheckConfig for consistent configuration management.
 * 
 * This class uses a singleton pattern to ensure the same instance is available to both
 * the early-startup servlet (before CDI) and CDI-managed services (after CDI initialization).
 */
public class HealthStateManager {
    
    // Singleton instance for sharing between servlet and CDI services
    private static volatile HealthStateManager instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    private final ConcurrentHashMap<String, HealthCheckResult> healthResults = new ConcurrentHashMap<>();
    
    // Caffeine-based health response cache - handles all timing, refresh, and concurrency automatically
    private final Cache<String, HealthResponse> healthCache;
    
    private final ScheduledExecutorService executor;
    private final HealthCheckToleranceManager toleranceManager;
    private final List<HealthCheck> livenessChecks = new ArrayList<>();  // Core checks only
    private final List<HealthCheck> readinessChecks = new ArrayList<>(); // Core + dependency checks
    private final List<HealthCheck> allHealthChecks = new ArrayList<>(); // All checks for full health
    private final ConcurrentHashMap<String, HealthCheck> registeredChecks = new ConcurrentHashMap<>();
    private final int checkIntervalSeconds;
    
    // Track which health checks have already logged timeout warnings to prevent spam
    private final ConcurrentHashMap<String, Boolean> timeoutWarningLogged = new ConcurrentHashMap<>();
    
    // Startup and operational state tracking
    private volatile boolean initialized = false;
    private final Instant startupTime = Instant.now();
    private final AtomicBoolean livenessFirstSuccess = new AtomicBoolean(false);
    private final AtomicBoolean readinessFirstSuccess = new AtomicBoolean(false);
    private final AtomicLong livenessSuccessfulChecks = new AtomicLong(0);
    private final AtomicLong readinessSuccessfulChecks = new AtomicLong(0);
    
    // Track ongoing targeted refreshes to prevent race conditions with probe requests
    private final ConcurrentHashMap<String, CompletableFuture<Void>> ongoingRefreshes = new ConcurrentHashMap<>();
    
    // Startup configuration - threshold for considering liveness stable
    private static final int STABLE_OPERATION_THRESHOLD = Config.getIntProperty("health.stable.operation.threshold", 3);
    
    // Track if we've transitioned to operational phase
    private final AtomicBoolean hasTransitionedToOperational = new AtomicBoolean(false);
    
    /**
     * Gets the singleton instance of HealthStateManager.
     * This ensures the same instance is used by both servlet and CDI services.
     */
    public static HealthStateManager getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new HealthStateManager(HealthCheckConfig.CHECK_INTERVAL_SECONDS);
                }
            }
        }
        return instance;
    }
    
    /**
     * Creates a new HealthStateManager with default settings
     */
    public HealthStateManager() {
        this(HealthCheckConfig.CHECK_INTERVAL_SECONDS);
    }
    
    /**
     * Creates a new HealthStateManager with custom check interval
     */
    public HealthStateManager(int checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.toleranceManager = new HealthCheckToleranceManager();
        this.executor = Executors.newScheduledThreadPool(HealthCheckConfig.THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r, "health-check-" + System.currentTimeMillis());
            t.setDaemon(true); // Don't prevent JVM shutdown
            t.setPriority(Thread.NORM_PRIORITY - 1); // Lower priority than main threads
            return t;
        });
        
        Logger.info(this, "HealthStateManager created with " + HealthCheckConfig.THREAD_POOL_SIZE + 
            " threads and " + checkIntervalSeconds + "s interval");
        
        // Initialize Caffeine cache
        healthCache = Caffeine.newBuilder()
            .expireAfterWrite(checkIntervalSeconds, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Registers a health check to be monitored (prevents duplicates by check name)
     * Uses explicit methods to categorize as liveness or readiness.
     */
    public synchronized void registerHealthCheck(HealthCheck healthCheck) {
        // Prevent duplicate registrations by check name
        if (registeredChecks.containsKey(healthCheck.getName())) {
            return; // Already registered
        }
        
        allHealthChecks.add(healthCheck);
        registeredChecks.put(healthCheck.getName(), healthCheck);
        
        // Use explicit methods to categorize the health check
        if (healthCheck.isLivenessCheck()) {
            livenessChecks.add(healthCheck);
        }
        
        if (healthCheck.isReadinessCheck()) {
            readinessChecks.add(healthCheck);
        }
        
        // Sort by order to maintain priority
        allHealthChecks.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        livenessChecks.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        readinessChecks.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        
        // Register callback for event-driven status changes if supported
        if (healthCheck instanceof com.dotcms.health.util.HealthCheckBase) {
            com.dotcms.health.util.HealthCheckBase baseCheck = (com.dotcms.health.util.HealthCheckBase) healthCheck;
            com.dotcms.health.api.HealthEventManager eventManager = baseCheck.getEventManager();
            
            if (eventManager != null) {
                eventManager.onHealthStatusChange((checkName, oldStatus, newStatus, reason) -> {
                    Logger.info(this, String.format(
                        "Event-driven status change for %s: %s -> %s (%s)", 
                        checkName, oldStatus, newStatus, reason
                    ));
                    
                    // Trigger immediate update of health check result
                    executor.execute(() -> runSingleHealthCheck(healthCheck));
                });
                
                Logger.info(this, "Registered event-driven status change callback for " + healthCheck.getName());
            }
        }
        
        // Start background check immediately - NEVER block registration
        executor.execute(() -> {
            runSingleHealthCheck(healthCheck);
            // Invalidate caches so they get fresh results on next access
            healthCache.invalidateAll();
        });
    }
    
    /**
     * Returns the current liveness health (core checks only)
     */
    public HealthResponse getLivenessHealth() {
        return healthCache.get("liveness", key -> computeLivenessHealth());
    }
    
    /**
     * Determines if the system is still in startup phase
     */
    public boolean isInStartupPhase() {
        // Exit startup phase only when both liveness and readiness have succeeded
        // This is the definitive signal that the system is fully operational
        // 
        // No arbitrary time limits - let Kubernetes probe configuration control timeouts:
        // - initialDelaySeconds: How long to wait before first probe
        // - timeoutSeconds: How long each probe can take  
        // - periodSeconds: How often to probe
        // - failureThreshold: How many failures before restart
        //
        // If startup is truly excessive, Kubernetes will restart the pod based on
        // its probe configuration, which is the correct behavior.
        return !(hasLivenessEverSucceeded() && hasReadinessEverSucceeded());
    }
    
    /**
     * Determines if the system is in liveness startup phase.
     * Liveness should succeed as soon as the process is responsive (not deadlocked).
     * This is separate from full operational readiness.
     */
    public boolean isInLivenessStartupPhase() {
        // Liveness startup phase ends when liveness checks succeed
        // This allows liveness to succeed during plugin upgrades, DB migrations, etc.
        // as long as the process itself is responsive
        return !hasLivenessEverSucceeded();
    }
    
    /**
     * Determines if the system is in readiness startup phase.
     * Readiness should only succeed when fully operational and ready to serve traffic.
     */
    public boolean isInReadinessStartupPhase() {
        // Readiness startup phase ends only when both liveness AND readiness succeed
        // This ensures we don't serve traffic until fully operational
        return !(hasLivenessEverSucceeded() && hasReadinessEverSucceeded());
    }
    
    /**
     * Determines if liveness has ever been successful (transitioned from startup to operational)
     */
    public boolean hasLivenessEverSucceeded() {
        return livenessFirstSuccess.get();
    }
    
    /**
     * Determines if readiness has ever been successful
     */
    public boolean hasReadinessEverSucceeded() {
        return readinessFirstSuccess.get();
    }
    
    /**
     * Gets the startup age in human readable format
     */
    public String getStartupAge() {
        Duration timeSinceStartup = Duration.between(startupTime, Instant.now());
        long minutes = timeSinceStartup.toMinutes();
        long seconds = timeSinceStartup.toSeconds() % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Gets the uptime of the application in seconds.
     * Used for metrics and monitoring purposes.
     */
    public long getUptimeSeconds() {
        return Duration.between(startupTime, Instant.now()).toSeconds();
    }
    
    /**
     * Gets the tolerance manager for accessing failure window information
     */
    public HealthCheckToleranceManager getToleranceManager() {
        return toleranceManager;
    }
    
    /**
     * Returns the current readiness health (core + dependency checks)
     */
    public HealthResponse getReadinessHealth() {
        return healthCache.get("readiness", key -> computeReadinessHealth());
    }
    
    /**
     * Gets the current overall health status for all registered health checks
     * Uses cached results to ensure fast, non-blocking response
     */
    public HealthResponse getCurrentHealth() {
        return healthCache.get("health", key -> computeFullHealth());
    }
    
    /**
     * Computes liveness health from current health check results using eventual consistency.
     * Always returns immediately - never blocks waiting for health checks to complete.
     */
    private HealthResponse computeLivenessHealth() {
        boolean inLivenessStartupPhase = isInLivenessStartupPhase();
        
        List<HealthCheckResult> results = livenessChecks.stream()
            .<HealthCheckResult>map(check -> {
                HealthCheckResult cachedResult = healthResults.get(check.getName());
                if (cachedResult != null) {
                    return toleranceManager.evaluateWithTolerance(cachedResult, true, inLivenessStartupPhase);
                } else {
                    // No cached result - trigger background check but return optimistic default
                    executor.execute(() -> runSingleHealthCheck(check));
                    return createOptimisticLivenessResult(check.getName());
                }
            })
            .collect(Collectors.toList());
        
        HealthStatus overallStatus = deriveOverallStatus(results);
        HealthResponse response = createRfcCompliantHealthResponse(overallStatus, results);
        
        // Track startup progression for liveness
        if (overallStatus == HealthStatus.UP || overallStatus == HealthStatus.DEGRADED) {
            if (livenessFirstSuccess.compareAndSet(false, true)) {
                boolean hasServletContainer = results.stream().anyMatch(r -> "servlet-container".equals(r.name()));
                String readyMessage = hasServletContainer ? 
                    "System is now live and ready to serve HTTP requests" : 
                    "System is now live (servlet container check not available)";
                    
                Logger.info(this, String.format(
                    "LIVENESS FIRST SUCCESS after %s startup - %s - status: %s", 
                    getStartupAge(), readyMessage, overallStatus
                ));
                
                // Immediately update readiness since liveness state changed
                healthCache.invalidate("readiness");
            }
            livenessSuccessfulChecks.incrementAndGet();
        }
        
        return response;
    }
    
    /**
     * Computes readiness health from current health check results using eventual consistency.
     * Always returns immediately - never blocks waiting for health checks to complete.
     */
    private HealthResponse computeReadinessHealth() {
        // Readiness should only succeed AFTER liveness has succeeded first
        if (!hasLivenessEverSucceeded()) {
            return createNotReadyDuringStartupResponse();
        }
        
        boolean inReadinessStartupPhase = isInReadinessStartupPhase();
        
        List<HealthCheckResult> results = readinessChecks.stream()
            .<HealthCheckResult>map(check -> {
                HealthCheckResult cachedResult = healthResults.get(check.getName());
                if (cachedResult != null) {
                    return toleranceManager.evaluateWithTolerance(cachedResult, false, inReadinessStartupPhase);
                } else {
                    // No cached result - trigger background check but return conservative default
                    executor.execute(() -> runSingleHealthCheck(check));
                    return createConservativeReadinessResult(check.getName());
                }
            })
            .collect(Collectors.toList());
        
        HealthStatus overallStatus = deriveOverallStatus(results);
        HealthResponse response = createRfcCompliantHealthResponse(overallStatus, results);
        
        // Track startup progression for readiness
        if (overallStatus == HealthStatus.UP || overallStatus == HealthStatus.DEGRADED) {
            if (readinessFirstSuccess.compareAndSet(false, true)) {
                boolean hasMonitorModeChecks = results.stream().anyMatch(r -> r.monitorModeApplied());
                boolean hasDatabaseCheck = results.stream().anyMatch(r -> r.name().contains("database"));
                
                String readyMessage = "System is now ready to receive traffic";
                if (hasMonitorModeChecks) {
                    readyMessage += " (with monitor mode active - degraded conditions converted to DEGRADED status)";
                }
                if (hasDatabaseCheck) {
                    readyMessage += " - database and external dependencies evaluated";
                }
                
                Logger.info(this, String.format(
                    "READINESS FIRST SUCCESS after %s startup - %s - status: %s", 
                    getStartupAge(), readyMessage, overallStatus
                ));
                
                // Trigger immediate operational transition since both liveness and readiness succeeded
                checkForOperationalTransition();
            }
            readinessSuccessfulChecks.incrementAndGet();
        }
        
        return response;
    }
    
    /**
     * Computes full health from current health check results using eventual consistency.
     * Always returns immediately - never blocks waiting for health checks to complete.
     */
    private HealthResponse computeFullHealth() {
        boolean inStartupPhase = isInStartupPhase();
        
        List<HealthCheckResult> results = allHealthChecks.stream()
            .<HealthCheckResult>map(check -> {
                HealthCheckResult cachedResult = healthResults.get(check.getName());
                if (cachedResult != null) {
                    return toleranceManager.evaluateWithTolerance(cachedResult, false, inStartupPhase);
                } else {
                    // No cached result - trigger background check but return conservative default
                    executor.execute(() -> runSingleHealthCheck(check));
                    return createConservativeHealthResult(check.getName());
                }
            })
            .collect(Collectors.toList());
        
        HealthStatus overallStatus = deriveOverallStatus(results);
        return createRfcCompliantHealthResponse(overallStatus, results);
    }
    
    /**
     * Creates a minimal response when no cached results are available
     */
    private HealthResponse createMinimalResponse(List<HealthCheck> checks, String type) {
        List<HealthCheckResult> results = new ArrayList<>();
        
        for (HealthCheck check : checks) {
            HealthCheckResult placeholderResult = HealthCheckResult.builder()
                .name(check.getName())
                .status(HealthStatus.UNKNOWN)
                .message("Health check not yet available - background check in progress")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
            results.add(placeholderResult);
        }
        
        return createRfcCompliantHealthResponse(HealthStatus.UNKNOWN, results);
    }
    
    /**
     * Creates a startup-aware result for liveness checks when no cached data is available.
     * Always optimistic: UP unless proven DOWN (process is responsive if serving HTTP requests)
     * This prevents unnecessary pod restarts during legitimate startup operations.
     */
    private HealthCheckResult createOptimisticLivenessResult(String checkName) {
        boolean inLivenessStartupPhase = isInLivenessStartupPhase();
        
        if (inLivenessStartupPhase) {
            // During liveness startup: Be optimistic - assume alive unless proven otherwise
            // The fact that we can serve HTTP requests means the process is responsive
            return HealthCheckResult.builder()
                .name(checkName)
                .status(HealthStatus.UP)
                .message("Liveness startup phase - process responsive, health check verifying in background")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
        } else {
            // After liveness startup: Continue being optimistic - assume alive unless proven otherwise
            return HealthCheckResult.builder()
                .name(checkName)
                .status(HealthStatus.UP)
                .message("Operational phase - optimistic liveness result (health check running in background)")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
        }
    }
    
    /**
     * Creates a startup-aware result for readiness checks when no cached data is available.
     * During startup: DOWN until verified (system not ready to serve traffic)
     * After startup: UNKNOWN until verified (conservative about external dependencies)
     */
    private HealthCheckResult createConservativeReadinessResult(String checkName) {
        boolean inReadinessStartupPhase = isInReadinessStartupPhase();
        
        if (inReadinessStartupPhase) {
            // During readiness startup: Definitely not ready to serve traffic
            return HealthCheckResult.builder()
                .name(checkName)
                .status(HealthStatus.DOWN)
                .message("Readiness startup phase - not ready to serve traffic (health check running in background)")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
        } else {
            // After readiness startup: Conservative about external dependencies
            return HealthCheckResult.builder()
                .name(checkName)
                .status(HealthStatus.UNKNOWN)
                .message("Operational phase - verifying external dependency (health check running in background)")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
        }
    }
    
    /**
     * Creates a startup-aware result for full health checks when no cached data is available.
     * During startup: DOWN until verified (system initializing)
     * After startup: UNKNOWN until verified (monitoring data being gathered)
     */
    private HealthCheckResult createConservativeHealthResult(String checkName) {
        boolean inStartupPhase = isInStartupPhase();
        
        if (inStartupPhase) {
            // During startup: System is initializing
            return HealthCheckResult.builder()
                .name(checkName)
                .status(HealthStatus.DOWN)
                .message("Startup phase - system initializing (health check running in background)")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
        } else {
            // After startup: Gathering monitoring data
            return HealthCheckResult.builder()
                .name(checkName)
                .status(HealthStatus.UNKNOWN)
                .message("Operational phase - gathering health data (health check running in background)")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build();
        }
    }
    
    /**
     * Starts the background health checking process
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        // Start all checks in background immediately - NEVER block initialization
        executor.execute(this::runAllHealthChecks);
        
        // Schedule periodic health checks with smart frequency
        scheduleSmartHealthChecks();
        
        initialized = true;
        Logger.info(this, "HealthStateManager initialized with background health checking");
    }
    
    /**
     * Forces an immediate health check run (non-blocking)
     */
    public void triggerHealthCheck() {
        executor.execute(this::runAllHealthChecks);
    }
    
    /**
     * Forces an immediate refresh of all health checks (non-blocking by default)
     * Use with caution as this can be resource-intensive
     */
    public void forceRefresh() {
        forceRefresh(false);
    }
    
    /**
     * Forces an immediate refresh of all health checks
     * @param blocking whether to wait for all checks to complete before returning
     */
    public void forceRefresh(boolean blocking) {
        Logger.info(this, String.format("Triggering force refresh of all health checks (blocking: %s)", blocking));
        
        if (blocking) {
            // Run all health checks and wait for completion to prevent race conditions
            runAllHealthChecksAndWait();
            healthCache.invalidateAll();
        } else {
            // Start all health checks asynchronously and return immediately
            runAllHealthChecksAsync();
            healthCache.invalidateAll();
        }
    }
    
    /**
     * Runs all health checks asynchronously without waiting for completion (non-blocking)
     * This allows the refresh endpoint to respond quickly while checks run in background
     */
    private void runAllHealthChecksAsync() {
        Logger.info(this, String.format("Starting async health checks for %d registered checks", allHealthChecks.size()));
        
        // Start all health checks asynchronously - don't wait for completion
        for (HealthCheck healthCheck : allHealthChecks) {
            CompletableFuture.runAsync(() -> {
                try {
                    runSingleHealthCheck(healthCheck);
                    Logger.debug(this, String.format("Async health check completed: %s", healthCheck.getName()));
                } catch (Exception e) {
                    Logger.warn(this, String.format("Async health check failed: %s - %s", healthCheck.getName(), e.getMessage()));
                }
            }, executor);
        }
        
        Logger.info(this, "All async health checks started - returning immediately");
    }
    
    /**
     * Runs all health checks and waits for their completion (blocking)
     * This ensures cache updates happen with fresh results, not stale ones
     */
    private void runAllHealthChecksAndWait() {
        // Create futures for all health checks to track completion
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (HealthCheck healthCheck : allHealthChecks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
                runSingleHealthCheckBlocking(healthCheck), executor);
            futures.add(future);
        }
        
        // Wait for all health checks to complete (with timeout to prevent hanging)
        try {
            CompletableFuture<Void> allChecks = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            // Calculate timeout based on the longest individual health check timeout + buffer
            long maxIndividualTimeout = allHealthChecks.stream()
                .mapToLong(this::getHealthCheckTimeoutMs)
                .max()
                .orElse(30000L); // Default 30 seconds if no checks
            
            // Add buffer time for execution overhead
            long timeoutMs = maxIndividualTimeout + 5000L; // Max individual timeout + 5 second buffer
            
            // Also respect any configured override
            timeoutMs = Math.max(timeoutMs, Config.getLongProperty("health.force.refresh.timeout-ms", timeoutMs));
            
            allChecks.get(timeoutMs, TimeUnit.MILLISECONDS);
            
            Logger.info(this, String.format("All health checks completed in forceRefresh (timeout: %dms)", timeoutMs));
        } catch (Exception e) {
            Logger.warn(this, "Some health checks did not complete within timeout during forceRefresh: " + e.getMessage());
            // Continue anyway - we'll use whatever results we have
        }
    }
    
    /**
     * Runs a single health check synchronously (blocking version)
     */
    private void runSingleHealthCheckBlocking(HealthCheck healthCheck) {
        String checkName = healthCheck.getName();
        
        try {
            // Try event-driven result first for faster failure detection
            if (healthCheck instanceof com.dotcms.health.util.HealthCheckBase) {
                com.dotcms.health.util.HealthCheckBase baseCheck = (com.dotcms.health.util.HealthCheckBase) healthCheck;
                HealthCheckResult eventDrivenResult = baseCheck.getEventDrivenResult();
                
                if (eventDrivenResult != null) {
                    Logger.debug(this, "Using event-driven result for " + checkName + ": " + eventDrivenResult.status());
                    
                    // Apply tolerance and safety mode handling 
                    HealthCheckResult processedResult = processHealthCheckResult(eventDrivenResult, healthCheck);
                    healthResults.put(checkName, processedResult);
                    return; // Use event-driven result, skip traditional polling
                }
            }
            
            // Fallback to traditional polling check (blocking)
            Logger.debug(this, "Running traditional health check for " + checkName);
            
            long timeoutMs = getHealthCheckTimeoutMs(healthCheck);
            HealthCheckResult result;
            
            try {
                // Create a future with timeout for the health check
                CompletableFuture<HealthCheckResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return healthCheck.check();
                    } catch (Exception e) {
                        Logger.warn(this, "Health check execution failed: " + checkName + " - " + e.getMessage(), e);
                        return createErrorResult(checkName, e);
                    }
                }, executor);
                
                result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                
                // Health check succeeded - reset timeout warning flag for this check
                timeoutWarningLogged.remove(checkName);
                
                Logger.debug(this, "Health check completed: " + checkName + " - status: " + result.status() + " - duration: " + result.durationMs() + "ms");
                
            } catch (Exception e) {
                // Handle timeout or execution failure
                Logger.warn(this, "Health check timed out or failed: " + checkName + " (timeout: " + timeoutMs + "ms)");
                timeoutWarningLogged.put(checkName, true);
                result = createTimeoutResult(checkName, timeoutMs, e);
            }
            
            // Apply tolerance and safety mode handling
            HealthCheckResult processedResult = processHealthCheckResult(result, healthCheck);
            healthResults.put(checkName, processedResult);
            
        } catch (Exception e) {
            Logger.error(this, "Unexpected error in blocking health check for " + checkName, e);
            HealthCheckResult errorResult = createErrorResult(checkName, e);
            HealthCheckResult processedResult = processHealthCheckResult(errorResult, healthCheck);
            healthResults.put(checkName, processedResult);
        }
    }
    
    /**
     * Forces a refresh of a specific health check by name.
     * This is a non-blocking operation by default.
     * 
     * @param healthCheckName the name of the health check to refresh
     */
    public void forceRefreshHealthCheck(String healthCheckName) {
        forceRefreshHealthCheck(healthCheckName, false);
    }
    
    /**
     * Forces a refresh of a specific health check by name.
     * 
     * @param healthCheckName the name of the health check to refresh
     * @param blocking whether to wait for the refresh to complete
     */
    public void forceRefreshHealthCheck(String healthCheckName, boolean blocking) {
        Logger.info(this, String.format("Forcing refresh of health check: %s (blocking: %s)", healthCheckName, blocking));
        
        Optional<HealthCheck> healthCheck = Optional.ofNullable(registeredChecks.get(healthCheckName));
        if (!healthCheck.isPresent()) {
            Logger.warn(this, String.format("Health check not found: %s", healthCheckName));
            return;
        }
        
        if (blocking) {
            // Execute the health check synchronously to get fresh result
            CompletableFuture<Void> refreshFuture = CompletableFuture.runAsync(() -> {
                Logger.debug(this, String.format("Executing health check: %s", healthCheckName));
                HealthCheckResult result = healthCheck.get().check();
                Logger.debug(this, String.format("Health check %s completed with status: %s", healthCheckName, result.status()));
                
                // Process and store the result
                HealthCheckResult processedResult = processHealthCheckResult(result, healthCheck.get());
                updateHealthCheckResult(healthCheckName, processedResult);
                
                // Invalidate all affected cache entries - Caffeine will recompute on next access
                invalidateAffectedCaches(healthCheckName);
                
                Logger.debug(this, String.format("Health check result updated and caches invalidated for: %s", healthCheckName));
            }, executor);
            
            // Track this refresh for race condition prevention
            ongoingRefreshes.put(healthCheckName, refreshFuture);
            
            try {
                refreshFuture.get(10, TimeUnit.SECONDS);
                Logger.info(this, String.format("Blocking refresh completed for health check: %s", healthCheckName));
                
                // Force immediate cache recomputation for blocking calls
                if (isLivenessCheck(healthCheckName)) {
                    getLivenessHealth(); // This will recompute and cache
                }
                if (isReadinessCheck(healthCheckName)) {
                    getReadinessHealth(); // This will recompute and cache
                }
                getCurrentHealth(); // Always recompute full health
                
            } catch (Exception e) {
                Logger.error(this, String.format("Failed to wait for health check completion: %s", healthCheckName), e);
            } finally {
                // Clean up completed future to prevent memory leak
                ongoingRefreshes.remove(healthCheckName);
            }
        } else {
            // Execute asynchronously - just run the check and invalidate
            CompletableFuture<Void> refreshFuture = CompletableFuture.runAsync(() -> {
                Logger.debug(this, String.format("Executing health check asynchronously: %s", healthCheckName));
                HealthCheckResult result = healthCheck.get().check();
                Logger.debug(this, String.format("Async health check %s completed with status: %s", healthCheckName, result.status()));
                
                // Process and store the result
                HealthCheckResult processedResult = processHealthCheckResult(result, healthCheck.get());
                updateHealthCheckResult(healthCheckName, processedResult);
                
                // Invalidate caches - they'll be recomputed on next access
                invalidateAffectedCaches(healthCheckName);
                
                Logger.debug(this, String.format("Async health check result updated and caches invalidated for: %s", healthCheckName));
            }, executor);
            
            // Track this refresh for race condition prevention and clean up when complete
            ongoingRefreshes.put(healthCheckName, refreshFuture);
            refreshFuture.whenComplete((result, throwable) -> {
                // Clean up completed future to prevent memory leak
                ongoingRefreshes.remove(healthCheckName);
            });
        }
    }
    
    /**
     * Invalidates cache entries that are affected by a specific health check
     */
    private void invalidateAffectedCaches(String healthCheckName) {
        // Always invalidate full health
        healthCache.invalidate("health");
        
        // Invalidate liveness if this check affects liveness
        if (isLivenessCheck(healthCheckName)) {
            healthCache.invalidate("liveness");
        }
        
        // Invalidate readiness if this check affects readiness
        if (isReadinessCheck(healthCheckName)) {
            healthCache.invalidate("readiness");
        }
        
        Logger.debug(this, String.format("Cache invalidation completed for health check: %s", healthCheckName));
    }
    
    /**
     * Checks if a health check affects liveness
     */
    private boolean isLivenessCheck(String healthCheckName) {
        return livenessChecks.stream().anyMatch(check -> check.getName().equals(healthCheckName));
    }
    
    /**
     * Checks if a health check affects readiness
     */
    private boolean isReadinessCheck(String healthCheckName) {
        return readinessChecks.stream().anyMatch(check -> check.getName().equals(healthCheckName));
    }
    
    /**
     * Waits for any ongoing targeted refresh of specific health checks to complete.
     * This prevents race conditions where probe requests get stale cached results
     * while targeted refreshes are in progress.
     * 
     * @param checkNames the names of health checks to wait for
     * @param timeoutMs maximum time to wait in milliseconds
     * @return true if all refreshes completed, false if timeout occurred
     */
    public boolean waitForOngoingRefreshes(List<String> checkNames, long timeoutMs) {
        List<CompletableFuture<Void>> refreshesToWaitFor = new ArrayList<>();
        
        for (String checkName : checkNames) {
            CompletableFuture<Void> ongoingRefresh = ongoingRefreshes.get(checkName);
            if (ongoingRefresh != null) {
                refreshesToWaitFor.add(ongoingRefresh);
                Logger.debug(this, String.format("Waiting for ongoing refresh of: %s", checkName));
            }
        }
        
        if (refreshesToWaitFor.isEmpty()) {
            return true; // No ongoing refreshes to wait for
        }
        
        try {
            CompletableFuture<Void> allRefreshes = CompletableFuture.allOf(
                refreshesToWaitFor.toArray(new CompletableFuture[0]));
            
            allRefreshes.get(timeoutMs, TimeUnit.MILLISECONDS);
            Logger.debug(this, String.format("All ongoing refreshes completed within %dms", timeoutMs));
            return true;
        } catch (Exception e) {
            Logger.debug(this, String.format("Some ongoing refreshes did not complete within %dms: %s", timeoutMs, e.getMessage()));
            return false;
        }
    }
    
    /**
     * Gets a specific health check result by name
     * 
     * @param checkName the name of the health check to retrieve
     * @return Optional containing the health check result if found, empty otherwise
     */
    public Optional<HealthCheckResult> getHealthCheckResult(String checkName) {
        HealthCheckResult result = healthResults.get(checkName);
        return Optional.ofNullable(result);
    }
    
    /**
     * Shuts down the health state manager
     */
    public void shutdown() {
        Logger.info(this, "Shutting down HealthStateManager");
        initialized = false;
        
        // Cancel all ongoing refreshes to prevent new work
        Logger.debug(this, String.format("Cancelling %d ongoing refreshes", ongoingRefreshes.size()));
        ongoingRefreshes.values().forEach(future -> future.cancel(true));
        ongoingRefreshes.clear();
        
        // Attempt graceful shutdown first
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                Logger.warn(this, "Executor did not terminate gracefully, forcing shutdown");
                
                // Force shutdown and wait a bit more
                var cancelledTasks = executor.shutdownNow();
                Logger.info(this, String.format("Cancelled %d tasks during forced shutdown", cancelledTasks.size()));
                
                // Wait for forced shutdown to complete
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Logger.error(this, "Executor did not terminate even after forced shutdown");
                }
            } else {
                Logger.info(this, "HealthStateManager executor shutdown gracefully");
            }
        } catch (InterruptedException e) {
            Logger.warn(this, "Shutdown interrupted, forcing immediate termination");
            var cancelledTasks = executor.shutdownNow();
            Logger.info(this, String.format("Cancelled %d tasks during interrupt", cancelledTasks.size()));
            Thread.currentThread().interrupt();
        }
        
        // Clear all cached data
        healthResults.clear();
        healthCache.invalidateAll();
        
        Logger.info(this, "HealthStateManager shutdown completed");
    }
    
    /**
     * Schedules health checks with optimized frequency:
     * - Event-driven checks: Less frequent polling (they get real-time updates)
     * - Traditional checks: Standard polling frequency
     */
    private void scheduleSmartHealthChecks() {
        // Standard frequency for traditional checks
        int standardInterval = checkIntervalSeconds;
        
        // Reduced frequency for event-driven checks (they get real-time updates)
        int eventDrivenInterval = Math.max(standardInterval * 3, 60); // 3x less frequent, min 60s
        
        // Schedule traditional checks at standard frequency
        executor.scheduleAtFixedRate(
            this::runTraditionalHealthChecks, 
            standardInterval, 
            standardInterval, 
            TimeUnit.SECONDS
        );
        
        // Schedule event-driven checks at reduced frequency (for fallback validation)
        executor.scheduleAtFixedRate(
            this::runEventDrivenHealthChecks, 
            eventDrivenInterval, 
            eventDrivenInterval, 
            TimeUnit.SECONDS
        );
        
        Logger.info(this, String.format(
            "Scheduled smart health checking: traditional=%ds, event-driven=%ds", 
            standardInterval, eventDrivenInterval
        ));
    }
    
    /**
     * Runs only traditional (non-event-driven) health checks
     */
    private void runTraditionalHealthChecks() {
        for (HealthCheck healthCheck : allHealthChecks) {
            if (!(healthCheck instanceof com.dotcms.health.util.HealthCheckBase) || 
                !((com.dotcms.health.util.HealthCheckBase) healthCheck).isEventDriven()) {
                executor.execute(() -> runSingleHealthCheck(healthCheck));
            }
        }
        healthCache.invalidateAll();
    }
    
    /**
     * Runs only event-driven health checks (for periodic validation)
     */
    private void runEventDrivenHealthChecks() {
        for (HealthCheck healthCheck : allHealthChecks) {
            if (healthCheck instanceof com.dotcms.health.util.HealthCheckBase && 
                ((com.dotcms.health.util.HealthCheckBase) healthCheck).isEventDriven()) {
                executor.execute(() -> runSingleHealthCheck(healthCheck));
            }
        }
        healthCache.invalidateAll();
    }
    
    private void runAllHealthChecks() {
        // Run health checks in parallel to prevent one slow check from blocking others
        // Each check runs in its own executor task for maximum parallelism
        for (HealthCheck healthCheck : allHealthChecks) {
            executor.execute(() -> runSingleHealthCheck(healthCheck));
        }
        
        // Invalidate all caches after health checks run - they'll be recomputed on next access
        healthCache.invalidateAll();
    }
    

    
    private void runSingleHealthCheck(HealthCheck healthCheck) {
        String checkName = healthCheck.getName();
        
        // Skip expensive health checks during shutdown to avoid accessing services that are shutting down
        if (shouldSkipHealthCheckDuringShutdown(healthCheck)) {
            Logger.debug(this, "Skipping health check during shutdown: " + checkName);
            HealthCheckResult shutdownResult = createShutdownSkippedResult(checkName);
            updateHealthCheckResult(checkName, shutdownResult);
            return;
        }
        
        // Try event-driven result first for faster failure detection
        if (healthCheck instanceof com.dotcms.health.util.HealthCheckBase) {
            com.dotcms.health.util.HealthCheckBase baseCheck = (com.dotcms.health.util.HealthCheckBase) healthCheck;
            HealthCheckResult eventDrivenResult = baseCheck.getEventDrivenResult();
            
            if (eventDrivenResult != null) {
                Logger.debug(this, "Using event-driven result for " + checkName + ": " + eventDrivenResult.status());
                
                // Apply tolerance and safety mode handling 
                HealthCheckResult processedResult = processHealthCheckResult(eventDrivenResult, healthCheck);
                healthResults.put(checkName, processedResult);
                healthCache.invalidateAll();
                checkForOperationalTransition();
                return; // Use event-driven result, skip traditional polling
            }
        }
        
        // Fallback to traditional polling check
        Logger.debug(this, "Running traditional health check for " + checkName);
        
        // Framework-level timeout handling with CompletableFuture
        CompletableFuture<HealthCheckResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Health check can block and be slow - this runs in background thread
                // Slow execution accurately reflects dependency performance
                return healthCheck.check();
            } catch (Exception e) {
                Logger.warn(this, "Health check execution failed: " + checkName + " - " + e.getMessage(), e);
                return createErrorResult(checkName, e);
            }
        }, executor);
        
        // Framework-level timeout configuration
        long timeoutMs = getHealthCheckTimeoutMs(healthCheck);
        
        future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    // Only log timeout warning on first occurrence or state change
                    boolean hasLoggedBefore = timeoutWarningLogged.getOrDefault(checkName, false);
                    
                    // Check if previous result was successful (to detect state change)
                    HealthCheckResult previousResult = healthResults.get(checkName);
                    boolean wasWorking = previousResult != null && 
                        (previousResult.status() == HealthStatus.UP || previousResult.status() == HealthStatus.DEGRADED);
                    
                    if (!hasLoggedBefore || wasWorking) {
                        String timeoutType = throwable instanceof TimeoutException ? "framework timeout" : "execution error";
                        Logger.warn(this, String.format(
                            "Health check timed out or failed: %s (timeout: %dms)", 
                            checkName, timeoutMs
                        ));
                        timeoutWarningLogged.put(checkName, true);
                    }
                    
                    // Create timeout result and process through tolerance system
                    HealthCheckResult timeoutResult = HealthCheckResult.builder()
                        .name(checkName)
                        .status(HealthStatus.DOWN)
                        .message("Health check timed out after " + timeoutMs + "ms (" + 
                                (throwable instanceof TimeoutException ? "framework timeout" : "execution error") + ")")
                        .durationMs(timeoutMs)
                        .lastChecked(Instant.now())
                        .build();
                    
                    HealthCheckResult processedResult = processHealthCheckResult(timeoutResult, healthCheck);
                    healthResults.put(checkName, processedResult);
                } else {
                    // Health check succeeded - reset timeout warning flag for this check
                    timeoutWarningLogged.remove(checkName);
                    
                    Logger.debug(this, "Health check completed: " + checkName + " - status: " + result.status() + " - duration: " + result.durationMs() + "ms");
                    
                    // Apply tolerance and safety mode handling
                    HealthCheckResult processedResult = processHealthCheckResult(result, healthCheck);
                    healthResults.put(checkName, processedResult);
                }
                
                healthCache.invalidateAll();
                checkForOperationalTransition();
            });
    }
    
    /**
     * Gets the timeout for a specific health check from configuration
     */
    private long getHealthCheckTimeoutMs(HealthCheck healthCheck) {
        String checkName = healthCheck.getName();
        
        // Check for check-specific timeout first
        String checkSpecificKey = "health.check." + checkName + ".framework-timeout-ms";
        long checkSpecificTimeout = Config.getLongProperty(checkSpecificKey, -1);
        
        if (checkSpecificTimeout > 0) {
            return checkSpecificTimeout;
        }
        
        // Fall back to global framework timeout
        return Config.getLongProperty("health.framework.timeout-ms", 30000); // 30 second default
    }
    
    /**
     * Creates an error result for health checks that throw exceptions
     */
    private HealthCheckResult createErrorResult(String checkName, Exception e) {
        return HealthCheckResult.builder()
            .name(checkName)
            .status(HealthStatus.DOWN)
            .error("Health check execution failed: " + e.getMessage())
            .lastChecked(Instant.now())
            .durationMs(0L)
            .build();
    }
    
    /**
     * Creates a timeout result for health checks that exceed framework timeout
     */
    private HealthCheckResult createTimeoutResult(String checkName, long timeoutMs, Throwable throwable) {
        String errorMessage;
        if (throwable instanceof java.util.concurrent.TimeoutException) {
            errorMessage = "Health check timed out after " + timeoutMs + "ms (framework timeout)";
        } else {
            errorMessage = "Health check failed: " + throwable.getMessage();
        }
        
        return HealthCheckResult.builder()
            .name(checkName)
            .status(HealthStatus.DOWN)
            .error(errorMessage)
            .lastChecked(Instant.now())
            .durationMs(timeoutMs)
            .build();
    }
    
    /**
     * Processes health check results by applying tolerance and safety mode handling.
     * This ensures event-driven and traditional polling results are handled consistently.
     */
    private HealthCheckResult processHealthCheckResult(HealthCheckResult originalResult, HealthCheck healthCheck) {
        // Apply tolerance management (startup vs operational phase)
        boolean inStartupPhase = isInStartupPhase();
        boolean isLivenessCheck = healthCheck.isLivenessCheck();
        
        HealthCheckResult toleranceResult = toleranceManager.evaluateWithTolerance(
            originalResult, isLivenessCheck, inStartupPhase);
        
        Logger.debug(this, String.format(
            "Processed %s result: original=%s, final=%s, startup=%s, liveness=%s", 
            healthCheck.getName(), originalResult.status(), toleranceResult.status(), 
            inStartupPhase, isLivenessCheck
        ));
        
        return toleranceResult;
    }
    
    private void updateHealthCheckResult(String checkName, HealthCheckResult result) {
        healthResults.put(checkName, result);
    }
    
    /**
     * Determines if a health check should be skipped during shutdown to avoid
     * accessing services that are shutting down or already shut down.
     */
    private boolean shouldSkipHealthCheckDuringShutdown(HealthCheck healthCheck) {
        // Import ShutdownCoordinator to check shutdown status
        try {
            boolean shutdownInProgress = com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted();
            if (!shutdownInProgress) {
                return false; // Not shutting down, run all checks normally
            }
            
            String checkName = healthCheck.getName();
            
            // Always allow shutdown health check to run (it's designed for this)
            if ("shutdown".equals(checkName)) {
                return false;
            }
            
            // Always allow essential liveness checks to run (they don't access external services)
            if ("application".equals(checkName) || 
                "system".equals(checkName) || 
                "threads".equals(checkName) ||
                "servlet-container".equals(checkName)) {
                return false;
            }
            
            // Skip expensive dependency checks during shutdown
            if ("database".equals(checkName) || 
                "cache".equals(checkName) || 
                "elasticsearch".equals(checkName) ||
                "cdi-initialization".equals(checkName)) {
                return true;
            }
            
            // For other checks, allow them to run but they should be fast
            return false;
            
        } catch (Exception e) {
            // If we can't check shutdown status, err on the side of running the check
            Logger.debug(this, "Could not check shutdown status, running health check: " + healthCheck.getName());
            return false;
        }
    }
    
    /**
     * Creates a result for health checks that are skipped during shutdown
     */
    private HealthCheckResult createShutdownSkippedResult(String checkName) {
        return HealthCheckResult.builder()
            .name(checkName)
            .status(HealthStatus.UNKNOWN)
            .message("Health check skipped during system shutdown to avoid accessing services that are shutting down")
            .lastChecked(Instant.now())
            .durationMs(0L)
            .build();
    }
    
    private HealthStatus deriveOverallStatus(List<HealthCheckResult> results) {
        if (results.isEmpty()) {
            return HealthStatus.UNKNOWN;
        }
        
        // Check for critical failures first - these take priority
        boolean hasDown = results.stream()
            .anyMatch(check -> check.status() == HealthStatus.DOWN);
        
        if (hasDown) {
            return HealthStatus.DOWN;
        }
        
        // Check for degraded conditions - these are reported but don't fail probes
        boolean hasDegraded = results.stream()
            .anyMatch(check -> check.status() == HealthStatus.DEGRADED);
        
        // Check if all are fully healthy
        boolean allUp = results.stream()
            .allMatch(check -> check.status() == HealthStatus.UP);
        
        if (allUp) {
            return HealthStatus.UP;
        } else if (hasDegraded) {
            // Show degraded in overall status but don't fail probes
            return HealthStatus.DEGRADED;
        } else {
            // Mix of UP and UNKNOWN, or all UNKNOWN
            return HealthStatus.UNKNOWN;
        }
    }
    
    /**
     * Creates an RFC-compliant health response with metadata for Prometheus compatibility
     */
    private HealthResponse createRfcCompliantHealthResponse(HealthStatus status, List<HealthCheckResult> checks) {
        String version = ReleaseInfo.getVersion();
        String buildNumber = ReleaseInfo.getBuildNumber();
        String buildDate = ReleaseInfo.getBuildDateString();
        
        return HealthResponse.builder()
            .status(status)
            .checks(checks)
            .timestamp(Instant.now())
            .version("UNVERSIONED".equals(version) ? Optional.empty() : Optional.of(version))
            .releaseId("0".equals(buildNumber) ? Optional.empty() : Optional.of(buildNumber))
            .serviceId(Optional.of("dotcms-health"))
            .description(Optional.of("dotCMS Application Health Status"))
            .links(Optional.of(Map.of("buildDate", buildDate)))
            .build();
    }
    
    /**
     * Creates a "not ready" response during startup when liveness hasn't succeeded yet
     */
    private HealthResponse createNotReadyDuringStartupResponse() {
        List<HealthCheckResult> startupResult = List.of(
            HealthCheckResult.builder()
                .name("startup-sequencing")
                .status(HealthStatus.DOWN)
                .message("Waiting for liveness probe to succeed before readiness (startup age: " + getStartupAge() + ")")
                .lastChecked(Instant.now())
                .durationMs(0L)
                .build()
        );
        
        return createRfcCompliantHealthResponse(HealthStatus.DOWN, startupResult);
    }
    
    /**
     * Logs startup phase transitions with appropriate context
     */
    private void logStartupTransition(String probeType, String event, HealthStatus status) {
        String startupAge = getStartupAge();
        boolean stillInStartup = isInStartupPhase();
        
        if (stillInStartup) {
            Logger.info(this, String.format(
                "%s probe: %s after %s startup - status: %s [STARTUP PHASE]", 
                probeType.toUpperCase(), event, startupAge, status
            ));
        } else {
            // Transitioning to operational phase
            Logger.info(this, String.format(
                "%s probe: %s after %s startup - status: %s [OPERATIONAL PHASE REACHED]", 
                probeType.toUpperCase(), event, startupAge, status
            ));
            
            // Notify tolerance manager of phase transition (only do this once)
            if (hasTransitionedToOperational.compareAndSet(false, true)) {
                toleranceManager.transitionToOperationalPhase();
            }
        }
    }
    
    /**
     * Checks if we've just transitioned out of startup phase and handles tolerance manager transition
     */
    private void checkForOperationalTransition() {
        boolean inStartupPhase = isInStartupPhase();
        
        // Transition to operational phase immediately when startup phase ends
        // (when both liveness and readiness have succeeded at least once)
        if (!inStartupPhase && hasTransitionedToOperational.compareAndSet(false, true)) {
            String startupAge = getStartupAge();
            Logger.info(this, String.format(
                "System transitioned to operational phase after %s - tolerance logic now active", 
                startupAge
            ));
            toleranceManager.transitionToOperationalPhase();
        }
    }
    
    /**
     * Formats a duration for logging
     */
    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;
        return String.format("%dm %ds", minutes, seconds);
    }
}
