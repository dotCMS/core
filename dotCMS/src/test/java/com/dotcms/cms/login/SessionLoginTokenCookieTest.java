package com.dotcms.cms.login;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.lang.reflect.Constructor;
import java.time.Duration;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.Test;

/**
 * Scheduling the renewal of a session login token cookie set through
 * {@link LoginServiceAPI#doRememberMe(HttpServletRequest, HttpServletResponse, User, int)}.
 */
public class SessionLoginTokenCookieTest extends UnitTestBase {

    private static final String SET_JWT_SESSION_COOKIE = "SET_JWT_SESSION_COOKIE";

    private final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
    private final HttpSession session = mock(HttpSession.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @After
    public void restoreSessionCookies() {
        Config.setProperty(SET_JWT_SESSION_COOKIE, true);
    }

    /**
     * With session cookies on, a session login token cookie schedules its renewal.
     */
    @Test
    public void sessionCookie_schedulesRenewal() throws Exception {
        Config.setProperty(SET_JWT_SESSION_COOKIE, true);

        loginService().doRememberMe(request, response, new User(), 0);

        verify(response).addCookie(any(Cookie.class));
        verify(session).setAttribute(eq(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT), anyLong());
    }

    /**
     * With session cookies turned off, a plugin login through the days overload still gets its
     * cookie, but no renewal is scheduled, since the renewal could not issue a new cookie.
     */
    @Test
    public void sessionCookiesTurnedOff_scheduleNoRenewal() throws Exception {
        Config.setProperty(SET_JWT_SESSION_COOKIE, false);

        loginService().doRememberMe(request, response, new User(), 7);

        verify(response).addCookie(any(Cookie.class));
        verify(session, never()).setAttribute(eq(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT), any());
        verify(session).removeAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
    }

    /**
     * A renewal issues the token with the id it was given, not the id the user holds by then, which
     * a logout or password change may have replaced.
     */
    @Test
    public void issueSessionLoginToken_usesGivenTokenId() throws Exception {
        final User user = new User();
        user.setRememberMeToken("id-set-by-password-change");

        loginService().issueSessionLoginToken(request, response, user, "validated-token-id");

        verify(jsonWebTokenUtils).createSessionLoginToken(eq(user), eq("validated-token-id"), any(Duration.class));
        verify(response).addCookie(any(Cookie.class));
    }

    /**
     * A login without "remember me" issues the token with the id the user holds.
     */
    @Test
    public void loginWithoutRememberMe_usesUsersTokenId() throws Exception {
        final User user = new User();
        user.setRememberMeToken("current-id");

        loginService().doRememberMe(request, response, user, false);

        verify(jsonWebTokenUtils).createSessionLoginToken(eq(user), eq("current-id"), any(Duration.class));
    }

    /**
     * A login service built on a new factory, so the session cookie setting is read again.
     */
    private LoginServiceAPI loginService() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(jsonWebTokenUtils.createSessionLoginToken(any(User.class), any(), any(Duration.class))).thenReturn("a.b.c");

        final Constructor<LoginServiceAPIFactory> factoryConstructor =
                LoginServiceAPIFactory.class.getDeclaredConstructor();
        factoryConstructor.setAccessible(true);
        final Constructor<?> serviceConstructor = Class.forName(
                LoginServiceAPIFactory.class.getName() + "$LoginServiceImpl").getDeclaredConstructor(
                LoginServiceAPIFactory.class, ApiProvider.class, JsonWebTokenUtils.class,
                HttpServletRequestThreadLocal.class, UserAPI.class);
        serviceConstructor.setAccessible(true);
        return (LoginServiceAPI) serviceConstructor.newInstance(factoryConstructor.newInstance(),
                mock(ApiProvider.class), jsonWebTokenUtils, mock(HttpServletRequestThreadLocal.class),
                mock(UserAPI.class));
    }
}
