package com.dotcms.experiments.business.result;

import com.dotcms.cube.AnalyticsResultSet;
import com.dotcms.cube.AnalyticsResultSetImpl;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * @param host          site context for per-site HMAC token lookup
     * @return {@link AnalyticsResultSet} populated from the CAEM response
     * @throws DotDataException if the response is non-2xx or the body cannot be parsed
     */
    public AnalyticsResultSet get(final String relativePath,
                                  final Map<String, String> queryParams,
                                  final Host host) throws DotDataException {
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
     */
    protected CaemResponse doGet(final String relativePath,
                                 final Map<String, String> queryParams,
                                 final Map<String, String> headers) {
        final String baseUrl = Config.getStringProperty(
                EventAnalyticsProxyHelper.DOT_ANALYTICS_BASE_URL, "");
        if (!UtilMethods.isSet(baseUrl)) {
            throw new RuntimeException("DOT_ANALYTICS_BASE_URL is not configured");
        }
        final String url = buildUrl(baseUrl, relativePath, queryParams);
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
            throw new RuntimeException("Failed to execute CAEM HTTP request for: " + url, e);
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
            final List<Map<String, Object>> data =
                    (List<Map<String, Object>>) parsed.get("data");
            if (!UtilMethods.isSet(data)) {
                return new AnalyticsResultSetImpl(Collections.emptyList());
            }
            final List<Map<String, Object>> mapped = new java.util.ArrayList<>();
            for (final Map<String, Object> row : data) {
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

    protected Map<String, String> buildHeaders(final Host host) {
        final Map<String, String> headers = new HashMap<>();
        ContentAnalyticsUtil.getBearerTokenFromAppSecrets(host)
                .ifPresent(token -> headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        return headers;
    }

    private String buildUrl(final String baseUrl,
                            final String relativePath,
                            final Map<String, String> queryParams) {
        final String clean = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        final StringBuilder sb = new StringBuilder(clean).append(relativePath);
        if (!queryParams.isEmpty()) {
            sb.append('?');
            queryParams.forEach((k, v) -> sb.append(k).append('=').append(v).append('&'));
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

}
