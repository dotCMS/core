package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LoginAsSession;
import com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CookieUtil;
import io.jsonwebtoken.Claims;
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
            List.of("/api/", "/dotAdmin/", "/html/", "/c/", "/dwr/");
    private static final String ADMIN_APPLICATION_ROOT = "/dotAdmin";

    /** Allowance for the second-precision issue and expiry times of a token. */
    private static final long TOKEN_TIME_SLACK_MILLIS = TimeUnit.MINUTES.toMillis(1);

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
     * when it is a session login token, so an active user keeps a valid cookie. A token counts as
     * one when it was issued for no longer than the session token lifetime; a "remember me" token
     * is left as it is, even in its last hour.
     */
    private void scheduleRenewalOfSessionLoginToken(final HttpServletRequest request, final JWToken token) {

        final HttpSession session = request.getSession(false);
        if (null != session && isSessionLoginToken(token)) {
            session.setAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT, 0L);
        }
    }

    /**
     * Whether the token was issued as a session login token: its whole lifetime (expiry minus
     * issue time) is no longer than the configured session token lifetime. The time it has left
     * does not matter, so a "remember me" token near its expiry is not one.
     *
     * @param token a validated login token
     * @return {@code true} for a session login token
     */
    @VisibleForTesting
    static boolean isSessionLoginToken(final JWToken token) {

        final Object issuedAtSeconds = token.getClaims().get(Claims.ISSUED_AT);
        if (!(issuedAtSeconds instanceof Number) || null == token.getExpiresDate()) {
            return false;
        }
        final long issuedFor = token.getExpiresDate().getTime()
                - TimeUnit.SECONDS.toMillis(((Number) issuedAtSeconds).longValue());
        return issuedFor <= LoginServiceAPI.sessionLoginTokenLifetime().toMillis() + TOKEN_TIME_SLACK_MILLIS;
    }

    /**
     * Issues a new session login token cookie once the renewal time recorded on the HTTP session
     * has passed. The new token is issued for the owner of the presented login cookie, loaded from
     * that token (so an inactive user or a token replaced by a logout or password change is never
     * renewed), and only when that owner is the session's user. Nothing is renewed while the
     * session is logged in as another user. Only back-end application requests that carry the
     * login cookie renew it, so site pages and assets never set a cookie; a session without a
     * renewal time (a "remember me" login, or a request authenticated by a header) is left alone.
     * Two requests that pass the check together each issue a valid token, so either cookie works.
     * A due renewal that cannot happen (no cookie, or a cookie that is not valid for the session's
     * user) is removed from the session, so later requests do not read the cookie again.
     */
    private void renewSessionLoginTokenIfDue(final HttpServletRequest request,
                                             final HttpServletResponse response) {

        final HttpSession session = request.getSession(false);
        if (null == session || isLoggedInAsAnotherUser(session)) {
            return;
        }
        final Object renewAt = session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
        if (!(renewAt instanceof Long) || System.currentTimeMillis() < (Long) renewAt
                || !isBackEndApplicationPath(request.getRequestURI())) {
            return;
        }
        final Cookie loginCookie = UtilMethods.getCookie(request.getCookies(), CookieKeys.REMEMBER_ME_COOKIE.get());
        final User cookieOwner = null == loginCookie ? null : Try.of(() -> this.jsonWebTokenUtils.getUser(
                loginCookie.getValue(), request.getRemoteAddr())).getOrNull();
        final Object sessionUserId = session.getAttribute(WebKeys.USER_ID);
        if (null != cookieOwner && null != sessionUserId
                && cookieOwner.getUserId().equals(sessionUserId.toString())) {
            this.loginServiceAPI.doRememberMe(request, response, cookieOwner, false);
            return;
        }
        // The cookie cannot become valid again by itself, so the renewal is dropped rather than
        // retried on every request; the next login schedules a new one.
        session.removeAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
        Logger.debug(this, () -> String.format("Session login token not renewed for user '%s': the request "
                + "carries %s", sessionUserId, null == loginCookie ? "no login cookie"
                : "a login cookie that is not valid for this user"));
    }

    private static boolean isLoggedInAsAnotherUser(final HttpSession session) {

        return null != session.getAttribute(LoginAsSession.SESSION_KEY)
                || null != session.getAttribute(WebKeys.PRINCIPAL_USER_ID);
    }

    private static boolean isBackEndApplicationPath(final String uri) {

        return null != uri && (ADMIN_APPLICATION_ROOT.equals(uri)
                || BACK_END_APPLICATION_PREFIXES.stream().anyMatch(uri::startsWith));
    }


} // E:O:F:DefaultAutoLoginWebInterceptor.
