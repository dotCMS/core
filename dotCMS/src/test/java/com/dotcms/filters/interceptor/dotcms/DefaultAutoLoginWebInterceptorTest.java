package com.dotcms.filters.interceptor.dotcms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils.LoginTokenOwner;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.util.security.Encryptor;
import com.dotmarketing.business.LoginAsSession;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.WebKeys;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    /** The id carried by the validated login cookie. */
    private static final String TOKEN_ID = "validated-token-id";

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

        verify(loginServiceAPI).issueSessionLoginToken(request, response, cookieOwner, TOKEN_ID);
    }

    /**
     * A password change that writes a new token id onto the shared user after the cookie was
     * validated does not reach the renewed token: it carries the validated id, so it stops
     * validating along with the old cookie.
     */
    @Test
    public void renewedToken_carriesValidatedId_whenUserIdChangesAfterValidation() {
        cookieOwner.setRememberMeToken(TOKEN_ID);
        when(jsonWebTokenUtils.getLoginTokenOwner(any(), any())).thenAnswer(call -> {
            final Optional<LoginTokenOwner> validated = owned(cookieOwner);
            cookieOwner.setRememberMeToken("id-set-by-password-change");
            return validated;
        });
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpServletRequest request = request("/api/v1/users/current", session(pastRenewal()), true);

        interceptor.intercept(request, response);

        verify(loginServiceAPI).issueSessionLoginToken(request, response, cookieOwner, TOKEN_ID);
    }

    /**
     * A response that carries a renewed cookie is marked as not to be stored before the request
     * goes on, so a page rendered in live mode further down (a GraphQL GET, which would otherwise
     * set the page's max-age) cannot make it cacheable.
     */
    @Test
    public void renewedCookieResponse_isMarkedNotToBeStored() {
        cookieOwnedBy(cookieOwner);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        interceptor.intercept(request("/api/v1/graphql", session(pastRenewal()), true), response);

        verify(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    }

    /**
     * A request that renews nothing leaves the response's cache headers to the endpoint.
     */
    @Test
    public void responseWithoutRenewal_keepsItsCacheHeaders() {
        cookieOwnedBy(cookieOwner);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        interceptor.intercept(request("/api/v1/graphql",
                session(System.currentTimeMillis() + 60_000L), true), response);

        verify(response, never()).setHeader(eq("Cache-Control"), anyString());
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
        final HttpServletRequest request = request("/api/v1/users/current", session(pastRenewal()), false);

        assertEquals(Result.SKIP, interceptor.intercept(request, mock(HttpServletResponse.class)));

        verifyNoRenewal();
    }

    /**
     * A session with no renewal time (a "remember me" login) keeps its long-lived cookie.
     */
    @Test
    public void keepsRememberMeCookie_whenSessionHasNoRenewalTime() {
        cookieOwnedBy(cookieOwner);
        final HttpServletRequest request = request("/api/v1/users/current", session(null), true);

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
     * When a due renewal does not happen because the cookie no longer validates, the renewal is
     * dropped from the session, so the next back-end request does not read the cookie again.
     */
    @Test
    public void failedRenewal_isNotRetriedOnTheNextRequest() {
        cookieOwnedBy(null);
        final HttpSession session = statefulSession(pastRenewal());

        interceptor.intercept(request("/api/v1/a", session, true), mock(HttpServletResponse.class));
        interceptor.intercept(request("/api/v1/b", session, true), mock(HttpServletResponse.class));

        verify(jsonWebTokenUtils, times(1)).getLoginTokenOwner(any(), any());
        verify(session).removeAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
        verifyNoRenewal();
    }

    /**
     * A due renewal on a request without the login cookie is dropped from the session as well.
     */
    @Test
    public void dueRenewalWithoutLoginCookie_isDropped() {
        final HttpSession session = statefulSession(pastRenewal());

        interceptor.intercept(request("/api/v1/a", session, false), mock(HttpServletResponse.class));

        verify(session).removeAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
        assertNull(session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT));
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
     * A renewal moves the session's renewal time forward, as the login service does when it sets
     * the new cookie, so a second back-end request right after it does not renew again.
     */
    @Test
    public void secondRequestAfterRenewal_doesNotRenewAgain() {
        cookieOwnedBy(cookieOwner);
        final HttpSession session = statefulSession(pastRenewal());
        final long nextRenewal = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30);
        doAnswer(call -> {
            session.setAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT, nextRenewal);
            return null;
        }).when(loginServiceAPI).issueSessionLoginToken(any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(User.class), anyString());
        final HttpServletResponse response = mock(HttpServletResponse.class);

        assertEquals(Result.SKIP, interceptor.intercept(request("/api/v1/a", session, true), response));
        assertEquals(Result.SKIP, interceptor.intercept(request("/api/v1/b", session, true), response));

        verify(loginServiceAPI, times(1)).issueSessionLoginToken(any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(User.class), anyString());
        assertEquals(nextRenewal, session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT));
    }

    /**
     * An unexpected error while loading the cookie's owner (a database error, for example) is not
     * read as a cookie that no longer validates: the renewal stays on the session and the next
     * back-end request tries again.
     */
    @Test
    public void errorLoadingCookieOwner_keepsRenewalForTheNextRequest() {
        final Long renewAt = pastRenewal();
        final HttpSession session = statefulSession(renewAt);
        when(jsonWebTokenUtils.getLoginTokenOwner(any(), any()))
                .thenThrow(new IllegalStateException("user load failed"))
                .thenReturn(owned(cookieOwner));
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final HttpServletRequest first = request("/api/v1/a", session, true);
        assertEquals(Result.SKIP, interceptor.intercept(first, response));
        verifyNoRenewal();
        assertEquals(renewAt, session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT));

        final HttpServletRequest second = request("/api/v1/b", session, true);
        assertEquals(Result.SKIP, interceptor.intercept(second, response));
        verify(loginServiceAPI).issueSessionLoginToken(second, response, cookieOwner, TOKEN_ID);
    }

    /**
     * An error while issuing the new token does not fail the request, and the renewal stays on the
     * session for the next back-end request.
     */
    @Test
    public void errorIssuingRenewedToken_keepsRequestAndRenewal() {
        cookieOwnedBy(cookieOwner);
        final Long renewAt = pastRenewal();
        final HttpSession session = statefulSession(renewAt);
        doAnswer(call -> {
            throw new IllegalStateException("user load failed");
        }).when(loginServiceAPI).issueSessionLoginToken(any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(User.class), anyString());

        assertEquals(Result.SKIP, interceptor.intercept(request("/api/v1/a", session, true),
                mock(HttpServletResponse.class)));

        assertEquals(renewAt, session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT));
    }

    /**
     * A renewal left on a session that is logged in as another user is not due, since nothing is
     * renewed there; otherwise the GraphQL cache would be skipped for the whole of that session.
     */
    @Test
    public void renewalIsNotDue_whileLoggedInAsAnotherUser() {
        final HttpSession session = session(pastRenewal());
        final HttpServletRequest request = request("/api/v1/graphql", session, true);
        assertTrue(DefaultAutoLoginWebInterceptor.isSessionLoginTokenRenewalDue(request));

        when(session.getAttribute(WebKeys.PRINCIPAL_USER_ID)).thenReturn("administrator");

        assertFalse(DefaultAutoLoginWebInterceptor.isSessionLoginTokenRenewalDue(request));
    }

    /**
     * A token carrying the session login claim is a session login token.
     */
    @Test
    public void tokenWithSessionLoginClaim_isASessionLoginToken() {
        assertTrue(DefaultAutoLoginWebInterceptor.isSessionLoginToken(
                token(ImmutableMap.of(JsonWebTokenUtils.CLAIM_SESSION_LOGIN, true))));
    }

    /**
     * A token without the claim (a "remember me" token, or one issued before the claim existed)
     * is not a session login token, however short its lifetime, so the session lifetime may be
     * set as long as the "remember me" lifetime without the two being confused.
     */
    @Test
    public void tokenWithoutSessionLoginClaim_isNotASessionLoginToken() {
        assertFalse(DefaultAutoLoginWebInterceptor.isSessionLoginToken(token(ImmutableMap.of())));
        assertFalse(DefaultAutoLoginWebInterceptor.isSessionLoginToken(
                token(ImmutableMap.of(JsonWebTokenUtils.CLAIM_SESSION_LOGIN, "true"))));
    }

    /**
     * Static files are never sent a renewed cookie, since a cache may keep them by file type:
     * {@code /html/} (whose common CSS and script pages are sent with public cache headers), the
     * admin application's entry page and bundles, and the DWR scripts.
     */
    @Test
    public void keepsSessionLoginToken_onStaticFiles() {
        cookieOwnedBy(cookieOwner);

        for (final String uri : new String[]{"/html/common/css.jsp", "/html/js/dojo/dojo.js",
                "/dotAdmin", "/dotAdmin/", "/dotAdmin/main-ABC123.js", "/dwr/engine.js",
                "/dwr/interface/UserAjax.js"}) {
            interceptor.intercept(request(uri, session(pastRenewal()), true), mock(HttpServletResponse.class));
        }

        verifyNoRenewal();
    }

    /**
     * A VTL endpoint's response is written by a site developer's script, which may mark it as
     * publicly cacheable, so it is never sent a renewed cookie.
     */
    @Test
    public void keepsSessionLoginToken_onScriptedVtlEndpoint() {
        cookieOwnedBy(cookieOwner);

        interceptor.intercept(request("/api/vtl/products/list", session(pastRenewal()), true),
                mock(HttpServletResponse.class));

        verifyNoRenewal();
    }

    /**
     * A JavaScript endpoint's response is written by a site developer's script, which may mark it
     * as publicly cacheable, so it is never sent a renewed cookie.
     */
    @Test
    public void keepsSessionLoginToken_onScriptedJsEndpoint() {
        cookieOwnedBy(cookieOwner);

        interceptor.intercept(request("/api/js/products/list", session(pastRenewal()), true),
                mock(HttpServletResponse.class));

        verifyNoRenewal();
    }

    /**
     * REST calls, legacy portal actions and DWR calls renew a due cookie.
     */
    @Test
    public void renewsSessionLoginToken_onDynamicBackEndPaths() {
        cookieOwnedBy(cookieOwner);

        for (final String uri : new String[]{"/api/v1/users/current", "/c/portal/layout",
                "/dotAdmin/c/portal/layout", "/dwr/call/plaincall/UserAjax.getUserById.dwr"}) {
            final HttpServletRequest request = request(uri, session(pastRenewal()), true);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            interceptor.intercept(request, response);
            verify(loginServiceAPI).issueSessionLoginToken(request, response, cookieOwner, TOKEN_ID);
        }
    }

    /**
     * A front-end user restored from the login cookie is kept on the session only as the CMS
     * user; that still identifies the session's user, so its cookie is renewed.
     */
    @Test
    public void renewsSessionLoginToken_forFrontEndUserKeptAsCmsUser() {
        cookieOwnedBy(cookieOwner);
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER)).thenReturn(sessionUser);
        when(session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT)).thenReturn(pastRenewal());
        final HttpServletRequest request = request("/api/v1/content", session, true);
        when(request.getAttribute(WebKeys.USER)).thenReturn(sessionUser);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        assertEquals(Result.SKIP, interceptor.intercept(request, response));

        verify(loginServiceAPI).issueSessionLoginToken(request, response, cookieOwner, TOKEN_ID);
    }

    private static JWToken token(final Map<String, Object> claims) {
        final JWToken token = mock(JWToken.class);
        when(token.getClaims()).thenReturn(ImmutableMap.copyOf(claims));
        return token;
    }

    private void verifyNoRenewal() {
        verify(loginServiceAPI, never()).issueSessionLoginToken(any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(User.class), anyString());
        verify(loginServiceAPI, never()).doRememberMe(any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(User.class), anyBoolean());
    }

    private void cookieOwnedBy(final User owner) {
        when(jsonWebTokenUtils.getLoginTokenOwner(any(), any())).thenReturn(owned(owner));
    }

    private static Optional<LoginTokenOwner> owned(final User owner) {
        return Optional.ofNullable(owner).map(user -> new LoginTokenOwner(user, TOKEN_ID));
    }

    private static User user(final String userId) {
        final User user = new User();
        user.setUserId(userId);
        return user;
    }

    private static Long pastRenewal() {
        return System.currentTimeMillis() - 1_000L;
    }

    /**
     * A session whose attributes are kept in a map, so a change is seen by later requests.
     */
    private HttpSession statefulSession(final Long renewAt) {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebKeys.USER, sessionUser);
        attributes.put(WebKeys.USER_ID, sessionUser.getUserId());
        attributes.put(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT, renewAt);
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.<String>getArgument(0)));
        doAnswer(call -> attributes.remove(call.<String>getArgument(0)))
                .when(session).removeAttribute(anyString());
        doAnswer(call -> attributes.put(call.getArgument(0), call.getArgument(1)))
                .when(session).setAttribute(anyString(), any());
        return session;
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
