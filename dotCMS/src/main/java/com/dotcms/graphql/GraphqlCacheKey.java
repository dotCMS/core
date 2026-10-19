package com.dotcms.graphql;

import com.dotcms.variant.VariantAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Derives the cache key under which {@link GraphqlCacheWebInterceptor} stores a GraphQL response.
 * <p>
 * The GraphQL servlet resolves who is asking and which site, language, variant, persona and page
 * mode apply from the request itself, so two requests that carry the same body can still produce
 * different responses. The key is therefore built on the server from every request input that
 * takes part in that resolution:
 * <ul>
 *     <li>the request body (the GraphQL query, variables and operation name)</li>
 *     <li>the caller: the {@code Authorization} and {@code DOTAUTH} headers, the remember-me
 *     cookie and the user logged into the session, or {@code anonymous} when none is present</li>
 *     <li>the server name the request was addressed to and every request parameter, which cover
 *     the {@code Host}, {@code language_id}, {@code variantName}, persona and {@code mode}
 *     selectors among others</li>
 *     <li>the session attributes the servlet reads: selected language, time machine date and
 *     language, selected site, page mode, variant and the visitor's persona</li>
 *     <li>the {@code dotcachekey} value, when the caller sends one, as an extra namespace</li>
 * </ul>
 * Credentials never make it into the key as-is: the whole canonical string is hashed with
 * SHA-256 and only the digest is used.
 */
final class GraphqlCacheKey {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String DOTAUTH_HEADER = "DOTAUTH";
    private static final String REQUESTED_WITH_HEADER = "X-Requested-With";
    private static final String TIME_MACHINE_DATE = "tm_date";
    private static final String TIME_MACHINE_LANGUAGE = "tm_lang";

    /**
     * Parameters that steer the cache itself and must not partition it.
     */
    private static final Set<String> CACHE_CONTROL_PARAMS = Set.of(
            GraphqlCacheWebInterceptor.GRAPHQL_CACHE_TTL_PARAM,
            GraphqlCacheWebInterceptor.GRAPHQL_CACHE_KEY_PARAM,
            GraphqlCacheWebInterceptor.GRAPHQL_CACHE_REFRESH_PARAM);

    /**
     * Session attributes, other than the logged-in user and the visitor, that change what the
     * servlet returns for an otherwise identical request.
     */
    private static final String[] SESSION_ATTRIBUTES = {
            WebKeys.HTMLPAGE_LANGUAGE,
            TIME_MACHINE_DATE,
            TIME_MACHINE_LANGUAGE,
            WebKeys.CMS_SELECTED_HOST_ID,
            WebKeys.PAGE_MODE_SESSION,
            VariantAPI.VARIANT_KEY
    };

    private final String value;
    private final boolean credentialed;
    private final boolean sessionScoped;

    private GraphqlCacheKey(final String value, final boolean credentialed,
            final boolean sessionScoped) {
        this.value = value;
        this.credentialed = credentialed;
        this.sessionScoped = sessionScoped;
    }

    /**
     * Builds the key for the given request.
     *
     * @param request   the incoming GraphQL request
     * @param body      the raw request body
     * @param clientKey the {@code dotcachekey} the caller sent, if any
     * @return the derived key
     */
    static GraphqlCacheKey from(final HttpServletRequest request, final String body,
            final Optional<String> clientKey) {

        final StringBuilder canonical = new StringBuilder(body.length() + 256);
        append(canonical, "body", body);
        append(canonical, "clientKey", clientKey.orElse(null));

        final String authorization = request.getHeader(AUTHORIZATION_HEADER);
        final String dotAuth = request.getHeader(DOTAUTH_HEADER);
        final String rememberMe = cookieValue(request, CookieKeys.JWT_ACCESS_TOKEN);
        final HttpSession session = request.getSession(false);
        final String sessionUserId = sessionUserId(session);

        final boolean credentialed = UtilMethods.isSet(authorization)
                || UtilMethods.isSet(dotAuth)
                || UtilMethods.isSet(rememberMe)
                || UtilMethods.isSet(sessionUserId);

        append(canonical, "authorization", authorization);
        append(canonical, "dotauth", dotAuth);
        append(canonical, "rememberMe", rememberMe);
        append(canonical, "sessionUser", sessionUserId);
        if (!credentialed) {
            append(canonical, "identity", "anonymous");
        }

        append(canonical, "serverName", request.getServerName());
        append(canonical, "requestedWith", request.getHeader(REQUESTED_WITH_HEADER));
        appendParameters(canonical, request);

        // Session parts are always appended, so a request with an empty session hashes the same
        // as one without a session.
        boolean sessionScoped = UtilMethods.isSet(rememberMe) || UtilMethods.isSet(sessionUserId);
        for (final String attribute : SESSION_ATTRIBUTES) {
            final String attributeValue = stringAttribute(session, attribute);
            sessionScoped |= attributeValue != null;
            append(canonical, "session." + attribute, attributeValue);
        }
        final String persona = visitorPersona(session);
        sessionScoped |= persona != null;
        append(canonical, "session.persona", persona);

        return new GraphqlCacheKey(DigestUtils.sha256Hex(canonical.toString()), credentialed,
                sessionScoped);
    }

    /**
     * The key to store and look up the response under.
     *
     * @return a SHA-256 hex digest
     */
    String value() {
        return value;
    }

    /**
     * Whether the request carries a credential (a header, the remember-me cookie or a logged-in
     * session). Anonymous requests share one entry per body and context.
     *
     * @return true when the caller presented a credential
     */
    boolean isCredentialed() {
        return credentialed;
    }

    /**
     * Whether part of the key came from the session or a cookie. Such requests cannot be replayed
     * in the background, because the copy used for a refresh carries neither.
     *
     * @return true when the key depends on session or cookie state
     */
    boolean isSessionScoped() {
        return sessionScoped;
    }

    private static void append(final StringBuilder canonical, final String name,
            final String value) {
        final String safeValue = value == null ? "" : value;
        canonical.append(name).append(':').append(safeValue.length()).append(':')
                .append(safeValue).append('\n');
    }

    private static void appendParameters(final StringBuilder canonical,
            final HttpServletRequest request) {
        final Map<String, String[]> parameters = request.getParameterMap();
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        final Map<String, String[]> sorted = new TreeMap<>(parameters);
        for (final Map.Entry<String, String[]> entry : sorted.entrySet()) {
            final String name = entry.getKey();
            if (name == null || CACHE_CONTROL_PARAMS.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            final String[] values = entry.getValue();
            append(canonical, "param." + name,
                    values == null ? null : String.join("\u0000", Arrays.asList(values)));
        }
    }

    private static String cookieValue(final HttpServletRequest request, final String name) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (final Cookie cookie : cookies) {
            if (cookie != null && name.equals(cookie.getName())
                    && UtilMethods.isSet(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static String sessionUserId(final HttpSession session) {
        if (session == null) {
            return null;
        }
        final Object user = session.getAttribute(com.liferay.portal.util.WebKeys.USER);
        if (user instanceof User) {
            return ((User) user).getUserId();
        }
        final Object userId = session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID);
        return userId instanceof String ? (String) userId : null;
    }

    private static String stringAttribute(final HttpSession session, final String name) {
        final Object attribute = session == null ? null : session.getAttribute(name);
        return attribute == null ? null : String.valueOf(attribute);
    }

    private static String visitorPersona(final HttpSession session) {
        final Object visitor = session == null ? null : session.getAttribute(WebKeys.VISITOR);
        if (visitor instanceof Visitor && ((Visitor) visitor).getPersona() != null) {
            return ((Visitor) visitor).getPersona().getKeyTag();
        }
        return null;
    }
}
