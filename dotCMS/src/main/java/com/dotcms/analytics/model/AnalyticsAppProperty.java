package com.dotcms.analytics.model;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotmarketing.util.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;


/**
 * Analytics app's properties holder.
 *
 * @author vico
 */
public enum AnalyticsAppProperty {

    CLIENT_ID("clientId") {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(final String value) {
            return builder -> builder.clientId(value);
        }
    },
    CLIENT_SECRET("clientSecret") {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.clientSecret(value);
        }
    },
    ANALYTICS_KEY("analyticsKey") {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.analyticsKey(AnalyticsKey.builder().jsKey(value).build());
        }
    },
    ANALYTICS_CONFIG_URL("analyticsConfigUrl") {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.analyticsConfigUrl(StringUtils.defaultIfBlank(
                value,
                Config.getStringProperty(AnalyticsApp.ANALYTICS_APP_CONFIG_URL_KEY, null)));
        }
    },
    ANALYTICS_WRITE_URL("analyticsWriteUrl") {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.analyticsWriteUrl(StringUtils.defaultIfBlank(
                value,
                Config.getStringProperty(AnalyticsApp.ANALYTICS_APP_WRITE_URL_KEY, null)));
        }
    },
    ANALYTICS_READ_URL("analyticsReadUrl") {
        @Override
        public Consumer<AnalyticsProperties.Builder> setter(String value) {
            return builder -> builder.analyticsReadUrl(StringUtils.defaultIfBlank(
                value,
                Config.getStringProperty(AnalyticsApp.ANALYTICS_APP_READ_URL_KEY, null)));
        }
    };

    private final String propertyName;

    AnalyticsAppProperty(final String propertyName) {
        this.propertyName = propertyName;
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

}
