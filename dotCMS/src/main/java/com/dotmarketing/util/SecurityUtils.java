package com.dotmarketing.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility class providing reusable security functions for input validation,
 * logging sanitization, and network security checks.
 * 
 * This class centralizes common security operations that were previously
 * scattered across different utility classes, promoting code reuse and
 * consistent security practices.
 * 
 * @author dotCMS Security Team
 * @since 2025-07-29
 */
public final class SecurityUtils {

    // Private constructor to prevent instantiation
    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * SECURITY: Sanitizes input for safe logging to prevent log injection attacks.
     * 
     * This method removes or escapes dangerous characters that could be used in
     * log injection attacks, including control characters, line breaks, and quotes.
     * 
     * @param input The input to sanitize for logging
     * @return Sanitized input safe for logging, or "null" if input is null/empty
     */
    public static String sanitizeForLogging(final String input) {
        if (UtilMethods.isNotSet(input)) {
            return "null";
        }

        // Remove/escape dangerous characters that could cause log injection
        return input
            .replaceAll("[\r\n\t]", "_")  // Replace line breaks and tabs
            .replaceAll("[\\x00-\\x1F\\x7F]", "_")  // Replace control characters
            .replaceAll("\"", "\\\\\"")  // Escape quotes
            .replaceAll("'", "\\\\'")   // Escape single quotes
            .trim();
    }



    /**
     * SECURITY: Safely extracts client IP from request.
     * 
     * This method relies on Tomcat's RemoteIPValve to handle proxy headers
     * (X-Forwarded-For, X-Real-IP, etc.) and set getRemoteAddr() to the real
     * client IP. If RemoteIPValve is not configured, this will return the
     * direct connection IP.
     * 
     * @param request The HTTP request
     * @return The client IP address, sanitized for logging
     */
    public static String getClientIPFromRequest(final HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // RemoteIPValve should have already handled proxy headers
        return sanitizeForLogging(request.getRemoteAddr());
    }

    /**
     * SECURITY: Gets the current client IP from the thread-local request if available.
     * 
     * Convenience method that uses the thread-local HttpServletRequest to extract
     * the client IP. Returns "unknown" if no request is available.
     * 
     * @return The client IP address or "unknown" if not available
     */
    public static String getCurrentClientIP() {
        try {
            HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            return getClientIPFromRequest(request);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * SECURITY: Safely extracts User-Agent from request for logging.
     * 
     * Gets the User-Agent header and sanitizes it for safe logging.
     * 
     * @param request The HTTP request
     * @return Sanitized User-Agent string or "unknown" if not available
     */
    public static String getUserAgentFromRequest(final HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        return UtilMethods.isSet(userAgent) ? 
            sanitizeForLogging(userAgent) : "unknown";
    }

    /**
     * SECURITY: Gets the current User-Agent from the thread-local request if available.
     * 
     * @return Sanitized User-Agent string or "unknown" if not available
     */
    public static String getCurrentUserAgent() {
        try {
            HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            return getUserAgentFromRequest(request);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * SECURITY: Truncates input to prevent log flooding while preserving useful information.
     * 
     * @param input The input to truncate
     * @param maxLength Maximum allowed length
     * @return Truncated input with indication if truncation occurred
     */
    public static String truncateForLogging(final String input, final int maxLength) {
        if (UtilMethods.isNotSet(input)) {
            return "null";
        }

        if (input.length() <= maxLength) {
            return input;
        }

        return input.substring(0, maxLength) + "...[TRUNCATED]";
    }
}