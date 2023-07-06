package com.dotmarketing.microprofile.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RuntimeContextConfig {

    // Create a singleton instance of the RuntimeContextConfig
    private static final RuntimeContextConfig INSTANCE = new RuntimeContextConfig();

    private static final String RUNTIME_CONTEXT_PATH = "runtime.context.path";

    // Create a private constructor to prevent other classes from instantiating
    private RuntimeContextConfig() {
    }

    // Provide a global point of access to the singleton
    public static RuntimeContextConfig getInstance() {
        return INSTANCE;
    }


    private static final Map<String,String> configuration = new ConcurrentHashMap<>();

    public String put(final String propertyName, String value) {
        return configuration.put(propertyName, value);
    }





}
