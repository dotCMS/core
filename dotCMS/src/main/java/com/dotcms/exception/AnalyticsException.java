package com.dotcms.exception;

/**
 * Analytics most generic exception thrown when fetching access tokens and analytics key.
 *
 * @author vico
 */
public class AnalyticsException extends Exception {

    private final int httpCode;

    public AnalyticsException(final String message, final int httpCode) {
        super(message);
        this.httpCode = httpCode;
    }

    public AnalyticsException(final String message, final Throwable cause, final int httpCode) {
        super(message, cause);
        this.httpCode = httpCode;
    }

    public AnalyticsException(final String message) {
        this(message, 0);
    }

    public AnalyticsException(final String message, final Throwable cause) {
        this(message, cause, 0);
    }

    public int getHttpCode() {
        return httpCode;
    }

}
