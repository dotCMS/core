package com.dotcms.publisher.pusher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.publisher.pusher.AuthCredentialPushPublishUtil.PushPublishAuthenticationToken;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests how {@link AuthCredentialPushPublishUtil} classifies the bearer token a sender presents to
 * a receiver, which is what decides the message the sender shows when a push is refused.
 */
public class AuthCredentialPushPublishUtilTest extends IntegrationTestBase {

    private static final String LOCAL_IP = "127.0.0.1";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
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
}
