package com.dotcms.rest.exception;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Used to produce a 409 Response code
 */
public class ConflictException extends DotRuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(Throwable cause) {
        super(cause);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
