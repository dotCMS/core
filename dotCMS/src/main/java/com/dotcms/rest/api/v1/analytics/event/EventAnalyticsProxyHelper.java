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

    /** Relative paths allowed by the catch-all proxy. Anything outside this list — including
     *  attempts to escape via {@code ..} segments — is rejected with 400. */
    private static final List<String> ALLOWED_PATH_PREFIXES = List.of("event");

    private EventAnalyticsProxyHelper() {
        // utility class — no instances
    }

    /**
     * Proxies the request to the dot-ca-event-manager service. When {@code body} is non-null the
     * upstream call is made as POST (with the body); otherwise a GET is issued.
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
     * @return dotCMS-wrapped {@link Response}
     */
    public static Response proxy(final String relativePath,
                                 final UriInfo uriInfo,
                                 final String body,
                                 final String userAgent) {
        return proxy(relativePath, uriInfo, body, userAgent, null);
    }

    /**
     * Proxies the request to the dot-ca-event-manager service, using per-site
     * bearer token from app secrets when available.
     *
     * @param relativePath path relative to {@code /v1/} on the upstream service
     * @param uriInfo      original URI info used to forward query parameters
     * @param body         optional JSON body; triggers a POST when non-null
     * @param userAgent    {@code User-Agent} header value from the original request
     * @param host         site context for per-site auth lookup; may be {@code null}
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
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
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

        final String analyticsProject = Config.getStringProperty(DOT_ANALYTICS_PROJECT, BLANK);
        if (UtilMethods.isSet(analyticsProject)) {
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
                final String trimmed = siteToken.get().trim();
                if (!trimmed.isEmpty()) {
                    return Optional.of("Bearer " + trimmed);
                }
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
     *   <li>paths containing {@code ..} segments (after URL decoding by JAX-RS) — these
     *       would let an authenticated backend user escape the {@code /v1/event/} prefix
     *       and probe administrative endpoints on the upstream</li>
     *   <li>paths that don't start with an allowed prefix</li>
     * </ul>
     */
    static boolean isAllowedRelativePath(final String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        if (relativePath.contains("..") || relativePath.contains("\\")) {
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

        try {
            final Object parsedBody = UtilMethods.isSet(cbResponse.getResponse()) ? DotObjectMapperProvider.getInstance()
                    .getDefaultObjectMapper()
                    .readValue(cbResponse.getResponse(), Object.class) : "";

            return Response.status(statusCode)
                    .entity(new ResponseEntityView<>(parsedBody))
                    .build();

        } catch (final Exception e) {
            Logger.warn(EventAnalyticsProxyHelper.class,
                    "Upstream response is not valid JSON (status=" + statusCode + "), forwarding as-is");
            return Response.status(statusCode)
                    .entity(new ResponseEntityView<>(cbResponse.getResponse()))
                    .build();
        }
    }

}