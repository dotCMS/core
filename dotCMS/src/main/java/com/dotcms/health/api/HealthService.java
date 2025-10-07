package com.dotcms.health.api;

import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthResponse;

import java.util.List;
import java.util.Optional;

/**
 * Internal service interface for programmatic access to health check status.
 * This service provides methods to query overall health, individual health checks,
 * and health status by category (liveness vs readiness).
 * 
 * This service is CDI-managed and can be injected into other services or components
 * that need to programmatically check system health status.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @Inject
 * private HealthService healthService;
 * 
 * // Get overall health status
 * HealthResponse overallHealth = healthService.getOverallHealth();
 * 
 * // Check if system is ready to serve traffic
 * boolean isReady = healthService.isReady();
 * 
 * // Check specific services using convenience methods
 * boolean dbHealthy = healthService.isDatabaseHealthy();
 * boolean searchHealthy = healthService.isSearchServiceHealthy();
 * 
 * // Get detailed health information
 * Optional<HealthCheckResult> dbHealth = healthService.getDatabaseHealth();
 * Optional<HealthCheckResult> searchHealth = healthService.getSearchServiceHealth();
 * 
 * // Force refresh and check critical services
 * boolean dbRecovered = healthService.refreshAndCheckDatabaseHealth(true);
 * if (!dbRecovered) {
 *     // Handle database outage
 * }
 * }
 * </pre>
 */
public interface HealthService {
    
    /**
     * Get the overall health status including all registered health checks
     * 
     * @return HealthResponse containing all health check results
     */
    HealthResponse getOverallHealth();
    
    /**
     * Get liveness health status (core checks only, no external dependencies)
     * 
     * @return HealthResponse containing only liveness health checks
     */
    HealthResponse getLivenessHealth();
    
    /**
     * Get readiness health status (all checks including dependencies)
     * 
     * @return HealthResponse containing all health checks for readiness assessment
     */
    HealthResponse getReadinessHealth();
    
    /**
     * Get a specific health check result by name
     * 
     * @param checkName the name of the health check to retrieve
     * @return Optional containing the health check result if found, empty otherwise
     */
    Optional<HealthCheckResult> getHealthCheck(String checkName);
    
    /**
     * Get all currently registered health checks
     * 
     * @return List of all health check results
     */
    List<HealthCheckResult> getAllHealthChecks();
    
    /**
     * Get only liveness health checks
     * 
     * @return List of health check results for liveness checks only
     */
    List<HealthCheckResult> getLivenessHealthChecks();
    
    /**
     * Get only readiness health checks
     * 
     * @return List of health check results for readiness checks only
     */
    List<HealthCheckResult> getReadinessHealthChecks();
    
    /**
     * Check if the system is currently alive (liveness probe equivalent)
     * 
     * @return true if all liveness checks are UP or UNKNOWN, false otherwise
     */
    boolean isAlive();
    
    /**
     * Check if the system is currently ready to serve traffic (readiness probe equivalent)
     * 
     * @return true if all readiness checks are UP, false otherwise
     */
    boolean isReady();
    
    /**
     * Check if a specific health check is currently UP
     * 
     * @param checkName the name of the health check to verify
     * @return true if the check exists and is UP, false otherwise
     */
    boolean isHealthCheckUp(String checkName);
    
    /**
     * Get the names of all registered health checks
     * 
     * @return List of health check names
     */
    List<String> getHealthCheckNames();
    
    /**
     * Force refresh of all health checks (triggers immediate re-evaluation)
     * Use with caution as this can be resource-intensive
     */
    void refreshHealthChecks();
    
    /**
     * Force refresh of a specific health check
     * 
     * @param checkName the name of the health check to refresh
     * @return true if the check was found and refreshed, false otherwise
     */
    boolean refreshHealthCheck(String checkName);
    
    /**
     * Force refresh of a specific health check with optional blocking behavior
     * 
     * @param checkName the name of the health check to refresh
     * @param blocking whether to wait for the refresh to complete before returning
     */
    void forceRefreshHealthCheck(String checkName, boolean blocking);
    
    /**
     * Check if the database is currently available and healthy.
     * This is a convenience method that checks the "database" health check.
     * 
     * @return true if database is UP, false otherwise (DOWN, DEGRADED, or UNKNOWN)
     */
    boolean isDatabaseHealthy();
    
    /**
     * Get the current database health status and details.
     * This is a convenience method for getting the "database" health check result.
     * 
     * @return Optional containing the database health check result if available, empty otherwise
     */
    Optional<HealthCheckResult> getDatabaseHealth();
    
    /**
     * Check if Elasticsearch (search service) is currently available and healthy.
     * This is a convenience method that checks the "elasticsearch" health check.
     * 
     * @return true if Elasticsearch is UP, false otherwise (DOWN, DEGRADED, or UNKNOWN)
     */
    boolean isSearchServiceHealthy();
    
    /**
     * Get the current Elasticsearch (search service) health status and details.
     * This is a convenience method for getting the "elasticsearch" health check result.
     * 
     * @return Optional containing the Elasticsearch health check result if available, empty otherwise
     */
    Optional<HealthCheckResult> getSearchServiceHealth();
    
    /**
     * Force refresh the database health check and return its current status.
     * This triggers an immediate database connectivity test.
     * 
     * @param blocking whether to wait for the refresh to complete before returning the result
     * @return true if database is healthy after refresh, false otherwise
     */
    boolean refreshAndCheckDatabaseHealth(boolean blocking);
    
    /**
     * Force refresh the Elasticsearch health check and return its current status.
     * This triggers an immediate Elasticsearch connectivity test.
     * 
     * @param blocking whether to wait for the refresh to complete before returning the result
     * @return true if Elasticsearch is healthy after refresh, false otherwise
     */
    boolean refreshAndCheckSearchServiceHealth(boolean blocking);
} 