package com.dotcms.filters.interceptor.dotcms;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.util.security.Encryptor;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.WebKeys;
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

    private final LoginServiceAPI loginServiceAPI = mock(LoginServiceAPI.class);
    private final DefaultAutoLoginWebInterceptor interceptor = new DefaultAutoLoginWebInterceptor(
            mock(JsonWebTokenUtils.class), loginServiceAPI, mock(Encryptor.class));
    private final User user = new User();

    /**
     * A back-end request whose session is past its renewal time gets a new session login token.
     */
    @Test
    public void renewsSessionLoginToken_whenDueOnBackEndPath() {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpServletRequest request = request("/api/v1/users/current", session(pastRenewal()), true);

        assertEquals(Result.SKIP, interceptor.intercept(request, response));

        verify(loginServiceAPI).doRememberMe(request, response, user, false);
    }

    /**
     * The cookie is not renewed before the session's renewal time.
     */
    @Test
    public void keepsSessionLoginToken_beforeRenewalTime() {
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
        final HttpServletRequest request = request("/about-us/index", session(pastRenewal()), true);

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
        final HttpServletRequest request = request("/dotAdmin/", session(null), true);

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
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        when(request.getHeader("Authorization")).thenReturn("Bearer some.api.token");

        assertEquals(Result.SKIP, interceptor.intercept(request, response));

        verifyNoRenewal();
        verify(response, never()).addCookie(any(Cookie.class));
    }

    /**
     * Two requests that arrive together after the renewal time both stay logged in: each may renew
     * the cookie, and the session is neither invalidated nor stripped of its user.
     */
    @Test
    public void concurrentRequestsAfterRenewalTime_keepTheSession() {
        final HttpSession session = session(pastRenewal());
        final HttpServletResponse response = mock(HttpServletResponse.class);

        assertEquals(Result.SKIP, interceptor.intercept(request("/api/v1/a", session, true), response));
        assertEquals(Result.SKIP, interceptor.intercept(request("/api/v1/b", session, true), response));

        verify(loginServiceAPI, times(2)).doRememberMe(any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(User.class), anyBoolean());
        verify(session, never()).invalidate();
        verify(session, never()).removeAttribute(WebKeys.USER);
        verify(response, never()).addCookie(any(Cookie.class));
    }

    private void verifyNoRenewal() {
        verify(loginServiceAPI, never()).doRememberMe(any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(User.class), anyBoolean());
    }

    private static Long pastRenewal() {
        return System.currentTimeMillis() - 1_000L;
    }

    private HttpSession session(final Long renewAt) {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebKeys.USER)).thenReturn(user);
        when(session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT)).thenReturn(renewAt);
        return session;
    }

    private static HttpServletRequest request(final String uri, final HttpSession session,
            final boolean withLoginCookie) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getSession(false)).thenReturn(session);
        when(request.getCookies()).thenReturn(withLoginCookie
                ? new Cookie[]{new Cookie(CookieKeys.REMEMBER_ME_COOKIE.get(), "a.b.c")}
                : null);
        return request;
    }
}
