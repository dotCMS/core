package com.dotcms.publisher.pusher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.datagen.EnvironmentDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.AuthCredentialPushPublishUtil.PushPublishAuthenticationToken;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.liferay.portal.model.User;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests how {@link AuthCredentialPushPublishUtil} classifies the bearer token a sender presents to
 * a receiver, which is what decides the message the sender shows when a push is refused.
 */
public class AuthCredentialPushPublishUtilTest extends IntegrationTestBase {

    private static final String LOCAL_IP = "127.0.0.1";

    private static Environment environment;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        environment = new EnvironmentDataGen().nextPersisted();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        APILocator.getEnvironmentAPI().deleteEnvironment(environment.getId());
    }

    /**
     * Builds the request a sender makes against a receiver, carrying the given bearer token.
     */
    private static HttpServletRequest requestWithBearer(final String jwt) {
        return requestWithBearer(jwt, LOCAL_IP);
    }

    /**
     * Builds the request a sender at {@code address} makes against a receiver, carrying the given
     * bearer value.
     */
    private static HttpServletRequest requestWithBearer(final String bearer, final String address) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + bearer);
        when(request.getRemoteAddr()).thenReturn(address);
        when(request.getRemoteHost()).thenReturn(address);
        return request;
    }

    /**
     * Stores an enabled sending endpoint for {@code address} with the given key, the way a record
     * saved before keys were required is stored: straight through the API, with no validation.
     */
    private static PublishingEndPoint sendingEndpoint(final String address, final String key) {
        return new PushPublishingEndPointDataGen()
                .environment(environment)
                .address(address)
                .authKey(key)
                .sending(true)
                .nextPersisted();
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#processAuthHeader(HttpServletRequest)}
     * Given Scenario: The sender presents a token that was valid when the endpoint was configured
     *                 and has since run out.
     * ExpectedResult: The token is reported as expired, not as invalid. The two answers reach the
     *                 sender as different messages — an expired token tells the user to request a
     *                 new one, an invalid one does not — so they must not be conflated.
     */
    @Test
    public void test_processAuthHeader_expiredToken_isReportedAsExpired() throws Exception {

        final User admin = TestUserUtils.getAdminUser();
        final ApiTokenAPI apiTokenAPI = APILocator.getApiTokenAPI();

        final ApiToken shortLived = apiTokenAPI.persistApiToken(ApiToken.builder()
                .withUser(admin)
                .withExpires(Date.from(Instant.now().plusSeconds(5)))
                .withIssueDate(new Date())
                .withRequestingUserId(admin.getUserId())
                .withRequestingIp(LOCAL_IP)
                .withClaims(Map.of("label", "pp-expiry-test"))
                .build(), admin);

        final String jwt = apiTokenAPI.getJWT(shortLived, admin);

        Thread.sleep(6000);

        final PushPublishAuthenticationToken result =
                AuthCredentialPushPublishUtil.INSTANCE.processAuthHeader(requestWithBearer(jwt));

        assertTrue("An expired token must be reported as expired", result.isTokenExpired());
        assertFalse("An expired token must not be reported as invalid", result.isTokenInvalid());
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#processAuthHeader(HttpServletRequest)}
     * Given Scenario: The sender presents a token that has not run out but has been revoked.
     * ExpectedResult: The token is reported as invalid and not as expired, since requesting a new
     *                 token is not what the user needs to hear about a revoked one.
     */
    @Test
    public void test_processAuthHeader_revokedToken_isReportedAsInvalid() throws Exception {

        final User admin = TestUserUtils.getAdminUser();
        final ApiTokenAPI apiTokenAPI = APILocator.getApiTokenAPI();

        final ApiToken token = apiTokenAPI.persistApiToken(ApiToken.builder()
                .withUser(admin)
                .withExpires(Date.from(Instant.now().plusSeconds(3600)))
                .withIssueDate(new Date())
                .withRequestingUserId(admin.getUserId())
                .withRequestingIp(LOCAL_IP)
                .withClaims(Map.of("label", "pp-revoked-test"))
                .build(), admin);

        final String jwt = apiTokenAPI.getJWT(token, admin);
        assertTrue(apiTokenAPI.revokeToken(token, admin));

        final PushPublishAuthenticationToken result =
                AuthCredentialPushPublishUtil.INSTANCE.processAuthHeader(requestWithBearer(jwt));

        assertTrue("A revoked token must be reported as invalid", result.isTokenInvalid());
        assertFalse("A revoked token must not be reported as expired", result.isTokenExpired());
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#processAuthHeader(HttpServletRequest)}
     * Given Scenario: The sender presents something that is not a token this receiver issued.
     * ExpectedResult: The token is reported as invalid and not as expired.
     */
    @Test
    public void test_processAuthHeader_unknownToken_isReportedAsInvalid() {

        final PushPublishAuthenticationToken result = AuthCredentialPushPublishUtil.INSTANCE
                .processAuthHeader(requestWithBearer("not-a-token"));

        assertTrue("An unknown token must be reported as invalid", result.isTokenInvalid());
        assertFalse("An unknown token must not be reported as expired", result.isTokenExpired());
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#processAuthHeader(HttpServletRequest)}
     * Given Scenario: A sending endpoint is stored with an empty key, and a request from its
     *                 address presents the digest of an empty key.
     * ExpectedResult: The request is refused. An endpoint without a key never authenticates.
     */
    @Test
    public void test_processAuthHeader_endpointWithEmptyKey_isRefused() throws Exception {
        assertKeyIsRefused("192.0.2.101", "");
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#processAuthHeader(HttpServletRequest)}
     * Given Scenario: A sending endpoint is stored with a key made only of spaces, and a request
     *                 from its address presents the digest of that key.
     * ExpectedResult: The request is refused, as for an empty key.
     */
    @Test
    public void test_processAuthHeader_endpointWithBlankKey_isRefused() throws Exception {
        assertKeyIsRefused("192.0.2.102", "   ");
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#processAuthHeader(HttpServletRequest)}
     * Given Scenario: A sending endpoint is stored with a real key, and a request from its address
     *                 presents the digest of that key.
     * ExpectedResult: The request is authenticated as that endpoint.
     */
    @Test
    public void test_processAuthHeader_endpointWithKey_isAccepted() throws Exception {
        final String address = "192.0.2.103";
        final PublishingEndPoint endpoint = sendingEndpoint(address, "pp-receiver-test-key");
        try {
            final PushPublishAuthenticationToken result = AuthCredentialPushPublishUtil.INSTANCE
                    .processAuthHeader(requestWithBearer(
                            PublicEncryptionFactory.digestString("pp-receiver-test-key"), address));

            assertFalse("The endpoint's own key must be accepted", result.isTokenInvalid());
            assertEquals(endpoint.getId(), result.getPublishingEndPoint().getId());
        } finally {
            APILocator.getPublisherEndPointAPI().deleteEndPointById(endpoint.getId());
        }
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#getRequestToken(PublishingEndPoint)}
     * Given Scenario: The sender's record of a receiver is stored with an empty key.
     * ExpectedResult: No token is produced, so nothing is sent for that receiver.
     */
    @Test
    public void test_getRequestToken_endpointWithEmptyKey_isEmpty() throws Exception {
        final PublishingEndPoint endpoint = sendingEndpoint("192.0.2.104", "");
        try {
            assertTrue("An endpoint without a key must not produce a token",
                    AuthCredentialPushPublishUtil.INSTANCE.getRequestToken(endpoint).isEmpty());
        } finally {
            APILocator.getPublisherEndPointAPI().deleteEndPointById(endpoint.getId());
        }
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#getRequestToken(PublishingEndPoint)}
     * Given Scenario: The sender's record of a receiver is stored with the literal key "null".
     * ExpectedResult: A token is produced. Only an empty or blank key counts as none, so a key
     *                 that happens to read "null" keeps working.
     */
    @Test
    public void test_getRequestToken_endpointWithNullTextKey_isPresent() throws Exception {
        final PublishingEndPoint endpoint = sendingEndpoint("192.0.2.105", "null");
        try {
            assertEquals(java.util.Optional.of("Bearer " + PublicEncryptionFactory.digestString("null")),
                    AuthCredentialPushPublishUtil.INSTANCE.getRequestToken(endpoint));
        } finally {
            APILocator.getPublisherEndPointAPI().deleteEndPointById(endpoint.getId());
        }
    }

    /**
     * Stores a sending endpoint for {@code address} with {@code key}, presents the digest of that
     * key from that address, and checks the request is refused.
     */
    private static void assertKeyIsRefused(final String address, final String key) throws Exception {
        final PublishingEndPoint endpoint = sendingEndpoint(address, key);
        try {
            final PushPublishAuthenticationToken result = AuthCredentialPushPublishUtil.INSTANCE
                    .processAuthHeader(requestWithBearer(PublicEncryptionFactory.digestString(key), address));

            assertTrue("An endpoint without a key must not authenticate anyone", result.isTokenInvalid());
        } finally {
            APILocator.getPublisherEndPointAPI().deleteEndPointById(endpoint.getId());
        }
    }
}
