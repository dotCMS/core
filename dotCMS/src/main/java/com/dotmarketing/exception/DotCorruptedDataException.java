package com.dotmarketing.exception;

/**
 * Exception to report errores when the data is corrupted on some storage
 * @author jsanca
 */
public class DotCorruptedDataException extends DotRuntimeException {

    public DotCorruptedDataException(String message) {
        super(message);
    }

    public DotCorruptedDataException(Throwable cause) {
        super(cause);
    }

    public DotCorruptedDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
