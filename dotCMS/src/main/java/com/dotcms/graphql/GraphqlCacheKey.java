package com.dotcms.graphql;

import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
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
import java.util.function.Supplier;

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
 *     <li>the session attributes the servlet reads: the selected language when it is not the
 *     default one, the time machine date, the selected site, the page mode and the visitor's
 *     persona</li>
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
    private static final String LANGUAGE_ID_PARAM = "language_id";

    /**
     * Parameters that steer the cache itself and must not partition it.
     */
    private static final Set<String> CACHE_CONTROL_PARAMS = Set.of(
            GraphqlCacheWebInterceptor.GRAPHQL_CACHE_TTL_PARAM,
            GraphqlCacheWebInterceptor.GRAPHQL_CACHE_KEY_PARAM,
            GraphqlCacheWebInterceptor.GRAPHQL_CACHE_REFRESH_PARAM);

    /**
     * Session attributes, other than the language, the logged-in user and the visitor, that change
     * what the servlet returns for an otherwise identical request. They are written by the back-end
     * UI, never by the GraphQL servlet itself, so they stay put for the life of a session.
     */
    private static final String[] SESSION_ATTRIBUTES = {
            TIME_MACHINE_DATE,
            WebKeys.CMS_SELECTED_HOST_ID,
            WebKeys.PAGE_MODE_SESSION
    };

    private final String value;
    private final boolean credentialed;
    private final boolean replayable;

    private GraphqlCacheKey(final String value, final boolean credentialed,
            final boolean replayable) {
        this.value = value;
        this.credentialed = credentialed;
        this.replayable = replayable;
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
        return from(request, body, clientKey,
                () -> APILocator.getLanguageAPI().getDefaultLanguage().getId());
    }

    /**
     * Builds the key for the given request.
     *
     * @param request           the incoming GraphQL request
     * @param body              the raw request body
     * @param clientKey         the {@code dotcachekey} the caller sent, if any
     * @param defaultLanguageId supplies the default language id, consulted only when the session
     *                          carries a language
     * @return the derived key
     */
    static GraphqlCacheKey from(final HttpServletRequest request, final String body,
            final Optional<String> clientKey, final Supplier<Long> defaultLanguageId) {

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
        append(canonical, "session.language", sessionLanguage(request, session, defaultLanguageId));
        for (final String attribute : SESSION_ATTRIBUTES) {
            append(canonical, "session." + attribute, stringAttribute(session, attribute));
        }
        append(canonical, "session.persona", visitorPersona(session));

        // A remember-me cookie logs the caller in through a filter the background replay never
        // runs, so the replay would run anonymously; once the session holds the user, it can be
        // copied along and the replay resolves the same caller.
        final boolean replayable = !UtilMethods.isSet(rememberMe) || UtilMethods.isSet(sessionUserId);

        return new GraphqlCacheKey(DigestUtils.sha256Hex(canonical.toString()), credentialed,
                replayable);
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
     * Whether a background refresh can replay the request as the same caller. The replay carries
     * the headers and a snapshot of the session, but no cookies, so a caller identified by the
     * remember-me cookie alone cannot be replayed.
     *
     * @return true when the request can be replayed in the background
     */
    boolean isReplayable() {
        return replayable;
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

    /**
     * The language the servlet would take from the session, or null when it does not take part in
     * the resolution: no session, a language parameter on the request (which always wins and is
     * already part of the key), or the default language. The servlet stores the language it
     * resolved back into the session on every request, so neither the parameter case nor the
     * default may count: otherwise the first request of a session (no attribute yet) and every
     * later one (attribute written back) would hash differently although they are served the
     * same way.
     *
     * @param request           the request
     * @param session           the session, if any
     * @param defaultLanguageId supplies the default language id
     * @return the session language id, or null when it does not change the response
     */
    private static String sessionLanguage(final HttpServletRequest request,
            final HttpSession session, final Supplier<Long> defaultLanguageId) {
        if (session == null || hasLanguageParameter(request)) {
            return null;
        }
        final String language = Optional.ofNullable(stringAttribute(session, TIME_MACHINE_LANGUAGE))
                .orElseGet(() -> stringAttribute(session, WebKeys.HTMLPAGE_LANGUAGE));
        if (language == null || language.equals(String.valueOf(defaultLanguageId.get()))) {
            return null;
        }
        return language;
    }

    private static boolean hasLanguageParameter(final HttpServletRequest request) {
        return UtilMethods.isSet(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE))
                || UtilMethods.isSet(request.getParameter(LANGUAGE_ID_PARAM));
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
