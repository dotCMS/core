package com.dotcms.analytics;


import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenStatus;
import com.dotcms.analytics.model.AnalyticsAppProperty;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;

import static com.dotcms.analytics.app.AnalyticsApp.ANALYTICS_APP_CONFIG_URL_KEY;
import static com.dotcms.analytics.app.AnalyticsApp.ANALYTICS_APP_READ_URL_KEY;
import static com.dotcms.analytics.app.AnalyticsApp.ANALYTICS_APP_WRITE_URL_KEY;

/**
 * Analytics test utils.
 *
 * @author vico
 */
public class AnalyticsTestUtils {

    public static final String CLIENT_ID = "analytics-customer-customer1";
    public static final String CLIENT_SECRET = "testsecret";

    public static AnalyticsApp prepareAnalyticsApp(final Host host, final String clientId) throws Exception {
        final AppSecrets appSecrets = new AppSecrets.Builder()
            .withKey(AnalyticsApp.ANALYTICS_APP_KEY)
            .withSecret(AnalyticsAppProperty.CLIENT_ID.getPropertyName(), clientId)
            .withHiddenSecret(AnalyticsAppProperty.CLIENT_SECRET.getPropertyName(), CLIENT_SECRET)
            .withSecret(
                AnalyticsAppProperty.ANALYTICS_CONFIG_URL.getPropertyName(),
                Config.getStringProperty(
                    ANALYTICS_APP_CONFIG_URL_KEY,
                    "http://localhost:8080/c/customer1/cluster1/keys"))
            .withSecret(
                AnalyticsAppProperty.ANALYTICS_WRITE_URL.getPropertyName(),
                Config.getStringProperty(ANALYTICS_APP_WRITE_URL_KEY, "http://localhost"))
            .withSecret(
                AnalyticsAppProperty.ANALYTICS_READ_URL.getPropertyName(),
                Config.getStringProperty(ANALYTICS_APP_READ_URL_KEY, "http://localhost"))
            .build();
        APILocator.getAppsAPI().saveSecrets(appSecrets, host, APILocator.systemUser());
        return AnalyticsHelper.get().appFromHost(host);
    }

    public static AnalyticsApp prepareAnalyticsApp(final Host host) throws Exception {
        return prepareAnalyticsApp(host, CLIENT_ID);
    }

    public static AccessToken createAccessToken(final String token,
                                                final String clientId,
                                                final String audience,
                                                final String scope,
                                                final String tokenType,
                                                final TokenStatus tokenStatus) {
        return AccessToken.builder()
            .accessToken(token)
            .clientId(clientId)
            .aud(audience)
            .scope(scope)
            .tokenType(tokenType)
            .expiresIn(3600)
            .status(AccessTokenStatus.builder().tokenStatus(tokenStatus).build())
            .build();
    }

}
