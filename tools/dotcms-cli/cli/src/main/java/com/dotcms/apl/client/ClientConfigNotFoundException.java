package com.dotcms.apl.client;

public class ClientConfigNotFoundException extends RuntimeException {
    public ClientConfigNotFoundException(String configName) {
        super(configName);
    }
}
