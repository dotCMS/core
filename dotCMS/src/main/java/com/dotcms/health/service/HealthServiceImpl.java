package com.dotcms.health.service;

import com.dotcms.health.api.HealthService;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthResponse;
import com.dotcms.health.model.HealthStatus;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CDI-based implementation of HealthService that provides programmatic access
 * to health check status through the HealthStateManager.
 * 
 * This service acts as a facade over the HealthStateManager, providing
 * convenient methods for internal components to query health status
 * without needing direct access to the state manager.
 * 
 * The service is thread-safe and designed for high-frequency access
 * by internal monitoring systems and admin interfaces.
 */
@ApplicationScoped
public class HealthServiceImpl implements HealthService {
    
    // Use singleton instance shared with servlet (no CDI injection needed)
    private final HealthStateManager healthStateManager = HealthStateManager.getInstance();
    
    @Override
    public HealthResponse getOverallHealth() {
        try {
            return healthStateManager.getCurrentHealth();
        } catch (Exception e) {
            Logger.error(this, "Failed to get overall health status", e);
            return createErrorResponse("Failed to retrieve overall health status");
        }
    }
    
    @Override
    public HealthResponse getLivenessHealth() {
        try {
            return healthStateManager.getLivenessHealth();
        } catch (Exception e) {
            Logger.error(this, "Failed to get liveness health status", e);
            return createErrorResponse("Failed to retrieve liveness health status");
        }
    }
    
    @Override
    public HealthResponse getReadinessHealth() {
        try {
            return healthStateManager.getReadinessHealth();
        } catch (Exception e) {
            Logger.error(this, "Failed to get readiness health status", e);
            return createErrorResponse("Failed to retrieve readiness health status");
        }
    }
    
    @Override
    public Optional<HealthCheckResult> getHealthCheck(String checkName) {
        if (checkName == null || checkName.trim().isEmpty()) {
            Logger.warn(this, "Health check name cannot be null or empty");
            return Optional.empty();
        }
        
        try {
            return healthStateManager.getHealthCheckResult(checkName.trim());
        } catch (Exception e) {
            Logger.error(this, "Failed to get health check: " + checkName, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<HealthCheckResult> getAllHealthChecks() {
        try {
            return healthStateManager.getCurrentHealth().checks();
        } catch (Exception e) {
            Logger.error(this, "Failed to get all health checks", e);
            return List.of();
        }
    }
    
    @Override
    public List<HealthCheckResult> getLivenessHealthChecks() {
        try {
            return healthStateManager.getLivenessHealth().checks();
        } catch (Exception e) {
            Logger.error(this, "Failed to get liveness health checks", e);
            return List.of();
        }
    }
    
    @Override
    public List<HealthCheckResult> getReadinessHealthChecks() {
        try {
            return healthStateManager.getReadinessHealth().checks();
        } catch (Exception e) {
            Logger.error(this, "Failed to get readiness health checks", e);
            return List.of();
        }
    }
    
    @Override
    public boolean isAlive() {
        try {
            HealthResponse livenessHealth = healthStateManager.getLivenessHealth();
            return livenessHealth.checks().stream()
                .allMatch(check -> check.status() == HealthStatus.UP || check.status() == HealthStatus.UNKNOWN);
        } catch (Exception e) {
            Logger.error(this, "Failed to check liveness status", e);
            return false;
        }
    }
    
    @Override
    public boolean isReady() {
        try {
            HealthResponse readinessHealth = healthStateManager.getReadinessHealth();
            return readinessHealth.status() == HealthStatus.UP;
        } catch (Exception e) {
            Logger.error(this, "Failed to check readiness status", e);
            return false;
        }
    }
    
    @Override
    public boolean isHealthCheckUp(String checkName) {
        if (checkName == null || checkName.trim().isEmpty()) {
            return false;
        }
        
        try {
            Optional<HealthCheckResult> result = getHealthCheck(checkName.trim());
            return result.map(check -> check.status() == HealthStatus.UP).orElse(false);
        } catch (Exception e) {
            Logger.error(this, "Failed to check health status for: " + checkName, e);
            return false;
        }
    }
    
    @Override
    public List<String> getHealthCheckNames() {
        try {
            return getAllHealthChecks().stream()
                .map(HealthCheckResult::name)
                .collect(Collectors.toList());
        } catch (Exception e) {
            Logger.error(this, "Failed to get health check names", e);
            return List.of();
        }
    }
    
    @Override
    public void refreshHealthChecks() {
        try {
            healthStateManager.forceRefresh();
            Logger.info(this, "Health checks refresh triggered");
        } catch (Exception e) {
            Logger.error(this, "Failed to refresh health checks", e);
        }
    }
    
    @Override
    public boolean refreshHealthCheck(String checkName) {
        if (checkName == null || checkName.trim().isEmpty()) {
            Logger.warn(this, "Health check name cannot be null or empty for refresh");
            return false;
        }
        
        try {
            healthStateManager.forceRefreshHealthCheck(checkName.trim(), false);
            Logger.info(this, "Health check refreshed: " + checkName);
            return true;
        } catch (Exception e) {
            Logger.error(this, "Failed to refresh health check: " + checkName, e);
            return false;
        }
    }
    
    @Override
    public void forceRefreshHealthCheck(String checkName, boolean blocking) {
        if (checkName == null || checkName.trim().isEmpty()) {
            Logger.warn(this, "Health check name cannot be null or empty for force refresh");
            return;
        }
        
        try {
            healthStateManager.forceRefreshHealthCheck(checkName.trim(), blocking);
            // Note: HealthStateManager already logs this operation, so we avoid duplicate logging here
        } catch (Exception e) {
            Logger.error(this, "Failed to force refresh health check: " + checkName, e);
        }
    }
    
    @Override
    public boolean isDatabaseHealthy() {
        return isHealthCheckUp("database");
    }
    
    @Override
    public Optional<HealthCheckResult> getDatabaseHealth() {
        return getHealthCheck("database");
    }
    
    @Override
    public boolean isSearchServiceHealthy() {
        return isHealthCheckUp("elasticsearch");
    }
    
    @Override
    public Optional<HealthCheckResult> getSearchServiceHealth() {
        return getHealthCheck("elasticsearch");
    }
    
    @Override
    public boolean refreshAndCheckDatabaseHealth(boolean blocking) {
        try {
            forceRefreshHealthCheck("database", blocking);
            return isDatabaseHealthy();
        } catch (Exception e) {
            Logger.error(this, "Failed to refresh and check database health", e);
            return false;
        }
    }
    
    @Override
    public boolean refreshAndCheckSearchServiceHealth(boolean blocking) {
        try {
            forceRefreshHealthCheck("elasticsearch", blocking);
            return isSearchServiceHealthy();
        } catch (Exception e) {
            Logger.error(this, "Failed to refresh and check search service health", e);
            return false;
        }
    }
    
    /**
     * Creates an error response when health status cannot be retrieved
     */
    private HealthResponse createErrorResponse(String errorMessage) {
        return HealthResponse.builder()
            .status(HealthStatus.DOWN)
            .checks(List.of())
            .output(errorMessage)
            .timestamp(Instant.now())
            .build();
    }
} 