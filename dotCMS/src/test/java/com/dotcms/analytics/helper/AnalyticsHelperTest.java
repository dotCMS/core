package com.dotcms.analytics.helper;

import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AnalyticsKey;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.time.Instant;
import java.util.Base64;

import static com.dotcms.analytics.AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AnalyticsHelper tests.
 *
 * @author vico
 */
public class AnalyticsHelperTest {

    private Response response;
    private Response.StatusType statusType;

    @Before
    public void setup() {
        response = mock(Response.class);
        statusType = mock(Response.StatusType.class);
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
    }

    /**
     * Given a {@link Response}
     * Then evaluate it does have a SUCCESS http status
     */
    @Test
    public void test_isSuccessResponse() {
        assertTrue(AnalyticsHelper.isSuccessResponse(response));
    }

    /**
     * Given a {@link Response}
     * Then evaluate it does not have a SUCCESS http status
     */
    @Test
    public void test_isSuccessResponse_not() {
        when(statusType.getFamily()).thenReturn(Response.Status.Family.CLIENT_ERROR);
        assertFalse(AnalyticsHelper.isSuccessResponse(response));
        when(statusType.getFamily()).thenReturn(Response.Status.Family.SERVER_ERROR);
        assertFalse(AnalyticsHelper.isSuccessResponse(response));
    }

    /**
     * Given an {@link Response}
     * Then verify that an {@link AccessToken} can be extracted as an entity
     */
    @Test
    public void test_extractToken() {
        final AccessToken accessToken = createAccessToken();
        when(response.readEntity(AccessToken.class)).thenReturn(accessToken);
        AnalyticsHelper.extractToken(response)
            .ifPresentOrElse(token -> assertEquals(accessToken, token), Assert::fail);
    }

    /**
     * Given an {@link Response}
     * Then verify that an {@link AccessToken} cannot be extracted as an entity
     */
    @Test
    public void test_extractToken_empty() {
        when(statusType.getFamily()).thenReturn(Response.Status.Family.CLIENT_ERROR);
        AnalyticsHelper.extractToken(response).ifPresent(token -> fail());
    }

    /**
     * Given an {@link Response}
     * Then verify that an {@link AnalyticsKey} can be extracted as an entity
     */
    @Test
    public void test_extractAnalyticsKey() {
        final AnalyticsKey analyticsKey = createAnalyticsKey();
        when(response.readEntity(AnalyticsKey.class)).thenReturn(analyticsKey);
        AnalyticsHelper.extractAnalyticsKey(response)
            .ifPresentOrElse(key -> assertEquals(analyticsKey, key), Assert::fail);
    }

    /**
     * Given an {@link Response}
     * Then verify that an {@link AnalyticsKey} cannot be extracted as an entity
     */
    @Test
    public void test_extractAnalyticsKey_empty() {
        when(statusType.getFamily()).thenReturn(Response.Status.Family.CLIENT_ERROR);
        AnalyticsHelper.extractAnalyticsKey(response).ifPresent(key -> fail());
    }

    /**
     * Given an {@link AccessToken}
     * When it does have a issue data set
     * Then verify it is expired
     */
    @Test
    public void test_isExpired_noIssueDate() {
        final AccessToken accessToken = createAccessToken();
        assertTrue(AnalyticsHelper.isExpired(accessToken));
    }

    /**
     * Given an {@link AccessToken}
     * When the issue date is set with different values
     * Then verify it is evaluated as expired according to the instant it's representing
     */
    @Test
    public void test_isExpired() {
        AccessToken accessToken = createAccessToken().withIssueDate(Instant.now());
        assertFalse(AnalyticsHelper.isExpired(accessToken));

        accessToken = accessToken.withIssueDate(Instant.now().minusSeconds(ANALYTICS_ACCESS_TOKEN_TTL));
        assertTrue(AnalyticsHelper.isExpired(accessToken));

        accessToken = accessToken.withIssueDate(Instant.now().minusSeconds(ANALYTICS_ACCESS_TOKEN_TTL));
        assertFalse(AnalyticsHelper.isExpired(accessToken, 2));
    }

    /**
     * Given client id and client secret strings
     * Then verify the concatenation is the same as the decoded value of client id and client secret Base64 encoding
     */
    @Test
    public void test_encodeClientIdAndSecret() {
        final String clientId = "some-client-id";
        final String clientSecret = "some-client-secret";
        final String toEncode = String.format("%s:%s", clientId, clientSecret);
        assertEquals(
            toEncode,
            new String(Base64.getDecoder().decode(AnalyticsHelper.encodeClientIdAndSecret(clientId, clientSecret))));
    }

    @NotNull
    private static AnalyticsKey createAnalyticsKey() {
        return AnalyticsKey.builder().jsKey("some-js-key").m2mKey("some-m2m-key").build();
    }

    private AccessToken createAccessToken() {
        return AnalyticsTestUtils.createAccessToken(
            "a1b2c3d4e5f6",
            "some-client-i",
            "some-audience",
            "some-scope",
            "some-token-type")
            .withExpiresIn(ANALYTICS_ACCESS_TOKEN_TTL);
    }

}
