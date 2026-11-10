package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.CookieUtil;
import io.vavr.control.Try;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Default Auto Login implementation
 *
 * @author jsanca
 */
public class DefaultAutoLoginWebInterceptor implements WebInterceptor {

    /** Paths of the back-end application, whose responses are never cached. */
    private static final List<String> BACK_END_APPLICATION_PREFIXES =
            List.of("/api/", "/dotAdmin", "/html/", "/c/", "/dwr/");

    private final JsonWebTokenUtils jsonWebTokenUtils;
    private final LoginServiceAPI loginServiceAPI;
    private Encryptor encryptor;

    public DefaultAutoLoginWebInterceptor() {
        this(JsonWebTokenUtils.getInstance(), APILocator.getLoginServiceAPI(),
                EncryptorFactory.getInstance().getEncryptor());
    }

    public DefaultAutoLoginWebInterceptor(final JsonWebTokenUtils jsonWebTokenUtils,
            final LoginServiceAPI loginServiceAPI,
            final Encryptor encryptor) {

        this.jsonWebTokenUtils = jsonWebTokenUtils;
        this.loginServiceAPI = loginServiceAPI;
        this.encryptor = encryptor;
    }

    /**
     * Logs the request in from the login token cookie when there is no logged-in user yet, and
     * renews a session login token cookie that is due for renewal when there is one.
     */
    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {

        if (PortalUtil.getUser(request) != null) {
            this.renewSessionLoginTokenIfDue(request, response);
            return Result.SKIP;
        }

        final Cookie rememberMe = UtilMethods.getCookie(request.getCookies(), CookieKeys.REMEMBER_ME_COOKIE.get());
        if (rememberMe == null) {
            return Result.NEXT;
        }


        final Optional<JWToken> token = APILocator.getApiTokenAPI().fromJwt(rememberMe.getValue(), request.getRemoteAddr());
        Optional<User> user= token.flatMap(JWToken::getActiveUser);

        if(user.isEmpty()) {
            // user is null because token is expired
            CookieUtil.deleteCookie(request, response, CookieKeys.JWT_ACCESS_TOKEN);
            return Result.NEXT;
        }

        // if the token was expiry date is greater than the allowed EXPIRY date, reset it
        // maybe someone updated the configured MAX_AGE_DAYS
        if(token.get().getExpiresDate().after(Date.from(Instant.now().plus(LoginServiceAPI.JWT_TOKEN_MAX_AGE_DAYS.get(), ChronoUnit.DAYS)))) {
            // refresh the token
            this.loginServiceAPI.doRememberMe(request, response, user.get(), true);
        }

        if (this.loginServiceAPI.doCookieLogin(token.get().getSubject(), request, response)) {
            this.scheduleRenewalOfSessionLoginToken(request, token.get());
            this.renewSessionLoginTokenIfDue(request, response);
            return Result.SKIP;
        }

        // user is null because token is expired
        CookieUtil.deleteCookie(request, response, CookieKeys.JWT_ACCESS_TOKEN);
        return Result.NEXT;


    } // intercept.

    /**
     * After a login from the cookie, marks the new HTTP session's login token as due for renewal
     * when it is a session login token (one that expires within the session token lifetime), so
     * an active user keeps a valid cookie. A longer "remember me" token is left as it is.
     */
    private void scheduleRenewalOfSessionLoginToken(final HttpServletRequest request, final JWToken token) {

        final long sessionLifetimeMillis = TimeUnit.MINUTES.toMillis(Config.getIntProperty(
                LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES,
                LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES_DEFAULT));
        final HttpSession session = request.getSession(false);
        if (null != session && token.getExpiresDate().getTime()
                <= System.currentTimeMillis() + sessionLifetimeMillis) {
            session.setAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT, 0L);
        }
    }

    /**
     * Issues a new session login token cookie for the session's user once the renewal time
     * recorded on the HTTP session has passed. Only back-end application requests that carry the
     * login cookie renew it, so site pages and assets never set a cookie; a session without a
     * renewal time (a "remember me" login, or a request authenticated by a header) is left alone.
     * Two requests that pass the check together each issue a valid token, so either cookie works.
     */
    private void renewSessionLoginTokenIfDue(final HttpServletRequest request,
                                             final HttpServletResponse response) {

        final HttpSession session = request.getSession(false);
        if (null == session) {
            return;
        }
        final Object renewAt = session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
        if (!(renewAt instanceof Long) || System.currentTimeMillis() < (Long) renewAt
                || !isBackEndApplicationPath(request.getRequestURI())
                || null == UtilMethods.getCookie(request.getCookies(), CookieKeys.REMEMBER_ME_COOKIE.get())) {
            return;
        }
        final User sessionUser = PortalUtil.getUser(session);
        if (null != sessionUser) {
            this.loginServiceAPI.doRememberMe(request, response, sessionUser, false);
        }
    }

    private static boolean isBackEndApplicationPath(final String uri) {

        return null != uri && BACK_END_APPLICATION_PREFIXES.stream().anyMatch(uri::startsWith);
    }


} // E:O:F:DefaultAutoLoginWebInterceptor.
