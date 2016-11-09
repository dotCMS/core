package com.dotcms.rest;

import java.io.Serializable;

/**
 * Encapsulates an error.
 * Usually the errors are returned to the client transformed on JSON.
 * @author jsanca
 */
public class ErrorEntity implements Serializable {

    /** In case an error code */
    private final String errorCode;

    /** Final message (no an i18n key */
    private final String message;

    /**
     * Constructor
     * @param errorCode
     * @param message
     */
    public ErrorEntity(final String errorCode,
                       final String message) {

        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorEntity{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
} // E:O:F:ErrorEntity.
