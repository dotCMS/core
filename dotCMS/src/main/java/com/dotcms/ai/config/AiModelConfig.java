package com.dotcms.ai.config;

import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates the Configuration for a model
 * @author jsanca
 */
public class AiModelConfig implements Serializable {

    public static final String API_KEY = "key";
    public static final String VENDOR  = "vendor";
    public static final String MODEL   = "model";
    public static final String API_URL = "apiUrl";
    public static final String TEMPERATURE = "temperature";
    public static final String MAX_OUTPUT_TOKENS = "maxOutputTokens";
    public static final String TIMEOUT_MS = "timeoutMs";

    private final String name;
    private final Map<String, String> config;

    public AiModelConfig(final String name, final Map<String, String> config) {
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

    @Override
    public String toString() {
        return "AiModelConfig{" +
                "name='" + name + '\'' +
                ", config=" + config +
                '}';
    }

    /**
     * Creates a Builder initialized from an existing AiModelConfig and overrides temperature.
     * Returned builder allows additional entries before calling build().
     */
    public static Builder withTemperature(final AiModelConfig base, final Float temperature) {
        final Builder builder = builderFrom(base);
        if (temperature != null) {
            builder.temperature(temperature);
        }
        return builder;
    }

    /**
     * Convenience to create a Builder pre-populated from an existing AiModelConfig.
     */
    public static Builder builderFrom(final AiModelConfig base) {
        final Builder builder = new Builder();
        builder.name(base.name);
        builder.entries(base.config);
        return builder;
    }

    /**
     * Generic builder for AiModelConfig. Keeps existing constructors intact.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private java.util.Map<String, String> config = new java.util.HashMap<>();

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Adds a key/value entry to the configuration map.
         */
        public Builder entry(final String key, final String value) {
            if (key != null && value != null) {
                this.config.put(key, value);
            }
            return this;
        }

        public Builder entries(final java.util.Map<String, String> entries) {
            if (entries != null) {
                this.config.putAll(entries);
            }
            return this;
        }

        public Builder temperature(final Float temperature) {
            if (temperature != null) {
                this.config.put(TEMPERATURE, String.valueOf(temperature));
            }
            return this;
        }

        public Builder temperature(final String temperature) {
            if (temperature != null) {
                this.config.put(TEMPERATURE, temperature);
            }
            return this;
        }

        public AiModelConfig build() {
            return new AiModelConfig(this.name, java.util.Map.copyOf(this.config));
        }
    }
}
