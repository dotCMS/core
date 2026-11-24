package com.dotcms.filters.interceptor.meta;

import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;
import javax.servlet.http.HttpServletRequest;

/**
 * Decides whether a request is sent the node response header
 * ({@link ResponseMetaDataWebInterceptor}).
 *
 * <p>They go to requests that carry valid credentials: a logged-in session, or a bearer token
 * (a dotCMS JWT) that resolves to an active user. Requests without credentials, or with ones that do not check
 * out, get no such headers unless
 * {@link FeatureFlagName#FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS} is on.
 *
 * <p>The check runs inside the interceptor chain, before the request's user is otherwise
 * resolved, so it looks the user up itself and does not store that user on the request. The
 * answer is remembered in a private request attribute so the interceptors that ask, one after the
 * other, verify a bearer token once per request. For a request with no session and no
 * {@code Authorization} header the check costs two null checks.
 */
final class ResponseMetaHeaderAudience {

    /** Request attribute holding this request's {@link #includes} answer, as a {@link Boolean}. */
    static final String DECISION_ATTRIBUTE = "dotcms.responseMetaHeaderAudience";

    private static final String BEARER = "Bearer ";

    private ResponseMetaHeaderAudience() {
        // static helper, no instances
    }

    /**
     * Tells whether the node header should be added to the response of this request.
     * The answer is computed on the first call for a request and reused afterwards.
     *
     * @param request the incoming request
     * @return {@code true} if the anonymous flag is on or the request carries valid credentials
     */
    static boolean includes(final HttpServletRequest request) {
        final Object remembered = request.getAttribute(DECISION_ATTRIBUTE);
        if (remembered instanceof Boolean) {
            return (Boolean) remembered;
        }

        final boolean included = Config.getBooleanProperty(
                        FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, false)
                || hasValidCredentials(request);
        request.setAttribute(DECISION_ATTRIBUTE, included);
        return included;
    }

    private static boolean hasValidCredentials(final HttpServletRequest request) {
        return Try.of(() -> isLoggedIn(PortalUtil.getUser(request.getSession(false)))
                        || isLoggedIn(bearerUser(request)))
                .getOrElse(false);
    }

    /**
     * Resolves the user a bearer token belongs to: a dotCMS JWT checked against the request's remote
     * address, so a token limited to a network is accepted from inside it. Unlike the credential
     * processors, it leaves the request attributes as they were.
     *
     * @param request the incoming request
     * @return the token's user, or {@code null} when there is no bearer token
     * @throws Exception when the token does not resolve; the caller treats that as no credentials
     */
    private static User bearerUser(final HttpServletRequest request) throws Exception {
        final String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            return null;
        }
        final String token = header.substring(BEARER.length()).trim();
        return JsonWebTokenFactory.getInstance().getJsonWebTokenService()
                .parseToken(token, request.getRemoteAddr()).getActiveUser().orElse(null);
    }

    private static boolean isLoggedIn(final User user) {
        return user != null && !user.isAnonymousUser() && user.isActive();
    }

}
