package com.dotcms.api.exception;

public class ClientConfigNotFoundException extends RuntimeException {
    public ClientConfigNotFoundException(String configName) {
        super(configName);
    }
}
