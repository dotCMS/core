package com.dotmarketing.exception;

/**
 * Exception to report when an invalid timezone is detected.
 */
public class InvalidTimeZoneException extends DotRuntimeException {
    public InvalidTimeZoneException(String message) {
        super(message);
    }

    public InvalidTimeZoneException(Throwable cause) {
        super(cause);
    }

    public InvalidTimeZoneException(String message, Throwable cause) {
        super(message, cause);
    }
}