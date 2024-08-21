package com.dotcms.jobs.business.error;

/**
 * Implements the Circuit Breaker pattern to prevent repeated failures in a system. It helps to
 * avoid cascading failures by temporarily disabling operations that are likely to fail.
 */
public class CircuitBreaker {

    private final int failureThreshold;
    private final long resetTimeout;
    private volatile int failureCount;
    private volatile long lastFailureTime;
    private volatile boolean isOpen;

    /**
     * Constructs a new CircuitBreaker.
     *
     * @param failureThreshold the number of failures that will cause the circuit to open
     * @param resetTimeout     the time in milliseconds after which to attempt to close the circuit
     */
    public CircuitBreaker(int failureThreshold, long resetTimeout) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("Failure threshold must be greater than zero");
        }
        if (resetTimeout <= 0) {
            throw new IllegalArgumentException("Reset timeout must be greater than zero");
        }
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
    }

    /**
     * Determines whether a request should be allowed to proceed.
     *
     * @return true if the request is allowed, false if the circuit is open
     */
    public synchronized boolean allowRequest() {
        if (isOpen) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFailureTime > resetTimeout) {
                isOpen = false;
                failureCount = 0;
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Records a failure, potentially causing the circuit to open if the failure threshold is
     * reached.
     */
    public synchronized void recordFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
        if (failureCount >= failureThreshold) {
            isOpen = true;
        }
    }

    /**
     * Manually resets the circuit breaker to a closed state.
     */
    public synchronized void reset() {
        isOpen = false;
        failureCount = 0;
        lastFailureTime = 0;
    }

    /**
     * @return the current state of the circuit breaker
     */
    public synchronized boolean isOpen() {
        return isOpen;
    }

    /**
     * @return the current failure count
     */
    public synchronized int getFailureCount() {
        return failureCount;
    }

    /**
     * @return the time of the last recorded failure, or 0 if no failures have been recorded
     */
    public synchronized long getLastFailureTime() {
        return lastFailureTime;
    }

}