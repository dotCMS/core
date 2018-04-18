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

    /** field name. if available */
    private final String fieldName;

    /**
     * Constructor
     * @param errorCode
     * @param message
     */
    public ErrorEntity(final String errorCode,
                       final String message) {

        this(errorCode, message,null);
    }

    /**
     * Constructor
     * @param errorCode
     * @param message
     * @param fieldName
     */
    public ErrorEntity(final String errorCode, final String message, final String fieldName) {
        this.errorCode = errorCode;
        this.message = message;
        this.fieldName = fieldName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String toString() {
        return "ErrorEntity{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                ", fieldName='" + fieldName + '\'' +
                '}';
    }
} // E:O:F:ErrorEntity.
