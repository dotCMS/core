package com.dotcms.health.servlet;

import com.dotcms.health.checks.ApplicationHealthCheck;
import com.dotcms.health.checks.CdiInitializationHealthCheck;
import com.dotcms.health.checks.GarbageCollectionHealthCheck;
import com.dotcms.health.checks.ServletContainerHealthCheck;
import com.dotcms.health.checks.ShutdownHealthCheck;
import com.dotcms.health.checks.SystemHealthCheck;
import com.dotcms.health.checks.ThreadHealthCheck;
import com.dotcms.health.model.HealthResponse;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.service.HealthCheckRegistry;
import com.dotcms.health.service.HealthStateManager;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.management.servlet.AbstractManagementServlet;
import com.dotcms.management.config.InfrastructureConstants;
import com.dotcms.health.config.HealthEndpointConstants;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Health probe service that provides Kubernetes-compatible health check endpoints.
 * This servlet is designed to be lightweight and decoupled from other dotCMS APIs,
 * allowing it to start early and remain responsive even when other systems are initializing.
 * 
 * Management Health Endpoints (requires management port):
 * - {MANAGEMENT_PATH_PREFIX}/livez - Kubernetes liveness probe (minimal text response)
 * - {MANAGEMENT_PATH_PREFIX}/readyz - Kubernetes readiness probe (minimal text response)
 * - {MANAGEMENT_PATH_PREFIX}/health - Complete health status (JSON response)
 * 
 * This servlet extends AbstractManagementServlet to ensure it can only be accessed
 * through the management path prefix, preventing accidental exposure on wrong ports.
 */
public class HealthProbeServlet extends AbstractManagementServlet {
    
    private static final long serialVersionUID = 1L;
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CHARSET_UTF8 = "UTF-8";
    
    private HealthStateManager healthStateManager;
    private ObjectMapper objectMapper;
    private WebResource webResource;
    
    // State tracking for intelligent logging (only log on transitions)
    private volatile Boolean lastLivenessState = null; // null = not yet checked
    private volatile Boolean lastReadinessState = null; // null = not yet checked
    private volatile boolean lastLivenessHadDegraded = false; // Track if degraded conditions exist
    private volatile boolean lastReadinessHadDegraded = false; // Track if degraded conditions exist
    private volatile long livenessCheckCount = 0;
    private volatile long readinessCheckCount = 0;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        // Initialize WebResource for authentication
        webResource = new WebResource();
        
        // Use the centralized ObjectMapper provider for consistent JSON serialization
        objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        
        // Get singleton health state manager instance (shared with CDI services)
        healthStateManager = HealthStateManager.getInstance();
        
        // Register core health checks that work without CDI (for early liveness checks)
        registerCoreHealthChecks();
        
        // Start background health monitoring
        healthStateManager.initialize();
        
        // Try to set up CDI integration if available (non-blocking)
        // This will discover and register CDI health checks like DatabaseHealthCheck, CacheHealthCheck, etc.
        setupCdiIntegration();
        
        Logger.info(this, "HealthProbeServlet initialized with core health checks and CDI coordination");
    }
    
    @Override
    protected void doManagementGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        // Extract the actual endpoint from the request
        String endpoint = request.getServletPath();
        Logger.debug(this, "HealthProbeServlet.doManagementGet called with endpoint: " + endpoint + ", URI: " + request.getRequestURI());
        
        // Handle management health probe endpoints
        if (HealthEndpointConstants.Endpoints.LIVENESS.equals(endpoint)) {
            handleLivenessProbe(request, response);
            return;
        }
        
        if (HealthEndpointConstants.Endpoints.READINESS.equals(endpoint)) {
            handleReadinessProbe(request, response);
            return;
        }
        
        if (HealthEndpointConstants.Endpoints.HEALTH.equals(endpoint)) {
            handleFullHealth(request, response);
            return;
        }
        
        // For other paths, return 404 with helpful message
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("Available health endpoints: " + 
                                  HealthEndpointConstants.Endpoints.LIVENESS + ", " +
                                  HealthEndpointConstants.Endpoints.READINESS + ", " +
                                  HealthEndpointConstants.Endpoints.HEALTH);
    }
    
    /**
     * Requires authentication for detailed health endpoints.
     * Non-anonymous users (any authenticated user) can access detailed health information.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return true if authenticated, false if authentication failed (response already set)
     * @throws IOException if writing error response fails
     */
    private boolean requireAuthentication(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        try {
            // Check if authentication is disabled via configuration
            boolean authRequired = Config.getBooleanProperty("health.detailed.authentication.required", true);
            if (!authRequired) {
                Logger.debug(this, "Authentication disabled for detailed health endpoints via configuration");
                return true;
            }
            
            // Initialize authentication
            InitDataObject initData = webResource.init(null, request, response, false, null);
            User user = initData.getUser();
            
            // Check if user is authenticated (not anonymous)
            if (user == null || APILocator.getUserAPI().getAnonymousUser().equals(user)) {
                Logger.debug(this, "Anonymous user attempted to access detailed health endpoint: " + request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("text/plain");
                response.setCharacterEncoding(CHARSET_UTF8);
                response.getWriter().write("Authentication required for detailed health information");
                return false;
            }
            
            Logger.debug(this, "Authenticated user accessing detailed health endpoint: " + user.getEmailAddress());
            return true;
            
        } catch (Exception e) {
            Logger.error(this, "Authentication error for health endpoint: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.setCharacterEncoding(CHARSET_UTF8);
            response.getWriter().write("Authentication service temporarily unavailable");
            return false;
        }
    }
    
    /**
     * Registers core health checks that don't require CDI
     */
    private void registerCoreHealthChecks() {
        try {
            Logger.info(this, "Registering essential core health checks");
            
            // Essential liveness checks - only internal state, no external dependencies
            ApplicationHealthCheck applicationCheck = new ApplicationHealthCheck();
            SystemHealthCheck systemCheck = new SystemHealthCheck();
            ThreadHealthCheck threadCheck = new ThreadHealthCheck();
            GarbageCollectionHealthCheck gcCheck = new GarbageCollectionHealthCheck();
            ServletContainerHealthCheck servletCheck = new ServletContainerHealthCheck();
            
            // Set servlet context for container health check
            servletCheck.setServletContext(getServletContext());
            
            healthStateManager.registerHealthCheck(applicationCheck);
            healthStateManager.registerHealthCheck(systemCheck);
            healthStateManager.registerHealthCheck(threadCheck);
            healthStateManager.registerHealthCheck(gcCheck);
            healthStateManager.registerHealthCheck(servletCheck);
            
            // CDI initialization check - readiness only
            CdiInitializationHealthCheck cdiCheck = new CdiInitializationHealthCheck();
            healthStateManager.registerHealthCheck(cdiCheck);
            
            // Shutdown status check - readiness only (fails when shutdown begins to stop new traffic)
            ShutdownHealthCheck shutdownCheck = new ShutdownHealthCheck();
            healthStateManager.registerHealthCheck(shutdownCheck);
            
            Logger.info(this, "Core health checks registered successfully");
        } catch (Exception e) {
            Logger.error(this, "Failed to register core health checks", e);
        }
    }
    
    
    /**
     * Sets up CDI integration if available, without blocking if CDI is not ready
     */
    private void setupCdiIntegration() {
        try {
            // Try to get CDI container and health check registry
            CDI<Object> cdi = CDI.current();
            if (cdi == null) {
                Logger.debug(this, "CDI container is null - CDI not yet available");
                return;
            }
            
            HealthCheckRegistry registry = cdi.select(HealthCheckRegistry.class).get();
            if (registry == null) {
                Logger.warn(this, "HealthCheckRegistry bean not found in CDI container");
                return;
            }
            
            // Set up the coordination between registry and CDI initialization check
            CdiInitializationHealthCheck cdiInitCheck = new CdiInitializationHealthCheck();
            registry.setCdiInitializationHealthCheck(cdiInitCheck);
            registry.setStateManager(healthStateManager);
            
            Logger.info(this, "CDI integration successfully enabled for health checks");
        } catch (IllegalStateException e) {
            // CDI container not available - this is expected during early startup
            Logger.debug(this, "CDI container not available (early startup): " + e.getMessage());
        } catch (Exception e) {
            // Unexpected error during CDI integration
            Logger.warn(this, "Unexpected error during CDI integration setup: " + e.getMessage(), e);
            
            // The CDI initialization check will detect this and report appropriately
            // Health system can still function with core checks
        }
    }
    
    // ===== PUBLIC HEALTH SERVICE METHODS (called by ManagementServlet) =====
    
    /**
     * Liveness probe - ONLY checks if the core application is alive and not stuck.
     * This should NEVER check external dependencies to avoid cascade failures.
     * Only fails for unrecoverable situations that require pod restart.
     * 
     * Called by ManagementServlet for /dotmgt/livez endpoint.
     */
    public void handleLivenessProbe(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HealthResponse health = healthStateManager.getLivenessHealth();
        
        // For liveness: Only fail on DOWN status - DEGRADED, UNKNOWN, and UP are considered "alive"
        // This prevents unnecessary pod restarts for non-critical issues
        boolean isAlive = health.checks().stream()
            .noneMatch(check -> check.status() == HealthStatus.DOWN);
        
        // Check if there are any degraded conditions to report
        boolean hasDegraded = health.checks().stream()
            .anyMatch(check -> check.status() == HealthStatus.DEGRADED);
        
        // Intelligent logging - only log on state changes
        livenessCheckCount++;
        logLivenessStateChange(isAlive, hasDegraded);
        
        if (isAlive) {
            response.setStatus(HttpServletResponse.SC_OK);
            writeMinimalResponse(response, "alive");
        } else {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            writeMinimalResponse(response, "unhealthy");
        }
    }
    
    /**
     * Readiness probe - checks if the application is ready to serve traffic.
     * Can include external dependencies since failure just removes from load balancer.
     * More comprehensive than liveness but doesn't trigger restarts.
     * 
     * Called by ManagementServlet for /dotmgt/readyz endpoint.
     */
    public void handleReadinessProbe(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HealthResponse health = healthStateManager.getReadinessHealth();
        
        // For readiness: Only fail on DOWN status - DEGRADED, UNKNOWN, and UP are considered "ready"
        // This allows degraded services to continue receiving traffic while monitoring issues
        boolean isReady = health.checks().stream()
            .noneMatch(check -> check.status() == HealthStatus.DOWN);
        
        // Check if there are any degraded conditions to report
        boolean hasDegraded = health.checks().stream()
            .anyMatch(check -> check.status() == HealthStatus.DEGRADED);
        
        // Intelligent logging - only log on state changes
        readinessCheckCount++;
        logReadinessStateChange(isReady, hasDegraded);
        
        if (isReady) {
            response.setStatus(HttpServletResponse.SC_OK);
            writeMinimalResponse(response, "ready");
        } else {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            writeMinimalResponse(response, "not ready");
        }
    }
    
    /**
     * Full health endpoint - returns detailed health information for monitoring.
     * 
     * Called by ManagementServlet for /dotmgt/health endpoint.
     */
    public void handleFullHealth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HealthResponse health = healthStateManager.getCurrentHealth();
        
        // Return 503 if any checks are down, 200 otherwise
        if (health.status() == HealthStatus.DOWN) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }
        
        writeJsonResponse(response, health);
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Writes a minimal response for Kubernetes probes to avoid response size limitations.
     */
    private void writeMinimalResponse(HttpServletResponse response, String status) throws IOException {
        Logger.debug(this, "writeMinimalResponse called with status: " + status);
        
        response.setContentType("text/plain");
        response.setCharacterEncoding(CHARSET_UTF8);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(status);
            writer.flush();
            Logger.debug(this, "Successfully wrote minimal response with status: " + status);
        } catch (Exception e) {
            Logger.error(this, "Failed to write minimal response: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = response.getWriter()) {
                writer.write("error");
                writer.flush();
            } catch (Exception fallbackError) {
                Logger.error(this, "Failed to write fallback error response: " + fallbackError.getMessage(), fallbackError);
            }
        }
    }
    
    /**
     * Writes a JSON response
     */
    private void writeJsonResponse(HttpServletResponse response, HealthResponse health) throws IOException {
        response.setContentType(CONTENT_TYPE_JSON);
        response.setCharacterEncoding(CHARSET_UTF8);
        
        try (PrintWriter writer = response.getWriter()) {
            objectMapper.writeValue(writer, health);
        } catch (Exception e) {
            // Fallback if JSON serialization fails
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = response.getWriter()) {
                writer.write("{\"status\":\"DOWN\",\"error\":\"Failed to serialize health response\"}");
            }
            Logger.error(this, "Failed to serialize health response", e);
        }
    }
    
    /**
     * Logs liveness state changes with startup context awareness
     */
    private void logLivenessStateChange(boolean isCurrentlyAlive, boolean hasDegraded) {
        HealthResponse health = healthStateManager.getLivenessHealth();
        boolean isStartupPhase = healthStateManager.isInStartupPhase();
        boolean hasEverSucceeded = healthStateManager.hasLivenessEverSucceeded();
        String startupAge = healthStateManager.getStartupAge();
        
        // Check if this is the first time we're checking liveness
        if (lastLivenessState == null) {
            if (isCurrentlyAlive) {
                // Check if HealthStateManager has already logged first success
                if (hasEverSucceeded) {
                    // HealthStateManager already achieved and logged first success - don't duplicate
                    Logger.debug(this, "Liveness probe: First probe request after system startup success (check #" + livenessCheckCount + ") [STARTUP PHASE - " + startupAge + "]");
                } else {
                    // This is truly the first success in the system
                    String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + "]" : "[OPERATIONAL]";
                    String message = "Liveness probe: Initial check PASSED - application is alive (check #" + livenessCheckCount + ") " + phaseInfo;
                    if (hasDegraded) {
                        message += " [WARNING: Degraded conditions detected but not blocking liveness]";
                    }
                    Logger.info(this, message);
                }
            } else {
                // Handle expected startup race condition more intelligently
                if (isStartupPhase && !hasEverSucceeded && livenessCheckCount <= 2) {
                    // This is the expected startup race condition - suppress or use DEBUG level
                    String failureDetails = getFailureDetails(health, "liveness");
                    Logger.debug(this, "Liveness probe: Initial startup check waiting for system readiness - " + 
                        failureDetails + " (check #" + livenessCheckCount + ") [STARTUP PHASE - " + startupAge + " - EXPECTED]");
                } else {
                    // Unexpected failure or persistent failure - log normally
                    String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + " - UNEXPECTED]" : "[OPERATIONAL - CRITICAL FAILURE]";
                    String failureDetails = getFailureDetails(health, "liveness");
                    Logger.warn(this, "Liveness probe: Initial check FAILED - " + failureDetails + " (check #" + livenessCheckCount + ") " + phaseInfo);
                }
            }
            lastLivenessState = isCurrentlyAlive;
            lastLivenessHadDegraded = hasDegraded;
            return;
        }
        
        // Check for state transitions
        boolean aliveStateChanged = lastLivenessState != isCurrentlyAlive;
        boolean degradedStateChanged = lastLivenessHadDegraded != hasDegraded;
        
        if (aliveStateChanged) {
            if (isCurrentlyAlive) {
                // Transition from failed to healthy - but distinguish between first success and recovery
                if (hasEverSucceeded && lastLivenessState == false && livenessCheckCount > 2) {
                    // This is a genuine recovery from a previous failure (not initial startup race)
                    // Only consider it recovery if we've had multiple checks (not just startup timing)
                    String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + "]" : "[OPERATIONAL]";
                    String message = "Liveness probe: RECOVERED - application is now alive after failure (check #" + livenessCheckCount + ") " + phaseInfo;
                    if (hasDegraded) {
                        message += " [WARNING: Degraded conditions still present]";
                    }
                    Logger.info(this, message);
                } else if (!hasEverSucceeded) {
                    // This is the first success detected by probe (not already logged by HealthStateManager)
                    String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + "]" : "[OPERATIONAL]";
                    String message = "Liveness probe: FIRST SUCCESS - application is now alive (check #" + livenessCheckCount + ") " + phaseInfo;
                    if (hasDegraded) {
                        message += " [WARNING: Degraded conditions detected]";
                    }
                    Logger.info(this, message);
                } else {
                    // HealthStateManager already logged first success, this is just probe catching up
                    // Don't log as recovery since it's just the probe servlet catching up to reality
                    Logger.debug(this, "Liveness probe: Probe successful after system startup (check #" + livenessCheckCount + ") [STARTUP PHASE - " + startupAge + "]");
                }
            } else {
                // Transition from healthy to failed
                String phaseInfo;
                String logLevel;
                if (isStartupPhase || !hasEverSucceeded) {
                    phaseInfo = "[STARTUP PHASE - " + startupAge + " - EXPECTED DURING STARTUP]";
                    logLevel = "INFO";
                } else {
                    phaseInfo = "[OPERATIONAL - CRITICAL FAILURE - POD RESTART RECOMMENDED]";
                    logLevel = "ERROR";
                }
                
                String failureDetails = getFailureDetails(health, "liveness");
                String message = "Liveness probe: FAILED - " + failureDetails + " (check #" + livenessCheckCount + ") " + phaseInfo;
                
                if ("ERROR".equals(logLevel)) {
                    Logger.error(this, message);
                } else {
                    Logger.info(this, message);
                }
            }
            lastLivenessState = isCurrentlyAlive;
        } else if (degradedStateChanged && isCurrentlyAlive) {
            // Only log degraded changes if probe is not failing (to avoid noise during failures)
            String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + "]" : "[OPERATIONAL]";
            if (hasDegraded) {
                String degradedDetails = getDegradedDetails(health, "liveness");
                Logger.warn(this, "Liveness probe: DEGRADED conditions detected - " + degradedDetails + " (check #" + livenessCheckCount + ") " + phaseInfo);
            } else {
                Logger.info(this, "Liveness probe: RECOVERED from degraded conditions - application fully healthy (check #" + livenessCheckCount + ") " + phaseInfo);
            }
        }
        
        // Update degraded state tracking
        lastLivenessHadDegraded = hasDegraded;
    }
    
    /**
     * Logs readiness state changes with startup context awareness
     */
    private void logReadinessStateChange(boolean isCurrentlyReady, boolean hasDegraded) {
        HealthResponse health = healthStateManager.getReadinessHealth();
        boolean isStartupPhase = healthStateManager.isInStartupPhase();
        boolean hasEverSucceeded = healthStateManager.hasReadinessEverSucceeded();
        String startupAge = healthStateManager.getStartupAge();
        
        // Check if this is the first time we're checking readiness
        if (lastReadinessState == null) {
            if (isCurrentlyReady) {
                // Check if HealthStateManager has already logged first success
                if (hasEverSucceeded) {
                    // HealthStateManager already achieved and logged first success - don't duplicate
                    Logger.debug(this, "Readiness probe: First probe request after system startup success (check #" + readinessCheckCount + ") [STARTUP PHASE - " + startupAge + "]");
                } else {
                    // This is truly the first success in the system
                    String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + "]" : "[OPERATIONAL]";
                    String message = "Readiness probe: Initial check PASSED - application ready to serve traffic (check #" + readinessCheckCount + ") " + phaseInfo;
                    if (hasDegraded) {
                        message += " [WARNING: Degraded conditions detected but not blocking traffic]";
                    }
                    Logger.info(this, message);
                }
            } else {
                // Handle expected startup failures more intelligently
                if (isStartupPhase && !hasEverSucceeded && readinessCheckCount <= 3) {
                    // Expected startup failure - use DEBUG level to reduce noise
                    String failureDetails = getFailureDetails(health, "readiness");
                    Logger.debug(this, "Readiness probe: Initial startup check waiting for dependencies - " + 
                        failureDetails + " (check #" + readinessCheckCount + ") [STARTUP PHASE - " + startupAge + " - EXPECTED]");
                } else {
                    // Unexpected or persistent failure - log normally
                    String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + " - UNEXPECTED]" : "[OPERATIONAL - DEPENDENCY ISSUE]";
                    String failureDetails = getFailureDetails(health, "readiness");
                    Logger.info(this, "Readiness probe: Initial check FAILED - " + failureDetails + " (check #" + readinessCheckCount + ") " + phaseInfo);
                }
            }
            lastReadinessState = isCurrentlyReady;
            lastReadinessHadDegraded = hasDegraded;
            return;
        }
        
        // Check for state transitions
        boolean readyStateChanged = lastReadinessState != isCurrentlyReady;
        boolean degradedStateChanged = lastReadinessHadDegraded != hasDegraded;
        
        if (readyStateChanged) {
            if (isCurrentlyReady) {
                // Transition from not ready to ready - but distinguish between first success and recovery
                if (hasEverSucceeded && lastReadinessState == false && readinessCheckCount > 3) {
                    // This is a genuine recovery from a previous failure (not initial startup race)
                    // Only consider it recovery if we've had multiple checks (not just startup timing)
                    String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + "]" : "[OPERATIONAL]";
                    String message = "Readiness probe: RECOVERED - application is now ready to serve traffic (check #" + readinessCheckCount + ") " + phaseInfo;
                    if (hasDegraded) {
                        message += " [WARNING: Degraded conditions still present]";
                    }
                    Logger.info(this, message);
                } else if (!hasEverSucceeded) {
                    // This is the first success detected by probe (not already logged by HealthStateManager)
                    String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + "]" : "[OPERATIONAL]";
                    String message = "Readiness probe: FIRST SUCCESS - application is now ready to serve traffic (check #" + readinessCheckCount + ") " + phaseInfo;
                    if (hasDegraded) {
                        message += " [WARNING: Degraded conditions detected]";
                    }
                    Logger.info(this, message);
                } else {
                    // HealthStateManager already logged first success, this is just probe catching up
                    // Don't log as recovery since it's just the probe servlet catching up to reality
                    Logger.debug(this, "Readiness probe: Probe successful after system startup (check #" + readinessCheckCount + ") [STARTUP PHASE - " + startupAge + "]");
                }
            } else {
                // Transition from ready to not ready
                String phaseInfo;
                String logLevel;
                if (isStartupPhase || !hasEverSucceeded) {
                    phaseInfo = "[STARTUP PHASE - " + startupAge + " - EXPECTED DURING STARTUP]";
                    logLevel = "INFO";
                } else {
                    phaseInfo = "[OPERATIONAL - DEPENDENCY ISSUE - REMOVED FROM LOAD BALANCER]";
                    logLevel = "WARN";
                }
                
                String failureDetails = getFailureDetails(health, "readiness");
                String message = "Readiness probe: FAILED - " + failureDetails + " (check #" + readinessCheckCount + ") " + phaseInfo;
                
                if ("WARN".equals(logLevel)) {
                    Logger.warn(this, message);
                } else {
                    Logger.info(this, message);
                }
            }
            lastReadinessState = isCurrentlyReady;
        } else if (degradedStateChanged && isCurrentlyReady) {
            // Only log degraded changes if probe is not failing (to avoid noise during failures)
            String phaseInfo = isStartupPhase ? "[STARTUP PHASE - " + startupAge + "]" : "[OPERATIONAL]";
            if (hasDegraded) {
                String degradedDetails = getDegradedDetails(health, "readiness");
                Logger.warn(this, "Readiness probe: DEGRADED conditions detected - " + degradedDetails + " (check #" + readinessCheckCount + ") " + phaseInfo);
            } else {
                Logger.info(this, "Readiness probe: RECOVERED from degraded conditions - application fully ready (check #" + readinessCheckCount + ") " + phaseInfo);
            }
        }
        
        // Update degraded state tracking
        lastReadinessHadDegraded = hasDegraded;
    }
    
    /**
     * Extracts failure details from health check results for logging
     */
    private String getFailureDetails(HealthResponse health, String probeType) {
        List<String> failureMessages = health.checks().stream()
            .filter(check -> check.status() == HealthStatus.DOWN)
            .map(check -> {
                String message = check.name() + ": " + 
                    (check.error() != null ? check.error() : check.message());
                return message;
            })
            .collect(Collectors.toList());
        
        if (failureMessages.isEmpty()) {
            return "No specific failures detected";
        }
        
        if (failureMessages.size() == 1) {
            return failureMessages.get(0);
        }
        
        // Multiple failures - summarize
        return String.format("%d checks failed: [%s]", 
            failureMessages.size(), 
            String.join(", ", failureMessages));
    }
    
    /**
     * Extracts degraded condition details from health check results for logging
     */
    private String getDegradedDetails(HealthResponse health, String probeType) {
        List<String> degradedMessages = health.checks().stream()
            .filter(check -> check.status() == HealthStatus.DEGRADED)
            .map(check -> check.name() + ": " + check.message())
            .collect(Collectors.toList());
        
        if (degradedMessages.isEmpty()) {
            return "No specific degraded conditions detected";
        }
        
        if (degradedMessages.size() == 1) {
            return degradedMessages.get(0);
        }
        
        // Multiple degraded conditions - summarize
        return String.format("%d checks degraded: [%s]", 
            degradedMessages.size(), 
            String.join(", ", degradedMessages));
    }
    
    @Override
    public void destroy() {
        if (healthStateManager != null) {
            healthStateManager.shutdown();
        }
        super.destroy();
        Logger.info(this, "HealthProbeServlet destroyed");
    }
}