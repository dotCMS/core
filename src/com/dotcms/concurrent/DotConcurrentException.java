package com.dotcms.concurrent;

/**
 * Dot Concurrent exception
 * @author jsanca
 */
public class DotConcurrentException extends RuntimeException {

    public DotConcurrentException() {
    }

    public DotConcurrentException(String message) {
        super(message);
    }

    public DotConcurrentException(String message, Throwable cause) {
        super(message, cause);
    }

    public DotConcurrentException(Throwable cause) {
        super(cause);
    }

    public DotConcurrentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
} // E:O:F:DotConcurrentException.
