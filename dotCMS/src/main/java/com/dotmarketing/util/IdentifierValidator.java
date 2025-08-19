package com.dotmarketing.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import javax.servlet.http.HttpServletRequest;

/**
 * Universal identifier validator for all dotCMS entity types.
 * Provides configurable validation rules that can be applied to any identifier format.
 * 
 * This replaces the need for separate validator classes since most identifiers share
 * the same security concerns and most are standardized UUIDs anyway.
 * 
 * @author dotCMS Security Team
 * @since 24.12.XX
 */
public final class IdentifierValidator {

    /** Default validation profile for standard UUIDs (most dotCMS entities) */
    public static final ValidationProfile UUID_PROFILE = ValidationProfile.builder()
        .maxLength(50)
        .allowOnlyAlphanumericAndDashes(true)
        .context("UUID_IDENTIFIER")
        .build();
        
    /** Validation profile for site identifiers (human-readable, more permissive) */
    public static final ValidationProfile SITE_PROFILE = ValidationProfile.builder()
        .maxLength(255)
        .allowOnlyAlphanumericAndDashes(false)
        .context("SITE_IDENTIFIER")
        .build();
        
    /** Validation profile for new site identifiers (more restrictive) */
    public static final ValidationProfile NEW_SITE_PROFILE = ValidationProfile.builder()
        .maxLength(128)
        .allowOnlyAlphanumericAndDashes(false)
        .restrictAdditionalChars(true)
        .context("NEW_SITE_IDENTIFIER")
        .build();

    // Private constructor to prevent instantiation
    private IdentifierValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates an identifier using the specified validation profile.
     * 
     * @param identifier The identifier to validate
     * @param profile The validation profile to use
     * @return true if the identifier is safe, false if potentially malicious
     */
    public static boolean isValid(final String identifier, final ValidationProfile profile) {
        if (UtilMethods.isNotSet(identifier)) {
            return false;
        }
        
        // Check length first to prevent DoS attacks
        if (identifier.length() > profile.maxLength || identifier.isEmpty()) {
            logSuspiciousIdentifier(identifier, "invalid_length", profile.context);
            return false;
        }
        
        // Apply character validation based on profile
        if (!isValidCharacters(identifier, profile)) {
            return false;
        }
        
        // Check for SQL injection patterns (applies to all identifier types)
        if (containsSQLInjectionPattern(identifier)) {
            logSuspiciousIdentifier(identifier, "sql_injection_pattern", profile.context);
            return false;
        }
        
        return true;
    }

    /**
     * Validates characters based on the validation profile.
     */
    private static boolean isValidCharacters(final String identifier, final ValidationProfile profile) {
        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            
            // Always block the most dangerous characters
            if (c == '\'' || c == '"' || c == ';' || c == '\0' || c < 32) {
                logSuspiciousIdentifier(identifier, "dangerous_character:" + (int)c, profile.context);
                return false;
            }
            
            // Apply profile-specific restrictions
            if (profile.allowOnlyAlphanumericAndDashes) {
                if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') {
                    logSuspiciousIdentifier(identifier, "invalid_character:" + (int)c, profile.context);
                    return false;
                }
            } else if (profile.restrictAdditionalChars) {
                // Additional restrictions for new entities (like new sites)
                if (c == '\\' || c == '<' || c == '>') {
                    logSuspiciousIdentifier(identifier, "restricted_character:" + (int)c, profile.context);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks for SQL injection patterns in the identifier.
     */
    private static boolean containsSQLInjectionPattern(final String identifier) {
        String lower = identifier.toLowerCase().trim();
        
        return lower.equals("drop") || lower.equals("delete") || 
               lower.equals("insert") || lower.equals("update") ||
               lower.equals("union") || lower.equals("select") ||
               lower.startsWith("drop ") || lower.startsWith("delete ") ||
               lower.startsWith("insert ") || lower.startsWith("update ") ||
               lower.startsWith("union ") || lower.startsWith("select ") ||
               lower.contains("'; ") || lower.contains("' ") ||
               lower.equals("--") || lower.equals("/*") ||
               lower.startsWith("-- ") || lower.startsWith("/* ");
    }

    /**
     * Logs suspicious identifier validation failures for threat intelligence.
     */
    private static void logSuspiciousIdentifier(final String identifier, final String reason, final String context) {
        try {
            String clientIP = "unknown";
            HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            if (request != null) {
                clientIP = request.getRemoteAddr();
            }

            String sanitizedInput = identifier != null ? 
                identifier.replaceAll("[\r\n\t]", "_").replaceAll("[\\x00-\\x1F\\x7F]", "_") : "null";
            String truncatedInput = sanitizedInput.length() > 100 ? 
                sanitizedInput.substring(0, 100) + "...[TRUNCATED]" : sanitizedInput;

            String securityMessage = String.format(
                "%s_VALIDATION_FAILED reason='%s' client_ip='%s' input='%s'",
                context, reason, clientIP, truncatedInput
            );

            SecurityLogger.logInfo(IdentifierValidator.class, securityMessage);
        } catch (Exception e) {
            Logger.warn(IdentifierValidator.class, "Failed to log suspicious identifier: " + e.getMessage());
        }
    }

    /**
     * Validation profile that defines the rules for a specific identifier type.
     */
    public static class ValidationProfile {
        private final int maxLength;
        private final boolean allowOnlyAlphanumericAndDashes;
        private final boolean restrictAdditionalChars;
        private final String context;

        private ValidationProfile(int maxLength, boolean allowOnlyAlphanumericAndDashes, 
                                boolean restrictAdditionalChars, String context) {
            this.maxLength = maxLength;
            this.allowOnlyAlphanumericAndDashes = allowOnlyAlphanumericAndDashes;
            this.restrictAdditionalChars = restrictAdditionalChars;
            this.context = context;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int maxLength = 255;
            private boolean allowOnlyAlphanumericAndDashes = false;
            private boolean restrictAdditionalChars = false;
            private String context = "IDENTIFIER";

            public Builder maxLength(int maxLength) {
                this.maxLength = maxLength;
                return this;
            }

            public Builder allowOnlyAlphanumericAndDashes(boolean allowOnly) {
                this.allowOnlyAlphanumericAndDashes = allowOnly;
                return this;
            }

            public Builder restrictAdditionalChars(boolean restrict) {
                this.restrictAdditionalChars = restrict;
                return this;
            }

            public Builder context(String context) {
                this.context = context;
                return this;
            }

            public ValidationProfile build() {
                return new ValidationProfile(maxLength, allowOnlyAlphanumericAndDashes, 
                                           restrictAdditionalChars, context);
            }
        }
    }
}