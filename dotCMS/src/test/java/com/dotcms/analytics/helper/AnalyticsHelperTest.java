package com.dotcms.analytics.helper;

import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenErrorType;
import com.dotcms.analytics.model.AccessTokenStatus;
import com.dotcms.analytics.model.AnalyticsKey;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.exception.UnrecoverableAnalyticsException;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.WebResource;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.Base64;

import static com.dotcms.analytics.AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL;
import static com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor.BEARER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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

    private CircuitBreakerUrl.Response response;

    @Before
    public void setup() {
        response = mock(CircuitBreakerUrl.Response.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    }

    /**
     * Given an int status code
     * Then evaluate it does have a SUCCESS http status
     */
    @Test
    public void test_isSuccesStatusCode() {
        assertTrue(AnalyticsHelper.isSuccessResponse(HttpStatus.SC_ACCEPTED));
        assertTrue(AnalyticsHelper.isSuccessResponse(HttpStatus.SC_OK));
        assertFalse(AnalyticsHelper.isSuccessResponse(HttpStatus.SC_BAD_REQUEST));
        assertFalse(AnalyticsHelper.isSuccessResponse(HttpStatus.SC_FORBIDDEN));
        assertFalse(AnalyticsHelper.isSuccessResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }

    /**
     * Given a {@link Response}
     * Then evaluate it does have a SUCCESS http status
     */
    @Test
    public void test_isSuccessResponse() {
        assertTrue(AnalyticsHelper.isSuccessResponse(response));
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_ACCEPTED);
        assertTrue(AnalyticsHelper.isSuccessResponse(response));
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        assertFalse(AnalyticsHelper.isSuccessResponse(response));
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
        assertFalse(AnalyticsHelper.isSuccessResponse(response));
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertFalse(AnalyticsHelper.isSuccessResponse(response));
    }

    /**
     * Given an {@link Response}
     * Then verify that an {@link AccessToken} can be extracted as an entity
     */
    @Test
    public void test_extractToken() {
        final AccessToken accessToken = createAccessToken();
        when(response.getResponse()).thenReturn(accessToken);
        AnalyticsHelper
            .extractToken(response)
            .ifPresentOrElse(token -> assertEquals(accessToken, token), Assert::fail);
    }

    /**
     * Given an {@link Response}
     * Then verify that an {@link AccessToken} cannot be extracted as an entity
     */
    @Test
    public void test_extractToken_empty() {
        when(response.getResponse()).thenReturn(null);
        AnalyticsHelper.extractToken(response).ifPresent(token -> fail("Token should not be present"));
    }

    /**
     * Given an {@link Response}
     * Then verify that an {@link AnalyticsKey} can be extracted as an entity
     */
    @Test
    public void test_extractAnalyticsKey() {
        final AnalyticsKey analyticsKey = createAnalyticsKey();
        when(response.getResponse()).thenReturn(analyticsKey);
        AnalyticsHelper.extractAnalyticsKey(response)
            .ifPresentOrElse(key -> assertEquals(analyticsKey, key), Assert::fail);
    }

    /**
     * Given an {@link Response}
     * Then verify that an {@link AnalyticsKey} cannot be extracted as an entity
     */
    @Test
    public void test_extractAnalyticsKey_empty() {
        when(response.getResponse()).thenReturn(null);
        AnalyticsHelper.extractAnalyticsKey(response).ifPresent(key -> fail());
    }

    /**
     * Given an {@link AccessToken}
     * When it does have issue date set
     * Then verify it is expired
     */
    @Test
    public void test_isExpired_noIssueDate() {
        final AccessToken accessToken = createAccessToken();
        assertTrue(AnalyticsHelper.hasTokenExpired(accessToken));
    }

    /**
     * Given an {@link AccessToken}
     * When the issue date is set with different values
     * Then verify it is evaluated as expired according to the instant it's representing
     */
    @Test
    public void test_isTokenExpired() {
        AccessToken accessToken = createFreshAccessToken();
        assertFalse(AnalyticsHelper.hasTokenExpired(accessToken));

        accessToken = accessToken.withIssueDate(Instant.now().minusSeconds(ANALYTICS_ACCESS_TOKEN_TTL));
        assertTrue(AnalyticsHelper.hasTokenExpired(accessToken));
    }

    /**
     * Given an {@link AccessToken}
     * When it does have issue date set
     * Then evaluate if issue date is in window
     */
    @Test
    public void test_iisTokenInWindow() {
        AccessToken accessToken = createFreshAccessToken();
        assertFalse(AnalyticsHelper.isTokenInWindow(accessToken));

        accessToken = accessToken.withIssueDate(Instant.now().minusSeconds(3600 - 60));
        assertTrue(AnalyticsHelper.isTokenInWindow(accessToken));

        accessToken = accessToken.withIssueDate(Instant.now().minusSeconds(3600 - 61));
        assertFalse(AnalyticsHelper.isTokenInWindow(accessToken));

        accessToken = accessToken.withIssueDate(Instant.now().minusSeconds(3600 - 1));
        assertTrue(AnalyticsHelper.isTokenInWindow(accessToken));
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

    /**
     * Given several instances of {@link AccessToken} with different status
     * Then evaluate that when resolving the status it returns the very status
     */
    @Test
    public void test_resolveTokenStatus() {
        assertSame(TokenStatus.NONE, AnalyticsHelper.resolveTokenStatus(null));

        AccessToken accessToken = createAccessToken().withStatus(null);
        assertSame(TokenStatus.NONE, AnalyticsHelper.resolveTokenStatus(accessToken));

         accessToken = createFreshAccessToken();
        assertSame(TokenStatus.OK, AnalyticsHelper.resolveTokenStatus(accessToken));

        accessToken = createAccessToken(TokenStatus.NOOP);
        assertSame(TokenStatus.NOOP, AnalyticsHelper.resolveTokenStatus(accessToken));

        accessToken = createAccessToken(TokenStatus.BLOCKED);
        assertSame(TokenStatus.BLOCKED, AnalyticsHelper.resolveTokenStatus(accessToken));

        accessToken = createAccessToken()
            .withIssueDate(Instant.now().minusSeconds(ANALYTICS_ACCESS_TOKEN_TTL));
        assertSame(TokenStatus.EXPIRED, AnalyticsHelper.resolveTokenStatus(accessToken));
    }

    /**
     * Given a NOOP {@link AccessToken} instance
     * When token is checked
     * Then verify it should throw {@link AnalyticsException}
     */
    @Test(expected = AnalyticsException.class)
    public void test_checkNoopAccessToken() throws AnalyticsException {
        checkStatusThrowing(TokenStatus.NOOP);
    }

    /**
     * Given a BLOCKED {@link AccessToken} instance
     * When token is checked
     * Then verify it should throw {@link AnalyticsException}
     */
    @Test(expected = AnalyticsException.class)
    public void test_checkBlockedAccessToken() throws AnalyticsException {
        checkStatusThrowing(TokenStatus.BLOCKED);
    }

    /**
     * Given a EXPIRED {@link AccessToken} instance
     * When token is checked
     * Then verify it should throw {@link AnalyticsException}
     */
    @Test(expected = AnalyticsException.class)
    public void test_checkExpiredAccessToken() throws AnalyticsException {
        checkStatusThrowing(createAccessToken().withIssueDate(Instant.now().minusSeconds(ANALYTICS_ACCESS_TOKEN_TTL)));
    }

    /**
     * Given a OK {@link AccessToken} instance
     * When token is checked
     * Then verify it should NOT throw {@link AnalyticsException}
     */
    @Test
    public void test_checkAccessToken() {
        try {
            checkStatusThrowing(createFreshAccessToken());
        } catch (AnalyticsException e) {
            fail("OK token status should not throw any exception");
        }
    }

    /**
     * Given an {@link AccessToken} instance
     * Then evaluate token cannot be formatted since exception is thrown due to token validation
     */
    @Test(expected = AnalyticsException.class)
    public void test_formatBearer_fail() throws AnalyticsException {
        final AccessToken accessToken = createAccessToken(TokenStatus.NOOP);
        AnalyticsHelper.formatBearer(accessToken);
    }

    /**
     * Given an {@link AccessToken} instance
     * Then evaluate token can be formatted with Bearer type
     */
    @Test
    public void test_formatBearer() throws AnalyticsException {
        final AccessToken accessToken = createFreshAccessToken();
        assertEquals(BEARER + accessToken.accessToken(), AnalyticsHelper.formatBearer(accessToken));
    }

    /**
     * Given an {@link AnalyticsKey} instance
     * Then evaluate key can be formatted with Basic type
     */
    @Test
    public void test_formatBasic() throws AnalyticsException {
        final String key = "this-is-a-key";
        final AnalyticsKey analyticsKey = AnalyticsKey.builder().jsKey(key).build();
        assertEquals(WebResource.BASIC + key, AnalyticsHelper.formatBasic(analyticsKey));
    }

    /**
     * Given a successful response
     * When throw from response
     * Then an exception should not be thrown
     */
    @Test
    public void test_throwFromResponse_success() {
        try {
            AnalyticsHelper.throwFromResponse(response, null);
        } catch (AnalyticsException e) {
            fail("When response is success exception should NOT be thrown");
        }
    }

    /**
     * Given a not successful response
     * When throw from response
     * Then a {@link UnrecoverableAnalyticsException} should be thrown
     */
    @Test(expected = UnrecoverableAnalyticsException.class)
    public void test_throwFromResponse_badRequest() throws AnalyticsException {
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        AnalyticsHelper.throwFromResponse(response, null);
    }

    /**
     * Given a not successful response
     * When throw from response
     * Then a {@link UnrecoverableAnalyticsException} should be thrown
     */
    @Test(expected = UnrecoverableAnalyticsException.class)
    public void test_throwFromResponse_unauthorized() throws AnalyticsException {
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
        AnalyticsHelper.throwFromResponse(response, null);
    }

    /**
     * Given a not successful response
     * When throw from response
     * Then a {@link AnalyticsException} should be thrown
     */
    @Test(expected = AnalyticsException.class)
    public void test_throwFromResponse_notFound() throws AnalyticsException {
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        AnalyticsHelper.throwFromResponse(response, null);
    }

    /**
     * Given an {@link AccessToken} instance
     * When creating NOOP token
     * Verify that in fact it has a {@link TokenStatus#NOOP} status
     */
    @Test
    public void test_createNoopToken() {
        final String message = "some-message";
        final AccessToken accessToken = AnalyticsHelper.createNoopToken(null, message);
        assertSame(TokenStatus.NOOP, accessToken.status().tokenStatus());
        assertSame(AccessTokenErrorType.PERMANENT_ERROR, accessToken.status().errorType());
        assertEquals(message, accessToken.status().reason());
    }

    /**
     * Given an {@link AccessToken} instance
     * When creating BLOCKED token
     * Verify that in fact it has a {@link TokenStatus#BLOCKED} status
     */
    @Test
    public void test_createBlockedToken() {
        final String message = "some-message";
        final AccessToken accessToken = AnalyticsHelper.createBlockedToken(null, message);
        assertSame(TokenStatus.BLOCKED, accessToken.status().tokenStatus());
        assertSame(AccessTokenErrorType.TEMPORARY_ERROR, accessToken.status().errorType());
        assertEquals(message, accessToken.status().reason());
    }

    /**
     * Given an {@link AccessToken} instance
     * Then verify it has a {@link TokenStatus#NOOP}
     */
    @Test
    public void test_isTokenOk() {
        final AccessToken accessToken = createAccessToken();
        assertTrue(AnalyticsHelper.isTokenOk(accessToken));
    }

    /**
     * Given an {@link AccessToken} instance
     * Then verify it has a {@link TokenStatus#NOOP}
     */
    @Test
    public void test_isTokenNoop() {
        final AccessToken accessToken = createAccessToken(TokenStatus.NOOP);
        assertTrue(AnalyticsHelper.isTokenNoop(accessToken));
    }

    /**
     * Given an {@link AccessToken} instance
     * Then verify it has a {@link TokenStatus#BLOCKED}
     */
    @Test
    public void test_isTokenBlocked() {
        final AccessToken accessToken = createAccessToken(TokenStatus.BLOCKED);
        assertTrue(AnalyticsHelper.isTokenBlocked(accessToken));
    }

    /**
     * Given any {@link com.dotcms.analytics.app.AnalyticsApp}
     * Then verify the audience is null
     * TODO: this needs to updated once how audience is resolved is clear
     */
    @Test
    public void test_resolveAudience() {
        assertNull(AnalyticsHelper.resolveAudience(null));
    }

    /**
     * Given an {@link AccessToken} instance
     * Then verify status based string resolution
     */
    @Test
    public void test_resolveStatusMessage() {
        AccessToken accessToken = createAccessToken().withStatus(null);
        String message = "ACCESS_TOKEN for clientId " + accessToken.clientId();
        assertEquals(message, AnalyticsHelper.resolveStatusMessage(accessToken));

        accessToken = createAccessToken(TokenStatus.NOOP);
        message += " is currently " + accessToken.status().tokenStatus();
        assertEquals(message, AnalyticsHelper.resolveStatusMessage(accessToken));

        AccessTokenStatus status = accessToken.status().withErrorType(AccessTokenErrorType.TEMPORARY_ERROR);
        accessToken = accessToken.withStatus(status);
        message += " due to " + accessToken.status().errorType();
        assertEquals(message, AnalyticsHelper.resolveStatusMessage(accessToken));

        String reason = "some-reason";
        status = accessToken.status().withReason(reason);
        accessToken = accessToken.withStatus(status);
        message += " (" + reason + ")";
        assertEquals(message, AnalyticsHelper.resolveStatusMessage(accessToken));
    }

    private void checkStatusThrowing(final AccessToken accessToken) throws AnalyticsException {
        AnalyticsHelper.checkAccessToken(accessToken);
    }

    private void checkStatusThrowing(final TokenStatus tokenStatus) throws AnalyticsException {
        checkStatusThrowing(createAccessToken(tokenStatus));
    }

    @NotNull
    private static AnalyticsKey createAnalyticsKey() {
        return AnalyticsKey.builder().jsKey("some-js-key").m2mKey("some-m2m-key").build();
    }

    private AccessToken createAccessToken(final TokenStatus tokenStatus) {
        return AnalyticsTestUtils
            .createAccessToken(
                "a1b2c3d4e5f6",
                "some-client-i",
                "some-audience",
                "some-scope",
                "some-token-type",
                tokenStatus)
            .withExpiresIn(ANALYTICS_ACCESS_TOKEN_TTL);
    }

    private AccessToken createAccessToken() {
        return createAccessToken(TokenStatus.OK);
    }

    private AccessToken createFreshAccessToken() {
        return createAccessToken(TokenStatus.OK).withIssueDate(Instant.now());
    }

}
