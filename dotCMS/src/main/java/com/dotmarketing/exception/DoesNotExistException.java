package com.dotmarketing.exception;

/**
 * Exception to report when something should exists and does not exists.
 * @author jsanca
 */
public class DoesNotExistException extends DotRuntimeException {

    public DoesNotExistException(String message) {
        super(message);
    }

    public DoesNotExistException(Throwable throwable) {
        super(throwable);
    }

    public DoesNotExistException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
