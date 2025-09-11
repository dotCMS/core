package com.dotcms.ai.v2.api.provider.config;

import java.util.Map;

/**
 * Encapsulates the Configuration for a model
 * @author jsanca
 */
public class ModelConfig {

    public static final String API_KEY = "key";
    public static final String VENDOR  = "vendor";
    public static final String MODEL   = "model";
    public static final String API_URL = "apiUrl";

    private final String name;
    private final Map<String, String> config;

    public ModelConfig(final String name, final Map<String, String> config) {
        this.name   = name;
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public String get(final String key) {
        return config.get(key);
    }

    public String getOrDefault(final String key, final String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public Map<String, String> asMap() {
        return Map.copyOf(config);
    }
}
