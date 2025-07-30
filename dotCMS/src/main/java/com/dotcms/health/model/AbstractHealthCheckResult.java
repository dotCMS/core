package com.dotcms.health.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Abstract interface for an immutable HealthCheckResult class.
 * Represents the result of an individual health check.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = HealthCheckResult.class)
@JsonDeserialize(as = HealthCheckResult.class)
@JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude null and empty Optional values
public interface AbstractHealthCheckResult {

    /**
     * The name/identifier of the health check
     */
    String name();

    /**
     * The current status of the health check
     */
    HealthStatus status();

    /**
     * Optional message providing additional details about the health check
     */
    Optional<String> message();

    /**
     * Optional error message if the check failed
     */
    Optional<String> error();

    /**
     * The timestamp when this check was last performed
     */
    Instant lastChecked();

    /**
     * Duration in milliseconds that the check took to complete (defaults to 0 if not measured)
     */
    @Value.Default
    default long durationMs() {
        return 0L;
    }
    
    /**
     * Indicates whether this result has been modified by monitor mode.
     * When true, the status has been converted from its original value for deployment safety.
     * This allows the tolerance manager to handle these results appropriately without 
     * applying additional tolerance logic.
     */
    @Value.Default
    default boolean monitorModeApplied() {
        return false;
    }
    
    /**
     * Optional structured data containing machine-parsable information specific to this health check.
     * This provides a fixed schema for health check results while allowing flexibility for
     * health check specific data such as:
     * - Database version and connection pool stats
     * - Memory usage percentages and thresholds
     * - Thread counts and deadlock information
     * - Performance metrics and system resource data
     * 
     * The data field complements the human-readable message field by providing structured
     * information that monitoring systems and APIs can easily parse and process.
     */
    Optional<Map<String, Object>> data();
}