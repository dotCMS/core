package com.dotcms.exception;

/**
 * Analytics exception thrown to represent an unrecoverable error.
 *
 * @author vico
 */
public class UnrecoverableAnalyticsException extends AnalyticsException {

    public UnrecoverableAnalyticsException(String message, int httpCode) {
        super(message, httpCode);
    }

    public UnrecoverableAnalyticsException(String message, Throwable cause, int httpCode) {
        super(message, cause, httpCode);
    }

    public UnrecoverableAnalyticsException(String message) {
        super(message);
    }

    public UnrecoverableAnalyticsException(String message, Throwable cause) {
        super(message, cause);
    }

}
