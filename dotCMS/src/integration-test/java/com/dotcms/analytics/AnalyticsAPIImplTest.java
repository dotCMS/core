package com.dotcms.analytics;


import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.cache.AnalyticsCache;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AnalyticsAppProperty;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.dotcms.analytics.app.AnalyticsApp.ANALYTICS_APP_CONFIG_URL_KEY;
import static com.dotcms.analytics.app.AnalyticsApp.ANALYTICS_APP_READ_URL_KEY;
import static com.dotcms.analytics.app.AnalyticsApp.ANALYTICS_APP_WRITE_URL_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Analytics API integration tests.
 *
 * @author vico
 */
public class AnalyticsAPIImplTest {

    private static final String CLIENT_ID = "analytics-customer-customer1";
    private static final String CLIENT_SECRET = "testsecret";

    private static AnalyticsAPI analyticsAPI;
    private static AppsAPI appAPI;
    private static Host host;
    private static AnalyticsApp analyticsApp;
    private static AnalyticsCache analyticsCache;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        analyticsAPI = APILocator.getAnalyticsAPI();
        appAPI = APILocator.getAppsAPI();
        host = APILocator.systemHost();
        prepareAnalyticsApp();
    }

    /**
     * Given a host
     * Then verify an {@link AccessToken} is fetched
     * And it matches the one stored in cache
     */
    @Test
    public void test_fetchAccessToken() throws DotDataException {
        final AccessToken accessToken = analyticsAPI.fetchAccessToken(host);
        assertNotNull(accessToken);
        assertTrue(analyticsCache.getAccessToken(analyticsApp.getAnalyticsProperties().clientId(), null).isPresent());
    }

    /**
     * Given a host
     * With an API configured with the wrong IPS url
     * Then expect a {@link DotDataException} to be thrown
     */
    @Test(expected = DotDataException.class)
    public void test_fetchAccessToken_fail() throws DotDataException {
        analyticsCache.removeAccessToken(analyticsApp.getAnalyticsProperties().clientId(), null);
        new AnalyticsAPIImpl("http://some-host:9999", analyticsCache).fetchAccessToken(host);
    }

    /**
     * Given a host
     * Then verify an analytics key is fetched
     * And it matches the one stored in the {@link AnalyticsApp} instance
     */
    @Test
    public void test_fetchAnalyticsKey() throws DotDataException {
        final String analyticsKey = analyticsAPI.fetchAnalyticsKey(host);
        assertNotNull(analyticsKey);
        analyticsApp = new AnalyticsApp(host);
        assertEquals(analyticsApp.getAnalyticsProperties().analyticsKey(), analyticsKey);
    }

    /**
     * Given an {@link AnalyticsApp}
     * With no analytics key set at all
     * When {@link AnalyticsAPI#resetAnalyticsKey(AnalyticsApp)} called to set a new key
     * Then verify new {@link AnalyticsApp} has a not null value
     */
    @Test
    public void test_resetAnalyticsKey() throws DotDataException {
        analyticsAPI.resetAnalyticsKey(analyticsApp);
        analyticsApp = AnalyticsHelper.getHostApp(host);
        assertNotNull(analyticsApp.getAnalyticsProperties().analyticsKey());
    }

    private static void prepareAnalyticsApp() throws DotDataException, DotSecurityException {
        final AppSecrets appSecrets = new AppSecrets.Builder()
            .withKey(AnalyticsApp.ANALYTICS_APP_KEY)
            .withSecret(AnalyticsAppProperty.CLIENT_ID.getPropertyName(), CLIENT_ID)
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
        appAPI.saveSecrets(appSecrets, host, APILocator.systemUser());
        analyticsApp = AnalyticsHelper.getHostApp(host);
        analyticsCache = CacheLocator.getAnalyticsCache();
    }

}
