package com.dotmarketing.util;

import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotmarketing.business.APILocator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized security threat logging utility with rate limiting and comprehensive
 * threat intelligence collection.
 * 
 * This class provides a unified approach to logging security threats across the
 * application, preventing log flooding while capturing essential threat intelligence
 * for monitoring and analysis.
 * 
 * Features:
 * - Rate limiting per IP/threat type to prevent log flooding
 * - Comprehensive threat context collection (IP, User-Agent, etc.)
 * - Secure logging that prevents information disclosure
 * - Thread-safe operations for high-concurrency environments
 * 
 * @author dotCMS Security Team
 * @since 2025-07-29
 */
public final class SecurityThreatLogger {

    // Configurable rate limiting settings
    private static final long RATE_LIMIT_WINDOW_MS = Config.getLongProperty("security.threat.logger.rate.limit.window.ms", 60000L); // Default: 1 minute
    private static final int MAX_LOGGED_THREATS_PER_MINUTE = Config.getIntProperty("security.threat.logger.max.threats.per.minute", 10); // Default: 10 per minute
    private static final int MAX_INPUT_LENGTH_FOR_LOGGING = Config.getIntProperty("security.threat.logger.max.input.length", 200); // Default: 200 chars
    
    // Rate limiting encoding constants
    private static final int MAX_SUPPORTED_THREAT_COUNT = Integer.MAX_VALUE; // ~2.1 billion threats per minute
    private static final long MINUTE_MASK = 0xFFFFFFFF00000000L; // High 32 bits for minute
    private static final long COUNT_MASK = 0x00000000FFFFFFFFL;  // Low 32 bits for count

    // Thread-safe rate limiting storage
    private static final ConcurrentHashMap<String, AtomicLong> THREAT_LOG_COUNTERS = new ConcurrentHashMap<>();

    // Security logger service
    private static final SecurityLoggerServiceAPI securityLoggerServiceAPI = APILocator.getSecurityLogger();

    // Private constructor to prevent instantiation
    private SecurityThreatLogger() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // Static initialization block to validate configuration
    static {
        validateConfiguration();
    }
    
    /**
     * Validates the security threat logger configuration to ensure it's within supported limits.
     * 
     * @throws IllegalStateException if configuration is invalid
     */
    private static void validateConfiguration() {
        if (MAX_LOGGED_THREATS_PER_MINUTE <= 0) {
            throw new IllegalStateException("security.threat.logger.max.threats.per.minute must be greater than 0");
        }
        
        if (MAX_LOGGED_THREATS_PER_MINUTE > MAX_SUPPORTED_THREAT_COUNT) {
            throw new IllegalStateException(
                "security.threat.logger.max.threats.per.minute (" + MAX_LOGGED_THREATS_PER_MINUTE + 
                ") exceeds maximum supported value (" + MAX_SUPPORTED_THREAT_COUNT + ")");
        }
        
        if (RATE_LIMIT_WINDOW_MS <= 0) {
            throw new IllegalStateException("security.threat.logger.rate.limit.window.ms must be greater than 0");
        }
        
        Logger.info(SecurityThreatLogger.class, 
            "SecurityThreatLogger initialized with rate limit: " + MAX_LOGGED_THREATS_PER_MINUTE + 
            " threats per " + (RATE_LIMIT_WINDOW_MS / 1000) + " seconds");
    }

    /**
     * SECURITY: Logs SQL injection attempts with comprehensive threat intelligence.
     * 
     * @param suspiciousInput The potentially malicious input that was detected
     * @param detectedThreat The specific threat pattern that was detected (e.g., evil SQL word)
     * @param sourceContext Context about where the threat was detected (e.g., "SQLUtil.sanitizeSQL")
     */
    public static void logSQLInjectionAttempt(final String suspiciousInput, final String detectedThreat, final String sourceContext) {
        logSecurityThreat("SQL_INJECTION_ATTEMPT", suspiciousInput, detectedThreat, sourceContext);
    }

    /**
     * SECURITY: Logs XSS attempts with comprehensive threat intelligence.
     * 
     * @param suspiciousInput The potentially malicious input that was detected
     * @param detectedThreat The specific threat pattern that was detected
     * @param sourceContext Context about where the threat was detected
     */
    public static void logXSSAttempt(final String suspiciousInput, final String detectedThreat, final String sourceContext) {
        logSecurityThreat("XSS_ATTEMPT", suspiciousInput, detectedThreat, sourceContext);
    }

    /**
     * SECURITY: Logs path traversal attempts with comprehensive threat intelligence.
     * 
     * @param suspiciousInput The potentially malicious input that was detected
     * @param detectedThreat The specific threat pattern that was detected
     * @param sourceContext Context about where the threat was detected
     */
    public static void logPathTraversalAttempt(final String suspiciousInput, final String detectedThreat, final String sourceContext) {
        logSecurityThreat("PATH_TRAVERSAL_ATTEMPT", suspiciousInput, detectedThreat, sourceContext);
    }

    /**
     * SECURITY: Logs general security threats with comprehensive threat intelligence.
     * 
     * @param threatType The type of threat detected (e.g., "SQL_INJECTION_ATTEMPT")
     * @param suspiciousInput The potentially malicious input that was detected
     * @param detectedThreat The specific threat pattern that was detected
     * @param sourceContext Context about where the threat was detected
     */
    public static void logSecurityThreat(final String threatType, final String suspiciousInput, 
                                       final String detectedThreat, final String sourceContext) {
        try {
            // Get client information for threat tracking
            String clientIP = SecurityUtils.getCurrentClientIP();
            String userAgent = SecurityUtils.getCurrentUserAgent();

            // Check rate limiting to prevent log flooding
            if (!shouldLogThreat(clientIP, detectedThreat)) {
                return; // Rate limit exceeded, skip logging
            }

            // Build comprehensive security message
            String securityMessage = buildThreatMessage(threatType, suspiciousInput, detectedThreat, 
                                                      sourceContext, clientIP, userAgent);

            // Log to security infrastructure
            SecurityLogger.logInfo(SecurityThreatLogger.class, securityMessage);
            securityLoggerServiceAPI.logInfo(SecurityThreatLogger.class, 
                threatType + " blocked - see security logs for details");

        } catch (Exception e) {
            // SECURITY: Never let security logging break the application
            Logger.warn(SecurityThreatLogger.class, "Failed to log security threat: " + e.getMessage());
        }
    }

    /**
     * SECURITY: Logs authentication-related security events.
     * 
     * @param eventType The type of auth event (e.g., "FAILED_LOGIN", "BRUTE_FORCE_ATTEMPT")
     * @param username The username involved (will be sanitized)
     * @param additionalContext Additional context about the event
     */
    public static void logAuthenticationThreat(final String eventType, final String username, final String additionalContext) {
        try {
            String clientIP = SecurityUtils.getCurrentClientIP();
            String userAgent = SecurityUtils.getCurrentUserAgent();

            // Build auth-specific message
            String securityMessage = String.format(
                "%s username='%s' client_ip='%s' user_agent='%s' context='%s'",
                eventType,
                SecurityUtils.sanitizeForLogging(username),
                clientIP,
                userAgent,
                SecurityUtils.sanitizeForLogging(additionalContext)
            );

            SecurityLogger.logInfo(SecurityThreatLogger.class, securityMessage);
            securityLoggerServiceAPI.logInfo(SecurityThreatLogger.class, eventType + " detected");

        } catch (Exception e) {
            Logger.warn(SecurityThreatLogger.class, "Failed to log authentication threat: " + e.getMessage());
        }
    }

    /**
     * SECURITY: Rate limiting check to prevent log flooding attacks.
     * Uses thread-safe atomic operations to track logging frequency per IP/threat combination.
     * 
     * @param clientIP The client IP address
     * @param detectedThreat The threat that was detected
     * @return true if the threat should be logged, false if rate limited
     */
    private static boolean shouldLogThreat(final String clientIP, final String detectedThreat) {
        String rateLimitKey = clientIP + ":" + detectedThreat;
        AtomicLong counter = THREAT_LOG_COUNTERS.computeIfAbsent(rateLimitKey, k -> new AtomicLong(0));
        
        long currentMinute = System.currentTimeMillis() / RATE_LIMIT_WINDOW_MS;
        
        // Thread-safe counter update with compare-and-set loop
        long currentValue, newValue;
        do {
            currentValue = counter.get();
            long storedMinute = (currentValue & MINUTE_MASK) >> 32; // Extract minute from high 32 bits
            long threatCount = currentValue & COUNT_MASK; // Extract count from low 32 bits
            
            if (currentMinute > storedMinute) {
                // Reset counter for new minute
                newValue = (currentMinute << 32) | 1L;
            } else {
                // Check rate limit before incrementing
                if (threatCount >= MAX_LOGGED_THREATS_PER_MINUTE) {
                    return false; // Rate limit exceeded
                }
                newValue = currentValue + 1;
            }
        } while (!counter.compareAndSet(currentValue, newValue));

        return true; // Threat should be logged
    }

    /**
     * SECURITY: Builds comprehensive threat message with all relevant context.
     * 
     * @param threatType Type of threat detected
     * @param suspiciousInput The malicious input
     * @param detectedThreat The specific threat pattern
     * @param sourceContext Where the threat was detected
     * @param clientIP Client IP address
     * @param userAgent Client user agent
     * @return Formatted security message
     */
    private static String buildThreatMessage(final String threatType, final String suspiciousInput,
                                           final String detectedThreat, final String sourceContext,
                                           final String clientIP, final String userAgent) {
        // Sanitize and truncate input for logging
        String sanitizedInput = SecurityUtils.sanitizeForLogging(suspiciousInput);
        String truncatedInput = SecurityUtils.truncateForLogging(sanitizedInput, MAX_INPUT_LENGTH_FOR_LOGGING);

        return String.format(
            "%s detected_threat='%s' source='%s' client_ip='%s' user_agent='%s' input_length=%d input_preview='%s'",
            threatType,
            SecurityUtils.sanitizeForLogging(detectedThreat),
            SecurityUtils.sanitizeForLogging(sourceContext),
            clientIP,
            userAgent,
            suspiciousInput != null ? suspiciousInput.length() : 0,
            truncatedInput
        );
    }

    /**
     * SECURITY: Clears old rate limiting entries to prevent memory leaks.
     * This method should be called periodically by a maintenance task.
     * 
     * @param maxAgeMinutes Maximum age in minutes for rate limiting entries
     */
    public static void cleanupOldRateLimitEntries(final int maxAgeMinutes) {
        try {
            long cutoffTime = (System.currentTimeMillis() / RATE_LIMIT_WINDOW_MS) - maxAgeMinutes;
            
            THREAT_LOG_COUNTERS.entrySet().removeIf(entry -> {
                long storedMinute = (entry.getValue().get() & MINUTE_MASK) >> 32; // Extract minute from high 32 bits
                return storedMinute < cutoffTime;
            });
            
            Logger.debug(SecurityThreatLogger.class, 
                "Cleaned up old rate limiting entries, remaining: " + THREAT_LOG_COUNTERS.size());
                
        } catch (Exception e) {
            Logger.warn(SecurityThreatLogger.class, "Failed to cleanup rate limit entries: " + e.getMessage());
        }
    }

    /**
     * Gets the current number of rate limiting entries for monitoring purposes.
     * 
     * @return Number of active rate limiting entries
     */
    public static int getRateLimitEntryCount() {
        return THREAT_LOG_COUNTERS.size();
    }
    
    /**
     * Gets the current rate limiting configuration for monitoring purposes.
     * 
     * @return String representation of current rate limiting configuration
     */
    public static String getRateLimitConfiguration() {
        return String.format("Max threats per minute: %d, Window: %d ms, Supported max: %d", 
                           MAX_LOGGED_THREATS_PER_MINUTE, RATE_LIMIT_WINDOW_MS, MAX_SUPPORTED_THREAT_COUNT);
    }
}