package com.dotcms.analytics;

import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.TokenStatus;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class AccessTokensTest {

    @Test
    public void testGet() {
        AccessTokens actualGetResult = AccessTokens.get();
        assertSame(actualGetResult, actualGetResult.get());
    }

    @Test
    public void testPutAccessToken() {
        final AccessToken accessToken = AnalyticsTestUtils.createAccessToken(
            "a1b2c3d4e5f6",
            "some-client-id",
            "some-audience",
            "some-scope",
            "some-token-type",
            TokenStatus.OK,
            Instant.now());
        AccessTokens.get().putAccessToken(accessToken);
        assertSame(accessToken, AccessTokens.get().getAccessToken("some-client-id", "some-audience").get());
    }

    @Test
    public void testRemoveAccessToken() {
        final AccessToken accessToken = AnalyticsTestUtils.createAccessToken(
            "a1b2c3d4e5f6",
            "some-client-id-2",
            "some-audience-2",
            "some-scope",
            "some-token-type",
            TokenStatus.OK,
            Instant.now());
        AccessTokens.get().removeAccessToken(accessToken);
        assertFalse(AccessTokens.get().getAccessToken("some-client-id-2", "some-audience-2").isPresent());
    }

}

