package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils.LoginTokenOwner;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.rest.annotation.HeaderFilter;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LoginAsSession;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CookieUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

/**
 * Default Auto Login implementation
 *
 * @author jsanca
 */
public class DefaultAutoLoginWebInterceptor implements WebInterceptor {

    /**
     * Dynamic back-end paths whose responses are never cached. Static files (the admin
     * application, {@code /html/} and the DWR scripts) are left out: some are sent with public
     * cache headers, and a cache may keep any of them by file type, cookie included. The admin
     * application calls {@code /api/} as soon as it loads, so its next request renews the cookie.
     */
    private static final List<String> BACK_END_APPLICATION_PREFIXES =
            List.of("/api/", "/c/", "/dotAdmin/c/", "/dwr/call/");
    /**
     * Paths under {@code /api/} whose responses are written by site developers' scripts, which may
     * mark them as publicly cacheable.
     */
    private static final List<String> SCRIPTED_API_PREFIXES = List.of("/api/vtl/", "/api/js/");
    /**
     * Response headers that could let a shared cache keep a response, and with it a renewed login
     * cookie. Header names are compared ignoring case.
     */
    private static final Set<String> CACHING_HEADERS = caseInsensitiveSet(HeaderFilter.CACHE_CONTROL,
            HeaderFilter.EXPIRES, "Surrogate-Control", "CDN-Cache-Control");
    /**
     * One lock per HTTP session id, so the due check, the cookie validation, the new cookie and the
     * change to the session's renewal time happen as one step for each session.
     */
    private static final Striped<Lock> RENEWAL_LOCKS = Striped.lazyWeakLock(64);

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
            return this.renewSessionLoginTokenIfDue(request, response);
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

        // if a "remember me" token expires later than the allowed EXPIRY date, reset it
        // maybe someone updated the configured MAX_AGE_DAYS. A session login token has its own
        // lifetime and stays a session cookie.
        if(!isSessionLoginToken(token.get()) && token.get().getExpiresDate().after(Date.from(Instant.now().plus(LoginServiceAPI.JWT_TOKEN_MAX_AGE_DAYS.get(), ChronoUnit.DAYS)))) {
            // refresh the token
            this.loginServiceAPI.doRememberMe(request, response, user.get(), true);
        }

        if (this.loginServiceAPI.doCookieLogin(token.get().getSubject(), request, response)) {
            this.scheduleRenewalOfSessionLoginToken(request, token.get());
            return this.renewSessionLoginTokenIfDue(request, response);
        }

        // user is null because token is expired
        CookieUtil.deleteCookie(request, response, CookieKeys.JWT_ACCESS_TOKEN);
        return Result.NEXT;


    } // intercept.

    /**
     * After a login from the cookie, marks the HTTP session's login token as due for renewal when
     * it is a session login token, so an active user keeps a valid cookie; a "remember me" token is
     * left as it is. A renewal already scheduled on the session is kept, so a front-end user, whose
     * cookie login is repeated on each request, is not renewed on every request.
     */
    private void scheduleRenewalOfSessionLoginToken(final HttpServletRequest request, final JWToken token) {

        final HttpSession session = request.getSession(false);
        if (null != session && isSessionLoginToken(token)
                && null == session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT)) {
            session.setAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT, 0L);
        }
    }

    /**
     * Whether the token was issued as a session login token, which the login service marks with a
     * signed claim. Tokens issued before that claim existed are not session login tokens, so they
     * are never renewed and simply expire.
     *
     * @param token a validated login token
     * @return {@code true} for a session login token
     */
    @VisibleForTesting
    static boolean isSessionLoginToken(final JWToken token) {

        return Boolean.TRUE.equals(token.getClaims().get(JsonWebTokenUtils.CLAIM_SESSION_LOGIN));
    }

    /**
     * Whether the request's HTTP session has a session login token renewal whose time has passed
     * and that can run now. While the session is logged in as another user nothing is renewed, so
     * a renewal left on such a session is not due.
     *
     * @param request the current request
     * @return {@code true} when the next back-end request should renew the login cookie
     */
    public static boolean isSessionLoginTokenRenewalDue(final HttpServletRequest request) {

        final HttpSession session = request.getSession(false);
        if (null == session || isLoggedInAsAnotherUser(session)) {
            return false;
        }
        final Object renewAt = session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
        return renewAt instanceof Long && System.currentTimeMillis() >= (Long) renewAt;
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
     * The new token carries the id of the token that was validated, and the response that carries
     * it is marked as not to be stored by any cache; the request goes on with a response on which
     * later code (a rule that sets a response header, or a template) cannot change that.
     * The due check, the validation, the new cookie and the change to the renewal time run under
     * a lock for the session, so a request that drops a renewal cannot do so after another request
     * of the same session has just renewed the cookie and scheduled the next renewal.
     * A due renewal that cannot happen (no cookie, or a cookie that is not valid for the session's
     * user) is removed from the session, so later requests do not read the cookie again. An
     * unexpected error while loading the cookie's owner or issuing the new token is logged and the
     * renewal is kept, so the next back-end request tries again and the request itself goes on.
     *
     * @param request  the current request
     * @param response the current response
     * @return {@link Result#SKIP}, or a skip that carries a {@link NoStoreResponse} when a new
     *         cookie was set
     */
    private Result renewSessionLoginTokenIfDue(final HttpServletRequest request,
                                               final HttpServletResponse response) {

        if (!isSessionLoginTokenRenewalDue(request)
                || !isBackEndApplicationPath(HttpRequestDataUtil.getDispatchPath(request))) {
            return Result.SKIP;
        }
        final HttpSession session = request.getSession(false);
        final Lock lock = RENEWAL_LOCKS.get(session.getId());
        lock.lock();
        try {
            // Checked again under the lock: another request may have renewed or dropped it.
            if (!isSessionLoginTokenRenewalDue(request)) {
                return Result.SKIP;
            }
            return this.renewSessionLoginToken(request, response, session)
                    ? new Result.Builder().skip().wrap(new NoStoreResponse(response)).build()
                    : Result.SKIP;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Renews a due session login token, or drops the renewal when it cannot happen. Runs under
     * the session's renewal lock.
     *
     * @return {@code true} when the response was marked as not to be stored and a new cookie was
     *         issued on it
     */
    private boolean renewSessionLoginToken(final HttpServletRequest request,
                                           final HttpServletResponse response,
                                           final HttpSession session) {

        final Cookie loginCookie = UtilMethods.getCookie(request.getCookies(), CookieKeys.REMEMBER_ME_COOKIE.get());
        final String sessionUserId = sessionUserId(session);
        final LoginTokenOwner cookieOwner;
        try {
            cookieOwner = null == loginCookie ? null
                    : this.jsonWebTokenUtils.getLoginTokenOwner(loginCookie.getValue(), request.getRemoteAddr())
                            .orElse(null);
        } catch (final RuntimeException e) {
            Logger.warnAndDebug(DefaultAutoLoginWebInterceptor.class, String.format("Could not load the owner of the login cookie for user '%s'; "
                    + "the session login token renewal is kept for the next request", sessionUserId), e);
            return false;
        }
        if (null != cookieOwner && null != sessionUserId
                && cookieOwner.user().getUserId().equals(sessionUserId)) {
            try {
                // Set before the endpoint runs: a page rendered in live mode (a GraphQL GET, for
                // example) only sets its max-age when no Cache-Control header is present, so a
                // shared cache cannot keep this response and the new login cookie with it. The
                // request goes on with a NoStoreResponse, so later code cannot replace it.
                response.setHeader(HeaderFilter.CACHE_CONTROL, HeaderFilter.NO_CACHE_CONTROL);
                // The validated token's id, not the one on the loaded user, which a concurrent
                // logout or password change may have replaced since.
                this.loginServiceAPI.issueSessionLoginToken(request, response, cookieOwner.user(),
                        cookieOwner.tokenId());
            } catch (final RuntimeException e) {
                Logger.warnAndDebug(DefaultAutoLoginWebInterceptor.class, String.format("Could not renew the session login token for user '%s'; "
                        + "the renewal is kept for the next request", sessionUserId), e);
            }
            return true;
        }
        // The cookie cannot become valid again by itself, so the renewal is dropped rather than
        // retried on every request; the next login schedules a new one.
        session.removeAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
        Logger.info(this, () -> String.format("Session login token not renewed for user '%s': the request "
                + "carries %s", sessionUserId, null == loginCookie ? "no login cookie"
                : "a login cookie that is not valid for this user"));
        return false;
    }

    private static boolean isLoggedInAsAnotherUser(final HttpSession session) {

        return null != session.getAttribute(LoginAsSession.SESSION_KEY)
                || null != session.getAttribute(WebKeys.PRINCIPAL_USER_ID);
    }

    /**
     * The id of the user the session is logged in as. A back-end login keeps it under
     * {@link WebKeys#USER_ID}; a front-end user restored from the login cookie is only kept as the
     * CMS user.
     */
    private static String sessionUserId(final HttpSession session) {

        final Object userId = session.getAttribute(WebKeys.USER_ID);
        if (null != userId) {
            return userId.toString();
        }
        final Object cmsUser = session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
        return cmsUser instanceof User ? ((User) cmsUser).getUserId() : null;
    }

    /**
     * Whether the path is a dynamic back-end path on which a due login cookie is renewed. The
     * path is the one the request is dispatched on (servlet path plus path info), the same path
     * the interceptors are matched against, so path parameters such as {@code /api;v=1/} and the
     * context path do not change the outcome.
     *
     * @param path the request's dispatch path
     * @return {@code true} when a due renewal may run on this path
     */
    private static boolean isBackEndApplicationPath(final String path) {

        return null != path && BACK_END_APPLICATION_PREFIXES.stream().anyMatch(path::startsWith)
                && SCRIPTED_API_PREFIXES.stream().noneMatch(path::startsWith);
    }

    private static Set<String> caseInsensitiveSet(final String... values) {

        final Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(List.of(values));
        return set;
    }

    /**
     * A response that carries a renewed login cookie. It keeps the no-store Cache-Control header
     * set on it: later attempts to set Cache-Control, Expires or a CDN caching header (a rule that
     * sets a response header on a rendered page, or a template calling {@code $response.setHeader})
     * are ignored, so no shared cache keeps the response together with the cookie.
     */
    @VisibleForTesting
    static final class NoStoreResponse extends HttpServletResponseWrapper {

        NoStoreResponse(final HttpServletResponse response) {
            super(response);
        }

        /** Sets the header unless it could make the response cacheable. */
        @Override
        public void setHeader(final String name, final String value) {
            if (isWritable(name)) {
                super.setHeader(name, value);
            }
        }

        /** Adds the header unless it could make the response cacheable. */
        @Override
        public void addHeader(final String name, final String value) {
            if (isWritable(name)) {
                super.addHeader(name, value);
            }
        }

        /** Sets the date header unless it could make the response cacheable. */
        @Override
        public void setDateHeader(final String name, final long date) {
            if (isWritable(name)) {
                super.setDateHeader(name, date);
            }
        }

        /** Adds the date header unless it could make the response cacheable. */
        @Override
        public void addDateHeader(final String name, final long date) {
            if (isWritable(name)) {
                super.addDateHeader(name, date);
            }
        }

        /** Sets the integer header unless it could make the response cacheable. */
        @Override
        public void setIntHeader(final String name, final int value) {
            if (isWritable(name)) {
                super.setIntHeader(name, value);
            }
        }

        /** Adds the integer header unless it could make the response cacheable. */
        @Override
        public void addIntHeader(final String name, final int value) {
            if (isWritable(name)) {
                super.addIntHeader(name, value);
            }
        }

        private static boolean isWritable(final String name) {
            if (null != name && CACHING_HEADERS.contains(name)) {
                Logger.debug(DefaultAutoLoginWebInterceptor.class.getName(), () -> String.format(
                        "Ignored the '%s' header on a response that carries a renewed login cookie", name));
                return false;
            }
            return true;
        }
    }

} // E:O:F:DefaultAutoLoginWebInterceptor.
