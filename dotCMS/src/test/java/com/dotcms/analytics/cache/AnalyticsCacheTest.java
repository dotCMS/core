package com.dotcms.analytics.cache;

import com.dotcms.UnitTestBase;
import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.TokenStatus;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Analytics Cache Test
 *
 * @author vico
 */
public class AnalyticsCacheTest extends UnitTestBase {

    private AnalyticsCache analyticsCache;

    @Before
    public void setup() {
        CacheLocator.init();
        analyticsCache = new AnalyticsCache();
    }

    @Test(expected = DotStateException.class)
    public void test_putAccessToken_noAccessToken() {
        analyticsCache.putAccessToken(null);
    }

    /**
     * Given an {@link AccessToken}
     * When its clientId and audience fields are changed over time
     * Then the access token is cached using a different key depending on the values it has set
     */
    @Test
    public void test_accessToken_lifecycle() {
        AccessToken accessToken = AnalyticsTestUtils.createAccessToken(
            "a1b2c3d4e5f6",
            null,
            null,
            "some-scope",
            "some-token-type",
            TokenStatus.OK);
        analyticsCache.putAccessToken(accessToken);

        final String clientId = "some-client-id";
        final String audience = "some-audience";
        assertTrue(analyticsCache.getAccessToken(null, null).isPresent());
        assertTrue(analyticsCache.getAccessToken(clientId, null).isEmpty());
        assertTrue(analyticsCache.getAccessToken(null, audience).isEmpty());
        assertTrue(analyticsCache.getAccessToken(clientId, audience).isEmpty());

        analyticsCache.removeAccessToken(accessToken);
        accessToken = accessToken.withClientId(clientId);
        analyticsCache.putAccessToken(accessToken);
        assertTrue(analyticsCache.getAccessToken(null, null).isEmpty());
        assertTrue(analyticsCache.getAccessToken(clientId, null).isPresent());
        assertTrue(analyticsCache.getAccessToken(null, audience).isEmpty());
        assertTrue(analyticsCache.getAccessToken(clientId, audience).isEmpty());

        analyticsCache.removeAccessToken(accessToken);
        accessToken = accessToken.withAud(audience).withClientId(null);
        analyticsCache.putAccessToken(accessToken);
        assertTrue(analyticsCache.getAccessToken(null, null).isEmpty());
        assertTrue(analyticsCache.getAccessToken(clientId, null).isEmpty());
        assertTrue(analyticsCache.getAccessToken(null, audience).isPresent());
        assertTrue(analyticsCache.getAccessToken(clientId, audience).isEmpty());

        analyticsCache.removeAccessToken(accessToken);
        accessToken = accessToken.withClientId(clientId);
        analyticsCache.putAccessToken(accessToken);
        assertTrue(analyticsCache.getAccessToken(null, null).isEmpty());
        assertTrue(analyticsCache.getAccessToken(clientId, null).isEmpty());
        assertTrue(analyticsCache.getAccessToken(null, audience).isEmpty());
        assertTrue(analyticsCache.getAccessToken(clientId, audience).isPresent());

        analyticsCache.removeAccessToken(accessToken);
        assertTrue(analyticsCache.getAccessToken(null, null).isEmpty());
        assertTrue(analyticsCache.getAccessToken(clientId, null).isEmpty());
        assertTrue(analyticsCache.getAccessToken(null, audience).isEmpty());
        assertTrue(analyticsCache.getAccessToken(clientId, audience).isEmpty());
    }

}
