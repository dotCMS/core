package com.dotcms.filters.interceptor.dotcms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.util.security.Encryptor;
import com.dotmarketing.business.LoginAsSession;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.WebKeys;
import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Test;

/**
 * Renewal of the session login token cookie by {@link DefaultAutoLoginWebInterceptor} for a user
 * who already has an HTTP session.
 */
public class DefaultAutoLoginWebInterceptorTest extends UnitTestBase {

    private static final String LOGIN_COOKIE_VALUE = "a.b.c";

    private final LoginServiceAPI loginServiceAPI = mock(LoginServiceAPI.class);
    private final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
    private final DefaultAutoLoginWebInterceptor interceptor = new DefaultAutoLoginWebInterceptor(
            jsonWebTokenUtils, loginServiceAPI, mock(Encryptor.class));
    /** The user held by the HTTP session. */
    private final User sessionUser = user("session-user");
    /** The same user as loaded from the login cookie's token. */
    private final User cookieOwner = user("session-user");

    /**
     * A back-end request whose session is past its renewal time gets a new session login token,
     * issued for the cookie's owner as loaded from its token rather than for the session's copy.
     */
    @Test
    public void renewsSessionLoginToken_whenDueOnBackEndPath() {
        cookieOwnedBy(cookieOwner);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpServletRequest request = request("/api/v1/users/current", session(pastRenewal()), true);

        assertEquals(Result.SKIP, interceptor.intercept(request, response));

        verify(loginServiceAPI).doRememberMe(request, response, cookieOwner, false);
    }

    /**
     * The cookie is not renewed before the session's renewal time.
     */
    @Test
    public void keepsSessionLoginToken_beforeRenewalTime() {
        cookieOwnedBy(cookieOwner);
        final HttpServletRequest request = request("/api/v1/users/current",
                session(System.currentTimeMillis() + 60_000L), true);

        assertEquals(Result.SKIP, interceptor.intercept(request, mock(HttpServletResponse.class)));

        verifyNoRenewal();
    }

    /**
     * Site pages never receive a renewed cookie, so their responses stay cacheable.
     */
    @Test
    public void keepsSessionLoginToken_onSitePage() {
        cookieOwnedBy(cookieOwner);
        final HttpServletRequest request = request("/about-us/index", session(pastRenewal()), true);

        assertEquals(Result.SKIP, interceptor.intercept(request, mock(HttpServletResponse.class)));

        verifyNoRenewal();
    }

    /**
     * A path that only starts with the admin application's name is not part of it.
     */
    @Test
    public void keepsSessionLoginToken_onPathThatOnlyStartsLikeTheAdminApp() {
        cookieOwnedBy(cookieOwner);
        final HttpServletRequest request = request("/dotAdminFoo/index", session(pastRenewal()), true);

        assertEquals(Result.SKIP, interceptor.intercept(request, mock(HttpServletResponse.class)));

        verifyNoRenewal();
    }

    /**
     * A request that does not carry the login cookie gets no new one.
     */
    @Test
    public void keepsSessionLoginToken_withoutLoginCookie() {
        final HttpServletRequest request = request("/dotAdmin/", session(pastRenewal()), false);

        assertEquals(Result.SKIP, interceptor.intercept(request, mock(HttpServletResponse.class)));

        verifyNoRenewal();
    }

    /**
     * A session with no renewal time (a "remember me" login) keeps its long-lived cookie.
     */
    @Test
    public void keepsRememberMeCookie_whenSessionHasNoRenewalTime() {
        cookieOwnedBy(cookieOwner);
        final HttpServletRequest request = request("/dotAdmin/", session(null), true);

        assertEquals(Result.SKIP, interceptor.intercept(request, mock(HttpServletResponse.class)));

        verifyNoRenewal();
    }

    /**
     * While an administrator is logged in as another user, the cookie is not renewed: neither
     * for the user being acted as nor for the administrator.
     */
    @Test
    public void keepsLoginCookie_whileLoggedInAsAnotherUser() {
        final User administrator = user("administrator");
        final User actedAs = user("acted-as");
        cookieOwnedBy(administrator);
        final HttpSession session = session(pastRenewal());
        when(session.getAttribute(WebKeys.USER)).thenReturn(actedAs);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(actedAs.getUserId());
        when(session.getAttribute(WebKeys.PRINCIPAL_USER_ID)).thenReturn(administrator.getUserId());
        when(session.getAttribute(LoginAsSession.SESSION_KEY))
                .thenReturn(new LoginAsSession(administrator.getUserId(), actedAs.getUserId()));

        assertEquals(Result.SKIP, interceptor.intercept(
                request("/api/v1/users/current", session, true), mock(HttpServletResponse.class)));

        verifyNoRenewal();
    }

    /**
     * A login cookie that belongs to a different user than the session's is not renewed.
     */
    @Test
    public void keepsLoginCookie_ownedByAnotherUser() {
        cookieOwnedBy(user("someone-else"));
        final HttpServletRequest request = request("/api/v1/users/current", session(pastRenewal()), true);

        assertEquals(Result.SKIP, interceptor.intercept(request, mock(HttpServletResponse.class)));

        verifyNoRenewal();
    }

    /**
     * A login cookie whose token no longer validates (expired, a user who is now inactive, or a
     * token id replaced by a logout or password change) is not renewed.
     */
    @Test
    public void keepsLoginCookie_whoseTokenNoLongerValidates() {
        cookieOwnedBy(null);
        final HttpServletRequest request = request("/api/v1/users/current", session(pastRenewal()), true);

        assertEquals(Result.SKIP, interceptor.intercept(request, mock(HttpServletResponse.class)));

        verifyNoRenewal();
    }

    /**
     * A request authenticated by an Authorization header, with no HTTP session and no login cookie,
     * passes through without any cookie being set.
     */
    @Test
    public void leavesBearerRequestUntouched() {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpServletRequest request = request("/api/v1/content", null, false);
        when(request.getAttribute(WebKeys.USER)).thenReturn(sessionUser);
        when(request.getHeader("Authorization")).thenReturn("Bearer some.api.token");

        assertEquals(Result.SKIP, interceptor.intercept(request, response));

        verifyNoRenewal();
        verify(response, never()).addCookie(any(Cookie.class));
    }

    /**
     * Two back-end requests in a row after the renewal time both stay logged in: each may renew
     * the cookie, and the session is neither invalidated nor stripped of its user.
     */
    @Test
    public void twoDueRequestsInARow_keepTheSession() {
        cookieOwnedBy(cookieOwner);
        final HttpSession session = session(pastRenewal());
        final HttpServletResponse response = mock(HttpServletResponse.class);

        assertEquals(Result.SKIP, interceptor.intercept(request("/api/v1/a", session, true), response));
        assertEquals(Result.SKIP, interceptor.intercept(request("/api/v1/b", session, true), response));

        verify(loginServiceAPI, times(2)).doRememberMe(any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(User.class), anyBoolean());
        verify(session, never()).invalidate();
        verify(session, never()).removeAttribute(WebKeys.USER);
    }

    /**
     * A token issued for the session token lifetime (60 minutes by default) is a session login
     * token, whatever time it has left.
     */
    @Test
    public void tokenIssuedForTheSessionLifetime_isASessionLoginToken() {
        final long now = System.currentTimeMillis();

        assertTrue(DefaultAutoLoginWebInterceptor.isSessionLoginToken(
                token(now, now + TimeUnit.MINUTES.toMillis(60))));
        assertTrue(DefaultAutoLoginWebInterceptor.isSessionLoginToken(
                token(now - TimeUnit.MINUTES.toMillis(50), now + TimeUnit.MINUTES.toMillis(10))));
    }

    /**
     * A "remember me" token in its last hour is not a session login token, so a login restored
     * from it does not turn its persistent cookie into a renewed session cookie.
     */
    @Test
    public void rememberMeTokenInItsLastHour_isNotASessionLoginToken() {
        final long now = System.currentTimeMillis();
        final long expires = now + TimeUnit.MINUTES.toMillis(30);

        assertFalse(DefaultAutoLoginWebInterceptor.isSessionLoginToken(
                token(expires - TimeUnit.DAYS.toMillis(14), expires)));
    }

    /**
     * A token without an issue time is not treated as a session login token.
     */
    @Test
    public void tokenWithoutIssueTime_isNotASessionLoginToken() {
        final JWToken token = mock(JWToken.class);
        when(token.getClaims()).thenReturn(ImmutableMap.of());
        when(token.getExpiresDate()).thenReturn(new Date());

        assertFalse(DefaultAutoLoginWebInterceptor.isSessionLoginToken(token));
    }

    private static JWToken token(final long issuedAtMillis, final long expiresMillis) {
        final JWToken token = mock(JWToken.class);
        when(token.getClaims()).thenReturn(
                ImmutableMap.of(Claims.ISSUED_AT, TimeUnit.MILLISECONDS.toSeconds(issuedAtMillis)));
        when(token.getExpiresDate()).thenReturn(new Date(expiresMillis));
        return token;
    }

    private void verifyNoRenewal() {
        verify(loginServiceAPI, never()).doRememberMe(any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(User.class), anyBoolean());
    }

    private void cookieOwnedBy(final User owner) {
        when(jsonWebTokenUtils.getUser(any(), any())).thenReturn(owner);
    }

    private static User user(final String userId) {
        final User user = new User();
        user.setUserId(userId);
        return user;
    }

    private static Long pastRenewal() {
        return System.currentTimeMillis() - 1_000L;
    }

    private HttpSession session(final Long renewAt) {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebKeys.USER)).thenReturn(sessionUser);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(sessionUser.getUserId());
        when(session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT)).thenReturn(renewAt);
        return session;
    }

    private static HttpServletRequest request(final String uri, final HttpSession session,
            final boolean withLoginCookie) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getSession(false)).thenReturn(session);
        when(request.getCookies()).thenReturn(withLoginCookie
                ? new Cookie[]{new Cookie(CookieKeys.REMEMBER_ME_COOKIE.get(), LOGIN_COOKIE_VALUE)}
                : null);
        return request;
    }
}
