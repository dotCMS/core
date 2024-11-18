package com.dotcms.analytics;


import com.dotcms.IntegrationTestBase;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenFetchMode;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.exception.UnrecoverableAnalyticsException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Analytics API integration tests.
 *
 * @author vico
 */
public class AnalyticsAPIImplTest extends IntegrationTestBase {

    private static AnalyticsAPI analyticsAPI;
    private Host host;
    private AnalyticsApp analyticsApp;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        analyticsAPI = APILocator.getAnalyticsAPI();
        Config.setProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", true);
        Config.setProperty(AnalyticsAPI.ANALYTICS_USE_DUMMY_TOKEN_KEY, false);
        analyticsAPI = new AnalyticsAPIImpl();
    }

    @AfterClass
    public static void afterClass() {
        Config.setProperty(AnalyticsAPI.ANALYTICS_USE_DUMMY_TOKEN_KEY, true);
        Config.setProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false);
    }

    @Before
    public void before() throws Exception {
        host = new SiteDataGen().nextPersisted();
        analyticsApp = AnalyticsTestUtils.prepareAnalyticsApp(host);
    }

    /**
     * Given a host
     * Then verify an {@link AccessToken} is fetched
     * And it matches the one stored in cache
     */
    @Test
    public void test_getAccessToken_fetchWhenNotCached() throws Exception {
        AccessToken accessToken = analyticsAPI.getAccessToken(analyticsApp, AccessTokenFetchMode.BACKEND_FALLBACK);
        assertNotNull(accessToken);
        assertTrue(AccessTokens.get()
            .getAccessToken(analyticsApp.getAnalyticsProperties().clientId(), null)
            .isPresent());
    }

    /**
     * Given a host
     * Then verify an {@link AccessToken} is not fetched
     */
    @Test
    public void test_getAccessToken() throws Exception {
        AccessTokens.get().removeAccessToken(
            analyticsApp.getAnalyticsProperties().clientId(),
            AnalyticsHelper.get().resolveAudience(analyticsApp));

        analyticsAPI.getCachedAccessToken(analyticsApp);
        AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNull(accessToken);

        analyticsAPI.getAccessToken(analyticsApp, AccessTokenFetchMode.BACKEND_FALLBACK);
        accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNotNull(accessToken);

        final Instant issueDate = accessToken.issueDate();
        analyticsAPI.getAccessToken(analyticsApp, AccessTokenFetchMode.FORCE_RENEW);
        accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNotEquals(issueDate, accessToken.issueDate());
        assertNotNull(accessToken);
    }

    /**
     * Given a host
     * With an API configured with the wrong IPS url
     * Then expect a {@link AnalyticsException} to be thrown
     */
    @Test(expected = AnalyticsException.class)
    public void test_getAccessToken_fail() throws Exception {
        AccessTokens.get().removeAccessToken(analyticsApp.getAnalyticsProperties().clientId(), null);
        Config.initializeConfig();
        new AnalyticsAPIImpl()
            .getAccessToken(analyticsApp, AccessTokenFetchMode.FORCE_RENEW);
    }

    /**
     * Given that no {@link AccessToken} has been defined
     * When call refresh token logic
     * Then verify that it does exist after refreshing
     */
    @Test 
    public void test_refreshAccessToken() throws AnalyticsException {
        AccessTokens.get().removeAccessToken(
            analyticsApp.getAnalyticsProperties().clientId(),
            AnalyticsHelper.get().resolveAudience(analyticsApp));

        AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNull(accessToken);

        analyticsAPI.refreshAccessToken(analyticsApp);
        accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNotNull(accessToken);
    }

    /**
     * Given that no {@link AccessToken} has been defined
     * When call refresh token logic
     * And ot has configured an invalid IDP url
     * Then verify that it an exception is thrown
     */
    @Test(expected = AnalyticsException.class)
    public void test_refreshAccessToken_fail_wrongIdp() throws AnalyticsException {
        AccessTokens.get().removeAccessToken(
            analyticsApp.getAnalyticsProperties().clientId(),
            AnalyticsHelper.get().resolveAudience(analyticsApp));

        AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNull(accessToken);

        new AnalyticsAPIImpl().refreshAccessToken(analyticsApp);
    }

    /**
     * Given that no {@link AccessToken} has been defined
     * When call refresh token logic
     * And ot has configured an invalid IDP url
     * Then verify that it an exception is thrown
     */
    @Test(expected = UnrecoverableAnalyticsException.class)
    public void test_refreshAccessToken_fail_wrong_clientId() throws Exception {
        analyticsApp = AnalyticsTestUtils.prepareAnalyticsApp(host, "some-client-id");

        AccessTokens.get().removeAccessToken(
            analyticsApp.getAnalyticsProperties().clientId(),
            AnalyticsHelper.get().resolveAudience(analyticsApp));

        AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNull(accessToken);

        analyticsAPI.refreshAccessToken(analyticsApp);
    }

    /**
     * Given a cached {@link AccessToken}
     * When resetting access token
     * Then verify that it has been removed from cache
     */
    @Test
    public void test_resetAccessToken() throws Exception {
        AccessToken accessToken = analyticsAPI.getAccessToken(analyticsApp, AccessTokenFetchMode.BACKEND_FALLBACK);
        assertNotNull(accessToken);

        analyticsAPI.resetAccessToken(analyticsApp);
        accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNull(accessToken);
    }

    /**
     * Given a host
     * Then verify an analytics key is fetched
     * And it matches the one stored in the {@link AnalyticsApp} instance
     */
    @Test
    public void test_getAnalyticsKey() throws Exception {
        analyticsAPI.resetAnalyticsKey(analyticsApp, true);
        final String analyticsKey = analyticsAPI.getAnalyticsKey(host);
        assertNotNull(analyticsKey);
        analyticsApp = new AnalyticsApp(host);
        assertEquals(analyticsApp.getAnalyticsProperties().analyticsKey(), analyticsKey);
    }

    /**
     * Given an {@link AnalyticsApp}
     * With no analytics key set at all
     * When {@link AnalyticsAPI#resetAnalyticsKey(AnalyticsApp, boolean)} called to set a new key
     * Then verify new {@link AnalyticsApp} has a not null value
     */
    @Test
    public void test_resetAnalyticsKey() throws Exception {
        analyticsAPI.getAccessToken(analyticsApp, AccessTokenFetchMode.FORCE_RENEW);
        analyticsAPI.getAnalyticsKey(host);
        analyticsAPI.resetAnalyticsKey(analyticsApp, false);

        analyticsApp = AnalyticsHelper.get().appFromHost(host);
        assertNotNull(analyticsApp.getAnalyticsProperties().analyticsKey());
    }

}
