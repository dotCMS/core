package com.dotcms.health.service;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Intelligent recovery testing with exponential backoff to reduce load during outages.
 * 
 * Instead of fixed-interval polling during failures, this uses exponential backoff:
 * - First retry: 5 seconds
 * - Second retry: 10 seconds  
 * - Third retry: 20 seconds
 * - etc., up to a maximum interval
 * 
 * This dramatically reduces load on failing dependencies while still detecting recovery quickly.
 */
@ApplicationScoped
public class ExponentialBackoffRecoveryTester {
    
    private final Map<String, RecoveryTestSession> activeTests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
        Config.getIntProperty("health.recovery.thread.pool.size", 3)
    );
    
    /**
     * Start recovery testing for a failed health check
     */
    public void startRecoveryTesting(String checkName, Supplier<Boolean> recoveryTest, Consumer<String> onRecovery) {
        if (activeTests.containsKey(checkName)) {
            Logger.debug(this, "Recovery testing already active for: " + checkName);
            return;
        }
        
        RecoveryTestSession session = new RecoveryTestSession(checkName, recoveryTest, onRecovery);
        activeTests.put(checkName, session);
        
        Logger.info(this, String.format(
            "Starting exponential backoff recovery testing for '%s' (initial delay: %s, max delay: %s)",
            checkName, session.currentBackoff, session.maxBackoff
        ));
        
        scheduleNextRecoveryTest(session);
    }
    
    /**
     * Stop recovery testing (called when recovery is detected externally)
     */
    public void stopRecoveryTesting(String checkName) {
        RecoveryTestSession session = activeTests.remove(checkName);
        if (session != null) {
            Logger.info(this, "Stopped recovery testing for: " + checkName);
        }
    }
    
    /**
     * Get active recovery test count (for monitoring)
     */
    public int getActiveTestCount() {
        return activeTests.size();
    }
    
    /**
     * Get recovery test session info (for monitoring)
     */
    public String getRecoveryTestInfo(String checkName) {
        RecoveryTestSession session = activeTests.get(checkName);
        if (session == null) {
            return "No active recovery testing";
        }
        
        Duration elapsed = Duration.between(session.startTime, Instant.now());
        return String.format(
            "Attempts: %d, Elapsed: %s, Next delay: %s", 
            session.attempts, elapsed, session.currentBackoff
        );
    }
    
    private void scheduleNextRecoveryTest(RecoveryTestSession session) {
        Duration nextDelay = session.getNextBackoffDelay();
        
        ScheduledFuture<?> future = executor.schedule(() -> {
            // Check if testing was stopped
            if (!activeTests.containsKey(session.checkName)) {
                return;
            }
            
            try {
                Logger.debug(this, String.format(
                    "Recovery test attempt #%d for '%s'",
                    session.attempts + 1, session.checkName
                ));
                
                boolean recovered = session.recoveryTest.get();
                session.recordAttempt(recovered);
                
                if (recovered) {
                    Duration totalTime = Duration.between(session.startTime, Instant.now());
                    Logger.info(this, String.format(
                        "Recovery detected for '%s' after %d attempts over %s",
                        session.checkName, session.attempts, totalTime
                    ));
                    
                    // Notify recovery and cleanup
                    session.onRecovery.accept(session.checkName);
                    activeTests.remove(session.checkName);
                    
                } else if (session.shouldContinueTesting()) {
                    // Schedule next attempt with exponential backoff
                    scheduleNextRecoveryTest(session);
                    
                } else {
                    Logger.warn(this, String.format(
                        "Recovery testing stopped for '%s' - reached limits (attempts: %d, duration: %s)",
                        session.checkName, session.attempts, 
                        Duration.between(session.startTime, Instant.now())
                    ));
                    activeTests.remove(session.checkName);
                }
                
            } catch (Exception e) {
                Logger.warn(this, "Recovery test failed for: " + session.checkName, e);
                session.recordAttempt(false);
                
                if (session.shouldContinueTesting()) {
                    scheduleNextRecoveryTest(session);
                } else {
                    activeTests.remove(session.checkName);
                }
            }
            
        }, nextDelay.toMillis(), TimeUnit.MILLISECONDS);
        
        // Store the future for potential cancellation
        session.currentTask = future;
    }
    
    @PreDestroy
    public void shutdown() {
        Logger.info(this, "Shutting down exponential backoff recovery tester");
        
        // Cancel all scheduled recovery tests
        Logger.debug(this, String.format("Cancelling %d active recovery tests", activeTests.size()));
        for (RecoveryTestSession session : activeTests.values()) {
            if (session.currentTask != null && !session.currentTask.isDone()) {
                session.currentTask.cancel(true);
                Logger.debug(this, String.format("Cancelled recovery test for: %s", session.checkName));
            }
        }
        activeTests.clear();
        
        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                Logger.warn(this, "Recovery tester executor did not terminate gracefully, forcing shutdown");
                var cancelledTasks = executor.shutdownNow();
                Logger.info(this, String.format("Cancelled %d remaining tasks during forced shutdown", cancelledTasks.size()));
                
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Logger.error(this, "Recovery tester executor did not terminate even after forced shutdown");
                }
            } else {
                Logger.info(this, "Recovery tester executor shutdown gracefully");
            }
        } catch (InterruptedException e) {
            Logger.warn(this, "Recovery tester shutdown interrupted, forcing immediate termination");
            var cancelledTasks = executor.shutdownNow();
            Logger.info(this, String.format("Cancelled %d tasks during interrupt", cancelledTasks.size()));
            Thread.currentThread().interrupt();
        }
        
        Logger.info(this, "Exponential backoff recovery tester shutdown completed");
    }
    
    /**
     * Recovery test session with exponential backoff configuration
     */
    private static class RecoveryTestSession {
        final String checkName;
        final Supplier<Boolean> recoveryTest;
        final Consumer<String> onRecovery;
        final Instant startTime = Instant.now();
        
        int attempts = 0;
        Duration currentBackoff;
        volatile ScheduledFuture<?> currentTask; // Track current scheduled task for cancellation
        
        // Configurable limits (loaded from Config)
        final Duration initialBackoff;
        final Duration maxBackoff;
        final Duration maxTestingDuration;
        final int maxAttempts;
        
        RecoveryTestSession(String checkName, Supplier<Boolean> recoveryTest, Consumer<String> onRecovery) {
            this.checkName = checkName;
            this.recoveryTest = recoveryTest;
            this.onRecovery = onRecovery;
            
            // Load configuration with per-check overrides
            String prefix = "health.recovery." + checkName + ".";
            String globalPrefix = "health.recovery.";
            
            this.initialBackoff = Duration.ofSeconds(
                Config.getIntProperty(prefix + "initial-delay-seconds", 
                Config.getIntProperty(globalPrefix + "initial-delay-seconds", 5))
            );
            
            this.maxBackoff = Duration.ofMinutes(
                Config.getIntProperty(prefix + "max-delay-minutes",
                Config.getIntProperty(globalPrefix + "max-delay-minutes", 5))
            );
            
            this.maxTestingDuration = Duration.ofHours(
                Config.getIntProperty(prefix + "max-duration-hours",
                Config.getIntProperty(globalPrefix + "max-duration-hours", 2))
            );
            
            this.maxAttempts = Config.getIntProperty(prefix + "max-attempts",
                Config.getIntProperty(globalPrefix + "max-attempts", 50)
            );
            
            this.currentBackoff = initialBackoff;
        }
        
        Duration getNextBackoffDelay() {
            return currentBackoff;
        }
        
        void recordAttempt(boolean success) {
            attempts++;
            
            if (!success) {
                // Exponential backoff: double the delay, up to maximum
                long nextDelayMs = Math.min(
                    currentBackoff.toMillis() * 2, 
                    maxBackoff.toMillis()
                );
                currentBackoff = Duration.ofMillis(nextDelayMs);
                
                Logger.debug(this, String.format(
                    "Recovery attempt #%d failed for '%s', next attempt in %s",
                    attempts, checkName, currentBackoff
                ));
            }
        }
        
        boolean shouldContinueTesting() {
            boolean withinAttemptLimit = attempts < maxAttempts;
            boolean withinTimeLimit = Duration.between(startTime, Instant.now()).compareTo(maxTestingDuration) < 0;
            
            return withinAttemptLimit && withinTimeLimit;
        }
    }
} 