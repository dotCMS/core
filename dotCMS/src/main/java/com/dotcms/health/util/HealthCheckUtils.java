package com.dotcms.health.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class providing common health check patterns and operations.
 * These utilities can be used across different health checks to avoid code duplication.
 * 
 * This class follows dotCMS development rules:
 * - Uses dotCMS Config for configuration
 * - Uses dotCMS Logger for logging
 * - Never logs sensitive information
 * - Always provides generic error messages to external callers
 */
public final class HealthCheckUtils {
    
    private static final ExecutorService timeoutExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "health-check-timeout");
        t.setDaemon(true);
        return t;
    });
    
    private HealthCheckUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Executes an operation with a timeout.
     * 
     * @param operation the operation to execute
     * @param timeoutMs timeout in milliseconds
     * @param operationName name of the operation for error messages
     * @return the result of the operation
     * @throws Exception if the operation fails or times out
     */
    public static <T> T executeWithTimeout(Callable<T> operation, long timeoutMs, String operationName) throws Exception {
        Future<T> future = timeoutExecutor.submit(operation);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new Exception(operationName + " timed out after " + timeoutMs + "ms");
        }
    }


    
    /**
     * Formats a duration in milliseconds to a human-readable string.
     * 
     * @param durationMs duration in milliseconds
     * @return formatted duration string
     */
    public static String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.1fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }
    
    /**
     * Creates a standard success message with timing information.
     * 
     * @param operation the operation that was performed
     * @param durationMs how long it took
     * @return formatted success message
     */
    public static String createSuccessMessage(String operation, long durationMs) {
        return String.format("%s (response time: %s)", operation, formatDuration(durationMs));
    }
    
    /**
     * Validates that a timeout value is reasonable (between 100ms and 30s).
     * 
     * @param timeoutMs the timeout to validate
     * @param defaultTimeoutMs the default to use if invalid
     * @return a valid timeout value
     */
    public static long validateTimeout(long timeoutMs, long defaultTimeoutMs) {
        if (timeoutMs < 100 || timeoutMs > 30000) {
            return defaultTimeoutMs;
        }
        return timeoutMs;
    }
} 