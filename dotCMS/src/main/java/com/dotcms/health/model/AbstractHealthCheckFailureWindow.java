package com.dotcms.health.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Abstract interface for tracking health check failure windows.
 * This model tracks consecutive failures and determines when to escalate from DEGRADED to DOWN status.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = HealthCheckFailureWindow.class)
@JsonDeserialize(as = HealthCheckFailureWindow.class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public interface AbstractHealthCheckFailureWindow {

    /**
     * The name of the health check this failure window tracks
     */
    String checkName();

    /**
     * The timestamp when the current failure sequence started
     * Empty if the check is currently passing
     */
    Optional<Instant> failureSequenceStart();

    /**
     * The number of consecutive failures in the current sequence
     */
    @Value.Default
    default int consecutiveFailures() {
        return 0;
    }

    /**
     * The most recent actual health status (before tolerance logic is applied)
     * This is the "raw" status that would be reported without failure tolerance
     */
    HealthStatus rawStatus();

    /**
     * The effective status after applying failure tolerance logic
     * This is what gets reported to the health system
     */
    HealthStatus effectiveStatus();

    /**
     * Whether this check is currently in a failure tolerance window
     * (failing but not yet escalated to DOWN)
     */
    @Value.Default
    default boolean inToleranceWindow() {
        return failureSequenceStart().isPresent() && 
               rawStatus() == HealthStatus.DOWN && 
               effectiveStatus() == HealthStatus.DEGRADED;
    }

    /**
     * The timestamp of the last status evaluation
     */
    Instant lastEvaluated();

    /**
     * Optional message explaining the current tolerance state
     */
    Optional<String> toleranceMessage();

    /**
     * Whether circuit breaker warnings have been logged for this failure sequence
     * Used to prevent log spam during sustained outages
     */
    @Value.Default
    default boolean circuitBreakerLogged() {
        return false;
    }
}