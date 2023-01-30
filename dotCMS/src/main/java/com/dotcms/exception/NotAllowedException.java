package com.dotcms.exception;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Thrown when a not allowed operation is attempted
 */
public class NotAllowedException extends DotRuntimeException {

    public NotAllowedException(String message) {
        super(message);
    }

    public NotAllowedException(Throwable cause) {
        super(cause);
    }

    public NotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
