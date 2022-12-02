package com.dotcms.analytics;


import com.dotcms.IntegrationTestBase;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.cache.AnalyticsCache;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Analytics API integration tests.
 *
 * @author vico
 */
public class AnalyticsAPIImplTest extends IntegrationTestBase {

    private static AnalyticsAPI analyticsAPI;
    private static AnalyticsCache analyticsCache;
    private static Host host;
    private static AnalyticsApp analyticsApp;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        analyticsAPI = APILocator.getAnalyticsAPI();
        analyticsCache = CacheLocator.getAnalyticsCache();
        Config.setProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", true);
    }

    @Before
    public void before() throws DotDataException, DotSecurityException {
        host = new SiteDataGen().nextPersisted(false);
        analyticsApp = AnalyticsTestUtils.prepareAnalyticsApp(host);
    }

    /**
     * Given a host
     * Then verify an {@link AccessToken} is fetched
     * And it matches the one stored in cache
     */
    @Test
    public void test_getAccessToken_fetchWhenNotCached() throws AnalyticsException {
        AccessToken accessToken = Try
            .of(() -> analyticsAPI.getAccessToken(analyticsApp, true))
            .getOrElse(analyticsAPI.getAccessToken(analyticsApp, true));
        assertNotNull(accessToken);
        assertTrue(analyticsCache.getAccessToken(analyticsApp.getAnalyticsProperties().clientId(), null).isPresent());
    }

    /**
     * Given a host
     * Then verify an {@link AccessToken} is not fetched
     */
    @Test
    public void test_getAccessToken() throws AnalyticsException {
        analyticsAPI.getAccessToken(analyticsApp, true);
        final AccessToken accessToken = analyticsAPI.getAccessToken(analyticsApp);
        assertNotNull(accessToken);
    }

    /**
     * Given a host
     * With an API configured with the wrong IPS url
     * Then expect a {@link AnalyticsException} to be thrown
     */
    @Test(expected = AnalyticsException.class)
    public void test_getAccessToken_fail() throws AnalyticsException {
        analyticsCache.removeAccessToken(analyticsApp.getAnalyticsProperties().clientId(), null);
        new AnalyticsAPIImpl("http://some-host:9999", analyticsCache).getAccessToken(analyticsApp, true);
    }

    /**
     * Given a host
     * Then verify an analytics key is fetched
     * And it matches the one stored in the {@link AnalyticsApp} instance
     */
    @Test
    public void test_getAnalyticsKey() throws AnalyticsException {
        analyticsAPI.getAccessToken(analyticsApp, true);
        final String analyticsKey = analyticsAPI.getAnalyticsKey(host);
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
    public void test_resetAnalyticsKey() throws AnalyticsException {
        analyticsAPI.resetAnalyticsKey(analyticsApp);
        analyticsApp = AnalyticsHelper.appFromHost(host);
        assertNotNull(analyticsApp.getAnalyticsProperties().analyticsKey());
    }

}
