package com.dotcms.auth.providers.oauth.provider;

import com.dotcms.auth.providers.oauth.OAuthSsrfGuard;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Shared post-auth groups fetch for {@link OIDCProvider} and {@link GenericOAuth2Provider}.
 * <p>
 * Exists because some IdPs cannot emit group membership in token claims at all — Google
 * Workspace is the canonical case (its Cloud Identity / Admin Directory APIs must be called
 * after authentication), GitHub's teams API is the other. Those APIs need the user's identity
 * in the request URL, return arrays of objects rather than plain strings, and paginate.
 * <p>
 * Supported {@code groupsUrl} features on top of a plain GET-with-bearer-token:
 * <ul>
 *   <li>{@code {email}} / {@code {sub}} placeholders, substituted URL-encoded. {@code {sub}}
 *       is the verified subject claim; {@code {email}} is the address dotCMS resolved for the
 *       user (honoring the configured {@code emailClaim} and its fallbacks — the caller
 *       supplies it under the {@code email} key), not necessarily a literal {@code email}
 *       claim. A placeholder with no value aborts the fetch — never call the IdP with a
 *       literal placeholder.</li>
 *   <li>An optional response path such as {@code memberships[].groupKey.id} (Google Cloud
 *       Identity), {@code groups[].email} (Google Directory) or {@code [].slug} (GitHub):
 *       the segment before {@code []} locates the array ({@code []} alone means the response
 *       root), the segment after is read from each element. Without a path, the legacy shapes
 *       — a JSON array of strings, or an object with a {@code groups} string array — apply.</li>
 *   <li>{@code nextPageToken} pagination (Google convention), bounded by
 *       {@code OAUTH_GROUPS_MAX_PAGES} (default 20); exceeding the cap aborts the fetch
 *       rather than returning a partial list.</li>
 * </ul>
 * All failures propagate as {@link DotRuntimeException}: the caller must be able to tell
 * "endpoint down" from "user has no groups", otherwise an IdP outage would silently strip
 * the user's roles during the login role rebuild.
 * <p>
 * <b>Worked Google Workspace configuration</b> (user-token Cloud Identity approach):
 * <pre>
 * scopes             = openid email profile https://www.googleapis.com/auth/cloud-identity.groups.readonly
 * groupsUrl          = https://cloudidentity.googleapis.com/v1/groups/-/memberships:searchDirectGroups?query=member_key_id=='{email}'
 * groupsResponsePath = memberships[].groupKey.id
 * </pre>
 * Groups then arrive as their email addresses (e.g. {@code team@example.com}) for the
 * {@code groupMappings} step. Use {@code memberships:searchTransitiveGroups} instead of
 * {@code searchDirectGroups} to include nested membership. GitHub teams:
 * {@code groupsUrl = https://api.github.com/user/teams?per_page=100},
 * {@code groupsResponsePath = [].slug} — ceiling of 100 teams: GitHub paginates via the
 * {@code Link} header, which this fetcher does not follow, and 100 is the max page size.
 */
final class OAuthGroupsFetcher {

    private static final ObjectMapper MAPPER = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    /** Heap-bound guard capping IdP response bodies — shared by both providers. */
    static final int MAX_IDP_RESPONSE_BYTES =
            Config.getIntProperty("OAUTH_IDP_MAX_RESPONSE_BYTES", 1024 * 1024);

    private OAuthGroupsFetcher() {
    }

    static int maxPages() {
        return Config.getIntProperty("OAUTH_GROUPS_MAX_PAGES", 20);
    }

    /** Production entry point — GETs each page with the user's bearer token. */
    static Collection<String> fetch(final String groupsUrl,
                                    final String responsePath,
                                    final String accessToken,
                                    final Map<String, Object> userInfo,
                                    final String label) {
        return fetch(groupsUrl, responsePath, userInfo, label, url -> httpGet(url, accessToken, label));
    }

    /** Package-private for tests: {@code httpGet} maps a page URL to its response body. */
    static Collection<String> fetch(final String groupsUrl,
                                    final String responsePath,
                                    final Map<String, Object> userInfo,
                                    final String label,
                                    final Function<String, String> httpGet) {
        final String resolvedUrl = resolveUrl(groupsUrl, userInfo);
        final Collection<String> groups = new LinkedHashSet<>();
        String pageToken = null;
        final int cap = maxPages();
        for (int page = 0; page < cap; page++) {
            final String pageUrl = pageToken == null
                    ? resolvedUrl
                    : resolvedUrl + (resolvedUrl.contains("?") ? "&" : "?")
                            + "pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8);
            final String body = httpGet.apply(pageUrl);
            final Object parsed;
            try {
                parsed = MAPPER.readValue(body, Object.class);
            } catch (final Exception e) {
                throw new DotRuntimeException(label + " groups response is not valid JSON: " + e.getMessage(), e);
            }
            groups.addAll(extract(parsed, responsePath, label));
            pageToken = nextPageToken(parsed);
            if (!UtilMethods.isSet(pageToken)) {
                return groups;
            }
        }
        // A partial list would be applied by the role rebuild and silently strip the roles
        // for the missing pages — the exact failure this class exists to prevent. Fail the
        // login instead; the cap is configurable for tenants with pathological group counts.
        throw new DotRuntimeException(label + " groups fetch exceeded the " + cap
                + "-page cap (OAUTH_GROUPS_MAX_PAGES) with more pages pending — aborting login"
                + " rather than applying a partial group list");
    }

    /**
     * Substitute {@code {email}} / {@code {sub}} from the user's verified claims, URL-encoded.
     * A placeholder without a matching claim is a hard error — calling the IdP with a literal
     * placeholder (or an empty key) would return wrong-user or all-tenant data.
     */
    static String resolveUrl(final String urlTemplate, final Map<String, Object> userInfo) {
        String url = urlTemplate;
        for (final String claim : List.of("email", "sub")) {
            final String placeholder = "{" + claim + "}";
            if (!url.contains(placeholder)) {
                continue;
            }
            final Object value = userInfo == null ? null : userInfo.get(claim);
            if (value == null || !UtilMethods.isSet(String.valueOf(value))) {
                throw new DotRuntimeException("groupsUrl contains " + placeholder
                        + " but the authenticated user's claims do not include '" + claim + "'");
            }
            url = url.replace(placeholder, URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8));
        }
        return url;
    }

    private static Collection<String> extract(final Object parsed, final String responsePath, final String label) {
        if (!UtilMethods.isSet(responsePath)) {
            // Legacy shapes: a root array of strings, or {"groups": [strings]}.
            if (parsed instanceof List) {
                return toStringList(parsed);
            }
            if (parsed instanceof Map) {
                return toStringList(((Map<?, ?>) parsed).get("groups"));
            }
            return List.of();
        }
        final int marker = responsePath.indexOf("[]");
        if (marker < 0 || responsePath.indexOf("[]", marker + 2) >= 0) {
            throw new DotRuntimeException("groupsResponsePath '" + responsePath
                    + "' is invalid — expected exactly one '[]' array marker, e.g. memberships[].groupKey.id");
        }
        final String arrayPath = responsePath.substring(0, marker);
        final String elementPath = responsePath.substring(marker + 2).replaceFirst("^\\.", "");

        final Object arrayNode = navigate(parsed, arrayPath);
        if (arrayNode == null) {
            // e.g. Google omits "memberships" entirely for a user with no groups.
            return List.of();
        }
        if (!(arrayNode instanceof List)) {
            throw new DotRuntimeException(label + " groups response: path '" + arrayPath
                    + "' did not resolve to an array — check groupsResponsePath");
        }
        final Collection<String> out = new LinkedHashSet<>();
        for (final Object element : (List<?>) arrayNode) {
            final Object value = navigate(element, elementPath);
            if (value != null && !(value instanceof Map) && !(value instanceof List)) {
                out.add(String.valueOf(value));
            }
        }
        return out;
    }

    /** Walk a dot-separated path through nested maps; empty path returns the node itself. */
    private static Object navigate(final Object node, final String dotPath) {
        if (!UtilMethods.isSet(dotPath)) {
            return node;
        }
        Object current = node;
        for (final String segment : dotPath.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(segment);
        }
        return current;
    }

    private static String nextPageToken(final Object parsed) {
        if (!(parsed instanceof Map)) {
            return null;
        }
        final Object token = ((Map<?, ?>) parsed).get("nextPageToken");
        return token == null ? null : String.valueOf(token);
    }

    private static String httpGet(final String url, final String accessToken, final String label) {
        // Re-run the SSRF guard on the final, substituted URL. The configured template was
        // validated at save time; this covers the resolved form actually being fetched.
        final String rejection = OAuthSsrfGuard.validateUrl(url);
        if (rejection != null) {
            throw new DotRuntimeException(label + " groups URL rejected (SSRF guard): " + rejection);
        }
        final CircuitBreakerUrl.Response<String> resp;
        try {
            resp = CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setMethod(CircuitBreakerUrl.Method.GET)
                    .setHeaders(ImmutableMap.of(
                            "Authorization", "Bearer " + accessToken,
                            "Accept", "application/json"))
                    .setTimeout(5000)
                    .setMaxResponseBytes(MAX_IDP_RESPONSE_BYTES)
                    .build()
                    .doResponse();
        } catch (final Exception e) {
            throw new DotRuntimeException(label + " groups fetch failed: " + e.getMessage(), e);
        }
        if (resp == null) {
            // CircuitBreakerUrl.doResponse() maps transport failures (DNS, refused, timeout) to null
            throw new DotRuntimeException(label + " groups endpoint unreachable: " + url);
        }
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            throw new DotRuntimeException(label + " groups endpoint returned HTTP " + resp.getStatusCode());
        }
        return resp.getResponse();
    }

    static Collection<String> toStringList(final Object value) {
        if (value instanceof Collection<?> values) {
            return values.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        return List.of();
    }
}
