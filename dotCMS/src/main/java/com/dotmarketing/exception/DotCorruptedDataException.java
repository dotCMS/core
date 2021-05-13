package com.dotmarketing.exception;

/**
 * Exception to report errores when the data is corrupted on some storage
 * @author jsanca
 */
public class DotCorruptedDataException extends DotRuntimeException {

    public DotCorruptedDataException(final String message) {
        super(message);
    }

    public DotCorruptedDataException(final Throwable cause) {
        super(cause);
    }

    public DotCorruptedDataException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
