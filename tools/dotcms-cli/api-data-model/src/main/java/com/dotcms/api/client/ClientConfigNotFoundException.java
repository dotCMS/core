package com.dotcms.api.client;

public class ClientConfigNotFoundException extends RuntimeException {
    public ClientConfigNotFoundException(String configName) {
        super(configName);
    }
}
