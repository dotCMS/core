package com.dotcms.api.exception;

public class APIConfigurationException extends RuntimeException {

    static final String MESSAGE = "API configuration is not valid for profile: %s";

    public APIConfigurationException(String configName) {
        super(MESSAGE + configName);
    }
}
