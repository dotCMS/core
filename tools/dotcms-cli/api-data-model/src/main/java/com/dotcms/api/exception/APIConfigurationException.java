package com.dotcms.api.exception;

public class APIConfigurationException extends RuntimeException {

    /**
     * Simple exception constructor
     * @param message
     */
    public APIConfigurationException(String message) {
        super(message);
    }

    /**
     * Custom message exception constructor
     * @param message
     * @param cause
     */
    public APIConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
