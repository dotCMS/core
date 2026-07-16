package com.dotcms.analytics.content;

import com.dotmarketing.business.DotStateException;

/**
 * Exception thrown when an Analytics App is not properly configured.
 * This exception is typically thrown when attempting to use analytics functionality
 * but the required configuration properties for the Analytics App are missing or invalid.
 * 
 * @author dotCMS
 * @since 24.05
 */
public class AnalyticsAppNotConfiguredException extends DotStateException {

    /**
     * Constructs a new AnalyticsAppNotConfiguredException with the specified detail message.
     *
     * @param message the detail message
     */
    public AnalyticsAppNotConfiguredException(String message) {
        super(message);
    }

    /**
     * Constructs a new AnalyticsAppNotConfiguredException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public AnalyticsAppNotConfiguredException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new AnalyticsAppNotConfiguredException with the specified cause.
     *
     * @param cause the cause
     */
    public AnalyticsAppNotConfiguredException(Throwable cause) {
        super(cause);
    }
}
