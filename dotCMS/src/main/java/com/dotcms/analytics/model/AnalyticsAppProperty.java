package com.dotcms.analytics.model;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotmarketing.util.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Analytics app's properties holder.
 *
 * @author vico
 */
public enum AnalyticsAppProperty {

    CLIENT_ID("clientId", AnalyticsApp.ANALYTICS_APP_CLIENT_ID_KEY) {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(final String value) {
            return builder -> builder.clientId(resolveEnvVarValue(this, value));
        }
    },
    CLIENT_SECRET("clientSecret", AnalyticsApp.ANALYTICS_APP_CLIENT_SECRET_KEY) {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.clientSecret(resolveEnvVarValue(this, value));
        }
    },
    ANALYTICS_KEY("analyticsKey") {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.analyticsKey(value);
        }
    },
    ANALYTICS_CONFIG_URL("analyticsConfigUrl", AnalyticsApp.ANALYTICS_APP_CONFIG_URL_KEY) {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.analyticsConfigUrl(resolveEnvVarValue(this, value));
        }
    },
    ANALYTICS_WRITE_URL("analyticsWriteUrl", AnalyticsApp.ANALYTICS_APP_WRITE_URL_KEY) {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.analyticsWriteUrl(resolveEnvVarValue(this, value));
        }
    },
    ANALYTICS_READ_URL("analyticsReadUrl", AnalyticsApp.ANALYTICS_APP_READ_URL_KEY) {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.analyticsReadUrl(resolveEnvVarValue(this, value));
        }
    };

    public static final List<AnalyticsAppProperty> ENV_VAR_PROPERTIES = Arrays
        .stream(values())
        .filter(property -> StringUtils.isNotBlank(property.envVarName))
        .collect(Collectors.toList());

    private final String propertyName;
    private final String envVarName;

    AnalyticsAppProperty(final String propertyName, final String envVarName) {
        this.propertyName = propertyName;
        this.envVarName = envVarName;
    }

    AnalyticsAppProperty(final String propertyName) {
        this(propertyName, null);
    }

    /**
     * Gets the actual {@link Consumer<AnalyticsProperties.Builder>} in charge to set a value to the properties.
     *
     * @param value string value to set
     * @return the actual consumer to be used to set value
     */
    public abstract Consumer<AnalyticsProperties.Builder> setter(String value);

    /**
     * @return the property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @return the env-var name associated
     */
    public String getEnvVarName() {
        return envVarName;
    }

    /**
     * Finds a property based on a provided property name among these enum's defined values.
     *
     * @param propertyName property name
     * @return resolved {@link AnalyticsAppProperty} instance
     */
    public static Optional<AnalyticsAppProperty> findProperty(final String propertyName) {
        return Arrays.stream(values())
            .filter(property -> property.getPropertyName().equals(propertyName))
            .findFirst();
    }

    /**
     * Resolves the value that will be set at the property when building it.
     *
     * @param property property in matter
     * @param value fallback value when the there is no env-var property detected
     * @return the resolved value
     */
    private static String resolveEnvVarValue(final AnalyticsAppProperty property, final String value) {
        return StringUtils.defaultIfBlank(Config.getStringProperty(property.getEnvVarName(), null), value);
    }
}
