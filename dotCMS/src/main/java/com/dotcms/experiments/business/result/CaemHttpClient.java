package com.dotcms.experiments.business.result;

import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.cube.AnalyticsResultSetImpl;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import org.jetbrains.annotations.Nullable;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authenticated HTTP client for the CAEM analytics API.
 * <p>
 * Sends {@code GET} requests to the CAEM sessions endpoints, parses the JSON response
 * into an {@link AnalyticsResultSet} using the {@code Events.*} field naming convention
 * expected by the existing {@code ExperimentsAPIImpl} processing loops.
 * </p>
 * <p>
 * Authentication reuses the existing HMAC Bearer token mechanism via
 * {@link EventAnalyticsProxyHelper#buildAuthHeader(Host)}. The base URL is read from
 * {@code DOT_ANALYTICS_BASE_URL} — the same property used by the analytics proxy.
 * </p>
 */
public class CaemHttpClient {

    static final int CAEM_TIMEOUT_MS = 30_000;

    /**
     * Sends an authenticated GET request to the CAEM analytics API.
     *
     * @param relativePath  CAEM path, e.g. {@code /v1/analytics/sessions}
     * @param queryParams   query parameters to append
     * @param host          site context for per-site HMAC token lookup; {@code null} when the
     *                      current host is resolved from the request context by the caller
     * @return {@link AnalyticsResultSet} populated from the CAEM response
     * @throws DotDataException if no bearer token is found for the resolved host, the response is
     *                          non-2xx, or the body cannot be parsed
     */
    public AnalyticsResultSet get(final String relativePath,
                                  final Map<String, String> queryParams,
                                  @Nullable final Host host) throws DotDataException {
        final Map<String, String> headers = buildHeaders(host);

        Logger.debug(this, "CAEM query: GET " + relativePath);

        final CaemResponse response = doGet(relativePath, queryParams, headers);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new DotDataException(
                    "CAEM request failed with status " + response.statusCode()
                    + " for path: " + relativePath);
        }

        return parseResponse(response.body(), relativePath);
    }

    /** Simple response carrier — avoids exposing the private CircuitBreakerUrl.Response constructor in tests. */
    public record CaemResponse(int statusCode, String body) {}

    /**
     * Executes the HTTP GET. Overridable for testing — subclass overrides this to return a stub
     * response without making a real HTTP call or requiring DOT_ANALYTICS_BASE_URL to be set.
     *
     * @throws DotDataException if CAEM is misconfigured or the HTTP request fails
     */
    protected CaemResponse doGet(final String relativePath,
                                 final Map<String, String> queryParams,
                                 final Map<String, String> headers) throws DotDataException {
        final String baseUrl = Config.getStringProperty(
                EventAnalyticsProxyHelper.DOT_ANALYTICS_BASE_URL, "");
        final String tenant  = Config.getStringProperty(
                EventAnalyticsProxyHelper.DOT_ANALYTICS_TENANT, "");
        final String project = Config.getStringProperty(
                EventAnalyticsProxyHelper.DOT_ANALYTICS_PROJECT, "");

        if (!UtilMethods.isSet(baseUrl)) {
            throw new DotDataException("CAEM is not configured: DOT_ANALYTICS_BASE_URL is missing");
        }
        if (!UtilMethods.isSet(tenant)) {
            throw new DotDataException("CAEM is not configured: DOT_ANALYTICS_TENANT is missing");
        }
        if (!UtilMethods.isSet(project)) {
            throw new DotDataException("CAEM is not configured: DOT_ANALYTICS_PROJECT is missing");
        }

        final String url = buildUrl(baseUrl, relativePath, queryParams, project);
        try {
            final CircuitBreakerUrl.Response<String> raw = CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setMethod(CircuitBreakerUrl.Method.GET)
                    .setHeaders(headers)
                    .setThrowWhenError(false)
                    .setTimeout(CAEM_TIMEOUT_MS)
                    .build()
                    .doResponse();
            return new CaemResponse(raw.getStatusCode(), raw.getResponse());
        } catch (final Exception e) {
            throw new DotDataException("Failed to execute CAEM HTTP request for: " + url, e);
        }
    }

    @SuppressWarnings("unchecked")
    private AnalyticsResultSet parseResponse(final String json,
                                             final String path) throws DotDataException {
        if (!UtilMethods.isSet(json)) {
            return new AnalyticsResultSetImpl(Collections.emptyList());
        }
        try {
            final Map<String, Object> parsed = JsonUtil.getJsonFromString(json);
            final List<Map<String, Object>> rows =
                    (List<Map<String, Object>>) parsed.get("rows");
            if (!UtilMethods.isSet(rows)) {
                return new AnalyticsResultSetImpl(Collections.emptyList());
            }
            final List<Map<String, Object>> mapped = new java.util.ArrayList<>();
            for (final Map<String, Object> row : rows) {
                mapped.add(mapCaemFieldsToEventFields(row));
            }
            return new AnalyticsResultSetImpl(mapped);
        } catch (final IOException e) {
            throw new DotDataException(
                    "Failed to parse CAEM response for path " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Translates CAEM response field names to the {@code Events.*} convention read by
     * {@code ExperimentsAPIImpl}'s processing loops.
     */
    private Map<String, Object> mapCaemFieldsToEventFields(final Map<String, Object> caemRow) {
        final Map<String, Object> mapped = new HashMap<>();
        caemRow.forEach((key, value) -> {
            final String mappedKey = toCaemEventFieldName(key);
            mapped.put(mappedKey, value);
        });
        return mapped;
    }

    private String toCaemEventFieldName(final String caemField) {
        switch (caemField) {
            case "variant":          return "Events.variant";
            case "day":              return "Events.day";
            case "totalSessions":    return "Events.totalSessions";
            // Bounce rate fields
            case "bounceSessions":   return "Events.bounceRateSuccesses";
            case "bounceRate":       return "Events.bounceRateConversionRate";
            // Exit rate fields
            case "exitSessions":     return "Events.exitRateSuccesses";
            case "exitRate":         return "Events.exitRateConversionRate";
            // Reach-target / URL-parameter fields
            case "successSessions":  return "Events.reachPageRateSuccesses";
            case "successRate":      return "Events.reachPageRateConversionRate";
            default:                 return "Events." + caemField;
        }
    }

    protected Map<String, String> buildHeaders(@Nullable final Host host) throws DotDataException {
        final Host resolvedHost = host != null ? host : resolveCurrentHost();
        // Delegates to buildAuthHeader so both token sources are tried in order:
        // (1) per-site app secrets, (2) DOT_ANALYTICS_BEARER_TOKEN global property.
        final String authHeader = EventAnalyticsProxyHelper.buildAuthHeader(resolvedHost)
                .orElseThrow(() -> new DotDataException(
                        "CAEM authentication failed: no bearer token found for host '"
                        + (resolvedHost != null ? resolvedHost.getHostname() : "unresolved")
                        + "'. Configure the Analytics app or set DOT_ANALYTICS_BEARER_TOKEN."));
        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, authHeader);
        return headers;
    }

    private static Host resolveCurrentHost() {
        try {
            final javax.servlet.http.HttpServletRequest request =
                    HttpServletRequestThreadLocal.INSTANCE.getRequest();
            if (request != null) {
                return WebAPILocator.getHostWebAPI().getCurrentHost(request);
            }
        } catch (final Exception e) {
            Logger.debug(CaemHttpClient.class,
                    "Could not resolve current host from request context: " + e.getMessage());
        }
        return null;
    }

    private String buildUrl(final String baseUrl,
                            final String relativePath,
                            final Map<String, String> queryParams,
                            final String project) {
        final String clean = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        final StringBuilder sb = new StringBuilder(clean).append(relativePath);

        final Map<String, String> allParams = new LinkedHashMap<>(queryParams);
        if (!allParams.containsKey("project")) {
            allParams.put("project", project);
        }

        sb.append('?');
        allParams.forEach((k, v) ->
                sb.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8)).append('&'));
        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

}
