package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.apache.http.HttpStatus;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liferay.util.StringPool.BLANK;

/**
 * Utility class that handles the low-level mechanics of the {@link EventAnalyticsProxyResource}:
 * building upstream URLs, constructing the HMAC bearer Authorization header from per-site app
 * secrets, executing the HTTP call via {@link CircuitBreakerUrl}, and mapping the upstream
 * response into the standard dotCMS response envelope.
 *
 * <p>Config properties consumed:
 * <ul>
 *   <li>{@link #DOT_ANALYTICS_BASE_URL} – base URL of the dot-ca-event-manager service</li>
 *   <li>{@link #DOT_ANALYTICS_TENANT} – tenant (customer) identifier used by the admin token
 *       exchange flow; not consumed at request-time auth</li>
 *   <li>{@link #DOT_ANALYTICS_PROJECT} – environment name appended as {@code ?project=} query param</li>
 *   <li>{@link #DOT_ANALYTICS_BEARER_TOKEN} – optional global bearer token used as a bootstrap
 *       fallback when no per-site app secret is configured</li>
 * </ul>
 *
 * @author dotCMS
 * @since 2026
 */
public class EventAnalyticsProxyHelper {

    public static final String DOT_ANALYTICS_BASE_URL = "DOT_ANALYTICS_BASE_URL";
    public static final String DOT_ANALYTICS_TENANT = "DOT_ANALYTICS_TENANT";
    public static final String DOT_ANALYTICS_PROJECT = "DOT_ANALYTICS_PROJECT";
    public static final String DOT_ANALYTICS_BEARER_TOKEN = "DOT_ANALYTICS_BEARER_TOKEN";

    /** Hard upstream-call timeout. Prevents a slow event manager from exhausting the
     *  Jersey thread pool and degrading the rest of the admin portal. */
    static final int PROXY_TIMEOUT_MS = 10_000;

    /** Relative paths allowed by the catch-all proxy. Each entry matches as an exact
     *  segment or as a prefix followed by {@code "/"}. The dot-ca-event-manager exposes
     *  more than just event endpoints — analytics dashboards also fetch from
     *  {@code conversion/*}, {@code session/*}, and {@code health}. Anything outside this
     *  list — including upstream admin endpoints like {@code admin/token} or
     *  {@code tenants/*} — is rejected with 400. */
    private static final List<String> ALLOWED_PATH_PREFIXES =
            List.of("event", "conversion", "session", "health");

    private EventAnalyticsProxyHelper() {
        // utility class — no instances
    }

    /**
     * Proxies the request to the dot-ca-event-manager service. When {@code body} is non-null the
     * upstream call is made as POST (with the body); otherwise a GET is issued. The
     * Authorization header is sourced from the per-site bearer token on {@code host} when
     * available, with the global {@link #DOT_ANALYTICS_BEARER_TOKEN} property as fallback.
     *
     * <p>The upstream path is built by prepending {@code /v1/} to {@code relativePath} and
     * stripping any trailing slash from the configured base URL, e.g.:
     * <pre>
     *   relativePath = "event/total-events"
     *   → upstream URL = {DOT_CA_EVENT_MANAGER_BASE_URL}/v1/event/total-events[?queryString]
     * </pre>
     *
     * @param relativePath path relative to {@code /v1/} on the upstream service
     * @param uriInfo      original URI info used to forward query parameters
     * @param body         optional JSON body; triggers a POST when non-null
     * @param userAgent    {@code User-Agent} header value from the original request; forwarded as-is
     * @param host         site context for per-site auth lookup; may be {@code null} for
     *                     contexts without a resolved site (CI, smoke tests)
     * @return dotCMS-wrapped {@link Response}
     */
    public static Response proxy(final String relativePath,
                                 final UriInfo uriInfo,
                                 final String body,
                                 final String userAgent,
                                 final Host host) {
        final String baseUrl = Config.getStringProperty(DOT_ANALYTICS_BASE_URL, "");
        if (!UtilMethods.isSet(baseUrl)) {
            Logger.error(EventAnalyticsProxyHelper.class,
                    "Configuration property '" + DOT_ANALYTICS_BASE_URL + "' is not set");
            return Response.serverError()
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity("CONFIGURATION_ERROR",
                                    "Analytics event manager URL is not configured"))))
                    .build();
        }

        if (!isAllowedRelativePath(relativePath)) {
            Logger.warn(EventAnalyticsProxyHelper.class,
                    "Rejected analytics proxy path: '" + relativePath + "'");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity("INVALID_PATH",
                                    "Path is not allowed by the analytics proxy"))))
                    .build();
        }

        final String upstreamUrl = buildUpstreamUrl(baseUrl, relativePath, uriInfo);
        final Optional<String> authHeader = buildAuthHeader(host);
        final boolean isPost = UtilMethods.isSet(body);

        final Map<String, String> requestHeaders = new HashMap<>();
        authHeader.ifPresent(h -> requestHeaders.put(HttpHeaders.AUTHORIZATION, h));
        requestHeaders.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        // Content-Type only applies to messages with a body — GETs without one violate RFC 9110 §8.3.
        if (isPost) {
            requestHeaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        }
        if (UtilMethods.isSet(userAgent)) {
            requestHeaders.put(HttpHeaders.USER_AGENT, userAgent);
        }

        Logger.debug(EventAnalyticsProxyHelper.class,
                () -> "Proxying analytics " + (isPost ? "POST" : "GET") + " request to: " + upstreamUrl);

        try {
            final CircuitBreakerUrl.Response<String> cbResponse = CircuitBreakerUrl.builder()
                    .setUrl(upstreamUrl)
                    .setMethod(isPost ? CircuitBreakerUrl.Method.POST : CircuitBreakerUrl.Method.GET)
                    .setHeaders(requestHeaders)
                    .setRawData(isPost ? body : null)
                    .setThrowWhenError(false)
                    .setTimeout(PROXY_TIMEOUT_MS)
                    .build()
                    .doResponse();

            return wrapUpstreamResponse(cbResponse);
        } catch (final Exception e) {
            Logger.error(EventAnalyticsProxyHelper.class,
                    "Error proxying analytics request to '" + upstreamUrl + "': " + e.getMessage(), e);
            return Response.serverError()
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity("PROXY_ERROR",
                                    "Failed to forward request to analytics service"))))
                    .build();
        }
    }

    /**
     * Removes a trailing slash from {@code url} if one is present, so that appending a path
     * segment never produces a double slash (e.g. {@code http://host//v1/health}).
     *
     * @param url the URL to normalize; must not be {@code null}
     * @return the URL without a trailing slash
     */
    private static String stripTrailingSlash(final String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Builds the full upstream URL from the base URL, the relative path, and any query parameters
     * extracted from the original request.
     *
     * @param baseUrl      configured base URL (trailing slash is stripped if present)
     * @param relativePath path relative to {@code /v1/} (e.g. {@code event/total-events})
     * @param uriInfo      original URI info carrying the query parameters to forward
     * @return fully-formed upstream URL string
     */
    static String buildUpstreamUrl(final String baseUrl,
                                   final String relativePath,
                                   final UriInfo uriInfo) {
        final String cleanBase = stripTrailingSlash(baseUrl);
        final String upstreamPath = "/v1/" + (relativePath != null ? relativePath : "");

        final StringBuilder queryString = new StringBuilder();
        uriInfo.getQueryParameters().forEach((key, values) ->
                values.forEach(value -> {
                    if (queryString.length() > 0) {
                        queryString.append("&");
                    }
                    queryString.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                            .append("=")
                            .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                })
        );

        // Append the configured project ONLY if the request didn't already include one.
        // Otherwise upstream sees two project= parameters and decoding behavior is undefined.
        final boolean projectAlreadyPresent = uriInfo.getQueryParameters().containsKey("project");
        final String analyticsProject = Config.getStringProperty(DOT_ANALYTICS_PROJECT, BLANK);
        if (!projectAlreadyPresent && UtilMethods.isSet(analyticsProject)) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append("project=").append(URLEncoder.encode(analyticsProject, StandardCharsets.UTF_8));
        }

        return cleanBase + upstreamPath + (queryString.length() > 0 ? "?" + queryString : "");
    }

    /**
     * Performs a lightweight health check against the CA Event Manager by issuing an unauthenticated
     * GET to {@code {DOT_ANALYTICS_BASE_URL}/v1/health}. The endpoint is expected to execute a
     * minimal ClickHouse query and return a non-empty JSON body (e.g. {@code {"clickhouse":"up"}})
     * on success.
     *
     * <p>Returns {@code false} immediately — without making any HTTP call — when
     * {@link #DOT_ANALYTICS_BASE_URL} is not configured.
     *
     * @return {@code true} if the CA Event Manager responds with a 2xx status and a non-empty
     *         body; {@code false} if the base URL is missing, the service is unreachable, or the
     *         response indicates an error
     */
    public static boolean healthCheck() {
        final String baseUrl = Config.getStringProperty(DOT_ANALYTICS_BASE_URL, BLANK);
        if (UtilMethods.isNotSet(baseUrl)) {
            Logger.debug(EventAnalyticsProxyHelper.class,
                    "Health check skipped: '" + DOT_ANALYTICS_BASE_URL + "' is not configured");
            return false;
        }

        final String healthUrl = stripTrailingSlash(baseUrl) + "/v1/health";

        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        try {
            final CircuitBreakerUrl.Response<String> cbResponse = CircuitBreakerUrl.builder()
                    .setUrl(healthUrl)
                    .setMethod(CircuitBreakerUrl.Method.GET)
                    .setHeaders(headers)
                    .setThrowWhenError(false)
                    // Aggressive timeout — health endpoints must fail fast or they'll
                    // exhaust the monitor's calling thread under upstream degradation.
                    .setTimeout(2_000)
                    .build()
                    .doResponse();

            return cbResponse != null
                    && cbResponse.getStatusCode() >= HttpStatus.SC_OK
                    && cbResponse.getStatusCode() < HttpStatus.SC_MULTIPLE_CHOICES
                    && UtilMethods.isSet(cbResponse.getResponse());
        } catch (final Exception e) {
            Logger.error(EventAnalyticsProxyHelper.class,
                    "CA Event Manager health check failed: " + ExceptionUtil.getErrorMessage(e), e);
            return false;
        }
    }

    /**
     * Builds the {@code Authorization} header for an upstream request. HMAC is the only
     * supported auth scheme; the token is sourced as a hidden secret on the Content
     * Analytics App for the given site, with the global {@link #DOT_ANALYTICS_BEARER_TOKEN}
     * property kept as a bootstrap fallback for non-site-bound contexts (CI, smoke tests).
     *
     * <p>Returns {@link Optional#empty()} when neither source yields a token — callers must
     * then omit the {@code Authorization} header rather than send a malformed {@code Bearer }
     * (no token) value, which violates RFC 6750 and is rejected by upstream gateways.
     *
     * @param host site context for per-site HMAC token lookup; may be {@code null}
     * @return {@code Bearer <token>} header value, or empty if no token is configured
     */
    static Optional<String> buildAuthHeader(final Host host) {
        if (host != null) {
            final Optional<String> siteToken =
                    ContentAnalyticsUtil.getBearerTokenFromAppSecrets(host);
            if (siteToken.isPresent() && UtilMethods.isSet(siteToken.get())) {
                return Optional.of("Bearer " + siteToken.get());
            }
        }
        final String fallback = Config.getStringProperty(DOT_ANALYTICS_BEARER_TOKEN, "").trim();
        if (UtilMethods.isSet(fallback)) {
            return Optional.of("Bearer " + fallback);
        }
        Logger.warn(EventAnalyticsProxyHelper.class,
                "No HMAC token available — set one via the Content Analytics App save flow"
                        + " or as a bootstrap " + DOT_ANALYTICS_BEARER_TOKEN);
        return Optional.empty();
    }

    /**
     * Whitelists the catch-all proxy to the {@code event/...} surface area on the upstream
     * event manager. Rejects:
     * <ul>
     *   <li>null / empty / blank paths</li>
     *   <li>backslash characters (no valid URL path contains them; defense-in-depth
     *       against alternate path separators)</li>
     *   <li>{@code ..} or {@code .} as exact path segments — would let an authenticated
     *       backend user escape the {@code /v1/event/} prefix and probe administrative
     *       endpoints on the upstream</li>
     *   <li>paths that don't start with an allowed prefix</li>
     * </ul>
     */
    static boolean isAllowedRelativePath(final String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        if (relativePath.contains("\\")) {
            return false;
        }
        // Segment-based traversal check: only reject segments that equal ".." or ".".
        // contains("..") was over-broad — a future endpoint like "event/last..7..days"
        // has no traversal intent but would have been wrongly rejected.
        for (final String segment : relativePath.split("/")) {
            if ("..".equals(segment) || ".".equals(segment)) {
                return false;
            }
        }
        // JAX-RS URL-decodes %3F / %23 in @PathParam values, so an attacker can smuggle
        // a literal '?' or '#' inside an otherwise valid prefix (e.g. event/foo?bar).
        // That would produce a malformed upstream URL (double-?), or strip our query
        // params entirely (#). Reject both.
        if (relativePath.indexOf('?') >= 0 || relativePath.indexOf('#') >= 0) {
            return false;
        }
        for (final String prefix : ALLOWED_PATH_PREFIXES) {
            if (relativePath.equals(prefix) || relativePath.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Maps a raw upstream {@link CircuitBreakerUrl.Response} into the standard dotCMS response
     * envelope:
     * <ul>
     *   <li>Success: upstream {@code data} → {@code entity}</li>
     *   <li>Error: upstream {@code error.code} / {@code error.message} → {@code errors[]}</li>
     * </ul>
     *
     * @param cbResponse raw response from the upstream service
     * @return dotCMS-wrapped {@link Response}
     */
    static Response wrapUpstreamResponse(final CircuitBreakerUrl.Response<String> cbResponse) {
        if (cbResponse == null ) {
            Logger.warn(EventAnalyticsProxyHelper.class,
                    "Received null or empty response from analytics service");
            return Response.serverError()
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity("PROXY_ERROR",
                                    "No response from analytics service"))))
                    .build();
        }

        final int statusCode = cbResponse.getStatusCode();
        final int clientStatus = clientFacingStatus(statusCode);

        try {
            final Object parsedBody = UtilMethods.isSet(cbResponse.getResponse()) ? DotObjectMapperProvider.getInstance()
                    .getDefaultObjectMapper()
                    .readValue(cbResponse.getResponse(), Object.class) : "";

            return Response.status(clientStatus)
                    .entity(new ResponseEntityView<>(parsedBody))
                    .build();

        } catch (final Exception e) {
            Logger.warn(EventAnalyticsProxyHelper.class,
                    "Upstream response is not valid JSON (status=" + statusCode + "), forwarding as-is");
            return Response.status(clientStatus)
                    .entity(new ResponseEntityView<>(cbResponse.getResponse()))
                    .build();
        }
    }

    /**
     * Translates the upstream event manager's HTTP status into the status the proxy returns
     * to the dotCMS client. Identity in every case EXCEPT upstream 401/403, which become
     * 502 Bad Gateway.
     *
     * <p>Rationale: an upstream 401/403 here means "dotCMS failed to authenticate to the
     * event manager on this user's behalf" (stale bearer token, rotated upstream secret,
     * unconfigured admin creds, etc.) — NOT "this user's dotCMS session is invalid." But
     * the dotCMS UI installs a global HTTP interceptor that treats any 401 from a fetch()
     * as session-expired and triggers {@code /dotAdmin/logout}. The dashboard fans out
     * ~6 analytics queries in parallel, so a single upstream auth glitch produces a burst
     * of 401s and immediately logs the admin out — a bad UX for what's actually a service
     * configuration problem.
     *
     * <p>502 Bad Gateway is the honest mapping: dotCMS reached the upstream and got an
     * error response. The upstream's response body is still propagated in the envelope so
     * the dashboard widget can surface a meaningful error state without nuking the session.
     */
    private static int clientFacingStatus(final int upstreamStatus) {
        return (upstreamStatus == Response.Status.UNAUTHORIZED.getStatusCode()
                || upstreamStatus == Response.Status.FORBIDDEN.getStatusCode())
                ? Response.Status.BAD_GATEWAY.getStatusCode()
                : upstreamStatus;
    }

}