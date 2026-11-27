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
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
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
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests how {@link AuthCredentialPushPublishUtil} classifies the bearer token a sender presents to
 * a receiver, which is what decides the message the sender shows when a push is refused.
 */
public class AuthCredentialPushPublishUtilTest extends IntegrationTestBase {

    private static final String LOCAL_IP = "127.0.0.1";

    private static final String SENDER_ADDRESS = "10.61.72.83";
    private static final String OTHER_ADDRESS = "10.61.72.84";
    private static final String SENDER_SUBNET = "10.62.0.0/24";
    private static final String ADDRESS_IN_SENDER_SUBNET = "10.62.0.9";
    private static final String IPV6_ADDRESS = "2001:db8:0:0:0:0:0:5";
    private static final String SENDER_KEY = "sender-key-6172";

    private Environment environment;
    private PushPublishingEndPoint endpoint;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Removes the sending endpoint and environment a test registered, so later tests of the suite
     * do not see them when they look a sender up by address.
     */
    @After
    public void cleanUp() throws Exception {
        try {
            // deleteEnvironment only removes receiving endpoints, so the sending one goes first
            if (endpoint != null) {
                APILocator.getPublisherEndPointAPI().deleteEndPointById(endpoint.getId());
                endpoint = null;
            }
        } finally {
            if (environment != null) {
                APILocator.getEnvironmentAPI().deleteEnvironment(environment.getId());
                environment = null;
            }
        }
    }

    /**
     * Registers an enabled sending endpoint at the given address (a single address or a subnet in
     * CIDR form) with {@link #SENDER_KEY} as its key.
     */
    private void registerSender(final String address) {
        environment = new EnvironmentDataGen().nextPersisted();
        endpoint = new PushPublishingEndPointDataGen()
                .environment(environment)
                .address(address)
                .authKey(SENDER_KEY)
                .sending(true)
                .nextPersisted();
    }

    /**
     * Builds a request that presents the digest of {@link #SENDER_KEY}, as a sender without an API
     * token does, over a connection whose address and reported host differ.
     *
     * @param remoteAddr the address of the connection
     * @param remoteHost the host the container reports for the connection
     */
    private static HttpServletRequest requestWithSenderKey(final String remoteAddr,
            final String remoteHost) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + PublicEncryptionFactory.digestString(SENDER_KEY));
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(request.getRemoteHost()).thenReturn(remoteHost);
        return request;
    }

    /**
     * Builds the request a sender makes against a receiver, carrying the given bearer token.
     */
    private static HttpServletRequest requestWithBearer(final String jwt) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(request.getRemoteAddr()).thenReturn(LOCAL_IP);
        when(request.getRemoteHost()).thenReturn(LOCAL_IP);
        return request;
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
     * Given Scenario: A sending endpoint is registered at one address. A request presents that
     *                 endpoint's key over a connection from a different address, while the host
     *                 reported for the connection is the endpoint's address.
     * ExpectedResult: The key is refused as invalid. A sender is identified by the address of the
     *                 connection, not by the host reported for it.
     */
    @Test
    public void test_processAuthHeader_senderKey_fromAnotherAddress_isReportedAsInvalid() {

        registerSender(SENDER_ADDRESS);

        final PushPublishAuthenticationToken result = AuthCredentialPushPublishUtil.INSTANCE
                .processAuthHeader(requestWithSenderKey(OTHER_ADDRESS, SENDER_ADDRESS));

        assertTrue("A key presented from another address must be reported as invalid",
                result.isTokenInvalid());
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#processAuthHeader(HttpServletRequest)}
     * Given Scenario: A sending endpoint is registered at one address. A request presents that
     *                 endpoint's key over a connection from that address, while the host reported
     *                 for the connection is a name that does not resolve to it.
     * ExpectedResult: The key is accepted and identifies the registered endpoint.
     */
    @Test
    public void test_processAuthHeader_senderKey_fromSenderAddress_isAccepted() {

        registerSender(SENDER_ADDRESS);

        final PushPublishAuthenticationToken result = AuthCredentialPushPublishUtil.INSTANCE
                .processAuthHeader(requestWithSenderKey(SENDER_ADDRESS, "localhost"));

        assertFalse("The sender's key from its own address must be accepted",
                result.isTokenInvalid());
        assertFalse(result.isTokenExpired());
        assertEquals(endpoint.getId(), result.getKey());
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#processAuthHeader(HttpServletRequest)}
     * Given Scenario: A sending endpoint is registered as a subnet in CIDR form. A request presents
     *                 that endpoint's key over a connection from an address inside the subnet,
     *                 while the host reported for the connection is a name.
     * ExpectedResult: The key is accepted and identifies the registered endpoint.
     */
    @Test
    public void test_processAuthHeader_senderKey_fromAddressInSenderSubnet_isAccepted() {

        registerSender(SENDER_SUBNET);

        final PushPublishAuthenticationToken result = AuthCredentialPushPublishUtil.INSTANCE
                .processAuthHeader(requestWithSenderKey(ADDRESS_IN_SENDER_SUBNET, "localhost"));

        assertFalse("The sender's key from an address in its subnet must be accepted",
                result.isTokenInvalid());
        assertFalse(result.isTokenExpired());
        assertEquals(endpoint.getId(), result.getKey());
    }

    /**
     * Method to test: {@link AuthCredentialPushPublishUtil#processAuthHeader(HttpServletRequest)}
     * When: a sending endpoint is registered as an IPv4 subnet in CIDR form, and a request with
     * that endpoint's key comes from an IPv6 address, which the subnet cannot contain
     * Should: report the key as invalid instead of failing the request with an exception
     */
    @Test
    public void test_processAuthHeader_senderKey_fromIpv6Address_withSubnetSender_isReportedAsInvalid() {

        registerSender(SENDER_SUBNET);

        final PushPublishAuthenticationToken result = AuthCredentialPushPublishUtil.INSTANCE
                .processAuthHeader(requestWithSenderKey(IPV6_ADDRESS, IPV6_ADDRESS));

        assertTrue("An address outside the sender's subnet must not be accepted",
                result.isTokenInvalid());
    }
}
