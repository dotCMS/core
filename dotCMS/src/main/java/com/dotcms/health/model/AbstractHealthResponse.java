package com.dotcms.health.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.List;
import org.immutables.value.Value;

/**
 * Abstract interface for an immutable HealthResponse class.
 * Represents the overall health status response containing multiple health checks.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = HealthResponse.class)
@JsonDeserialize(as = HealthResponse.class)
public interface AbstractHealthResponse {

    /**
     * The overall status derived from all individual health checks
     */
    HealthStatus status();

    /**
     * List of individual health check results
     */
    List<HealthCheckResult> checks();

    /**
     * The timestamp when this response was generated
     */
    Instant timestamp();

    /**
     * Optional version information about the application
     */
    @Value.Default
    default String version() {
        return "unknown";
    }

    /**
     * Computes the overall status based on individual check results.
     * If any check is DOWN, overall status is DOWN.
     * If all checks are UP, overall status is UP.
     * Otherwise, overall status is UNKNOWN.
     */
    @Value.Derived
    default HealthStatus derivedStatus() {
        if (checks().isEmpty()) {
            return HealthStatus.UNKNOWN;
        }
        
        boolean hasDown = checks().stream()
            .anyMatch(check -> check.status() == HealthStatus.DOWN);
        
        if (hasDown) {
            return HealthStatus.DOWN;
        }
        
        boolean allUp = checks().stream()
            .allMatch(check -> check.status() == HealthStatus.UP);
        
        return allUp ? HealthStatus.UP : HealthStatus.UNKNOWN;
    }
}