package com.dotcms.health.model;

import java.time.Instant;

/**
 * Factory class for creating HealthCheckFailureWindow instances.
 * This class provides static factory methods that were moved from the interface
 * to avoid circular dependency issues with the generated Immutables builder.
 */
public final class HealthCheckFailureWindowFactory {

    private HealthCheckFailureWindowFactory() {
        // Utility class - no instantiation
    }

    /**
     * Creates a new failure window for a passing check
     */
    public static HealthCheckFailureWindow passing(String checkName) {
        return HealthCheckFailureWindow.builder()
            .checkName(checkName)
            .consecutiveFailures(0)
            .rawStatus(HealthStatus.UP)
            .effectiveStatus(HealthStatus.UP)
            .lastEvaluated(Instant.now())
            .build();
    }

    /**
     * Creates a new failure window for a check that just started failing
     */
    public static HealthCheckFailureWindow startingFailure(String checkName, HealthStatus rawStatus, String message) {
        return HealthCheckFailureWindow.builder()
            .checkName(checkName)
            .failureSequenceStart(Instant.now())
            .consecutiveFailures(1)
            .rawStatus(rawStatus)
            .effectiveStatus(HealthStatus.DEGRADED) // Start with degraded during tolerance window
            .lastEvaluated(Instant.now())
            .toleranceMessage("Check failing but within tolerance window: " + message)
            .build();
    }
} 