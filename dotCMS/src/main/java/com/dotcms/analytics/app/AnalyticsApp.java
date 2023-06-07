package com.dotcms.analytics.app;

import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AbstractAnalyticsProperties;
import com.dotcms.analytics.model.AnalyticsKey;
import com.dotcms.analytics.model.AnalyticsProperties;
import com.dotcms.analytics.model.AnalyticsAppProperty;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Holds configuration for Analytics App from secrets.
 *
 * @author vico
 */
public class AnalyticsApp {

    public static final String ANALYTICS_APP_KEY = "dotAnalytics-config";
    // test
    public static final String ANALYTICS_APP_CONFIG_URL_KEY = "analytics.app.config.url";
    public static final String ANALYTICS_APP_WRITE_URL_KEY = "analytics.app.write.url";
    public static final String ANALYTICS_APP_READ_URL_KEY = "analytics.app.read.url";
    public static final String ANALYTICS_APP_OVERRIDE_NOT_ALLOWED_KEY = "analytics.app.override.not.allowed";

    private final Host host;
    private final AnalyticsProperties analyticsProperties;

    public AnalyticsApp(final Host host) {
        this.host = Objects.requireNonNullElse(host, APILocator.systemHost());
        analyticsProperties = resolveProperties(getSecrets());
    }

    /**
     * @return {@link AnalyticsProperties} holding information about analytics configuration based on App secrets
     */
    public AbstractAnalyticsProperties getAnalyticsProperties() {
        return analyticsProperties;
    }

    /**
     * Evaluates if minimum required properties are set.
     *
     * @return if required properties are set.
     */
    public boolean isConfigValid() {
        return Stream
            .of(analyticsProperties.clientId(), analyticsProperties.clientSecret())
            .allMatch(StringUtils::isNotBlank);
    }

    /**
     * @return encoded representation of clientId:clientSecret
     */
    public String clientIdAndSecret() {
        return AnalyticsHelper.get().encodeClientIdAndSecret(
            getAnalyticsProperties().clientId(),
            getAnalyticsProperties().clientSecret());
    }

    /**
     * Stores the analytics key to App's secrets
     *
     * @param analyticsKey analytics key
     */
    public void saveAnalyticsKey(final AnalyticsKey analyticsKey) throws DotDataException, DotSecurityException {
        final AppSecrets appSecrets = getSecrets();
        if (appSecrets.getSecrets().isEmpty()) {
            Logger.warn(this, "Resolved secrets is empty, not storing the ANALYTICS_KEY");
            return;
        }

        APILocator.getAppsAPI().saveSecret(
            ANALYTICS_APP_KEY,
            new Tuple2<>(
                AnalyticsAppProperty.ANALYTICS_KEY.getPropertyName(),
                Secret.newSecret(analyticsKey.jsKey().toCharArray(), Type.STRING, false)),
            host,
            APILocator.systemUser());
    }

    /**
     * Resolve analytics configuration properties from the provided {@link AppSecrets}.
     *
     * @param appSecrets application secrets associated with analytics  app
     * @return {@link AnalyticsProperties} instance holding the analytics configuration properties
     */
    private AnalyticsProperties resolveProperties(final AppSecrets appSecrets) {
        final AnalyticsProperties.Builder propertiesBuilder = AnalyticsProperties.builder();

        appSecrets.getSecrets()
            .forEach((key, secret) ->
                AnalyticsAppProperty.findProperty(key)
                    .ifPresentOrElse(
                        property -> property
                            .setter(secret.getString())
                            .accept(propertiesBuilder),
                        () -> Logger.warn(this, String.format("Analytics app property %s cannot be found", key))));

        return propertiesBuilder.build();
    }

    /**
     * Based on defined application key get the {@link AppSecrets} instance associated with it.
     *
     * @return app secrets
     */
    private AppSecrets getSecrets() {
        final AppsAPI appsAPI = APILocator.getAppsAPI();
        return Try.of(
            () -> appsAPI.getSecrets(ANALYTICS_APP_KEY, true, host, APILocator.systemUser()))
                .getOrElseGet(e -> Optional.empty())
            .orElse(AppSecrets.empty());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyticsApp that = (AnalyticsApp) o;
        return analyticsProperties.clientId().equals(that.analyticsProperties.clientId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(analyticsProperties.clientId());
    }
}
