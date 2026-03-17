package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Utility class that handles the low-level mechanics of the {@link EventAnalyticsProxyResource}:
 * building upstream URLs, constructing the Basic-auth header, executing the HTTP call via
 * {@link CircuitBreakerUrl}, and mapping the upstream response into the standard dotCMS response
 * envelope.
 *
 * <p>Config properties consumed:
 * <ul>
 *   <li>{@link #DOT_CA_EVENT_MANAGER_BASE_URL} – base URL of the dot-ca-event-manager service</li>
 *   <li>{@link #DOT_CA_EVENT_MANAGER_CUSTOMER_ID} – customer ID for Basic auth</li>
 *   <li>{@link #DOT_CA_EVENT_MANAGER_PASSWORD} – password for Basic auth</li>
 *   <li>{@link #ANALYTICS_ENVIRONMENT} – environment name appended as {@code ?environment=} query param</li>
 * </ul>
 *
 * @author dotCMS
 * @since 2026
 */
public class EventAnalyticsProxyHelper {

    static final String DOT_CA_EVENT_MANAGER_BASE_URL = "DOT_CA_EVENT_MANAGER_BASE_URL";
    static final String DOT_CA_EVENT_MANAGER_CUSTOMER_ID = "DOT_CA_EVENT_MANAGER_CUSTOMER_ID";
    static final String DOT_CA_EVENT_MANAGER_PASSWORD = "DOT_CA_EVENT_MANAGER_PASSWORD";
    static final String ANALYTICS_ENVIRONMENT = "DOT_ANALYTICS_ENVIRONMENT";

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
     * @return dotCMS-wrapped {@link Response}
     */
    public static Response proxy(final String relativePath,
                                 final UriInfo uriInfo,
                                 final String body) {
        final String baseUrl = Config.getStringProperty(DOT_CA_EVENT_MANAGER_BASE_URL, "");
        if (!UtilMethods.isSet(baseUrl)) {
            Logger.error(EventAnalyticsProxyHelper.class,
                    "Configuration property '" + DOT_CA_EVENT_MANAGER_BASE_URL + "' is not set");
            return Response.serverError()
                    .entity(new ResponseEntityView<>(
                            List.of(new ErrorEntity("CONFIGURATION_ERROR",
                                    "Analytics event manager URL is not configured"))))
                    .build();
        }

        final String upstreamUrl = buildUpstreamUrl(baseUrl, relativePath, uriInfo);
        final String authHeader = buildBasicAuthHeader();
        final boolean isPost = UtilMethods.isSet(body);

        Logger.debug(EventAnalyticsProxyHelper.class,
                () -> "Proxying analytics " + (isPost ? "POST" : "GET") + " request to: " + upstreamUrl);

        try {
            final CircuitBreakerUrl.Response<String> cbResponse = CircuitBreakerUrl.builder()
                    .setUrl(upstreamUrl)
                    .setMethod(isPost ? CircuitBreakerUrl.Method.POST : CircuitBreakerUrl.Method.GET)
                    .setAuthHeaders(authHeader)
                    .setRawData(isPost ? body : null)
                    .setThrowWhenError(false)
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
        final String cleanBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        final String upstreamPath = "/v1/" + (relativePath != null ? relativePath : "");

        final StringBuilder queryString = new StringBuilder();
        uriInfo.getQueryParameters().forEach((key, values) ->
                values.forEach(value -> {
                    if (queryString.length() > 0) {
                        queryString.append("&");
                    }
                    queryString.append(key).append("=").append(value);
                })
        );

        final String analyticsEnvironment = Config.getStringProperty(ANALYTICS_ENVIRONMENT, "");
        if (UtilMethods.isSet(analyticsEnvironment)) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append("environment=").append(analyticsEnvironment);
        }

        return cleanBase + upstreamPath + (queryString.length() > 0 ? "?" + queryString : "");
    }

    /**
     * Constructs the {@code Authorization: Basic <base64>} header value using the customer ID and
     * password read from Config properties.
     *
     * @return full Authorization header value, e.g. {@code Basic dXNlcjpwYXNz}
     */
    static String buildBasicAuthHeader() {
        final String customerId = Config.getStringProperty(DOT_CA_EVENT_MANAGER_CUSTOMER_ID, "");
        final String password = Config.getStringProperty(DOT_CA_EVENT_MANAGER_PASSWORD, "");
        final byte[] credentials = (customerId + ":" + password).getBytes(StandardCharsets.UTF_8);
        return "Basic " + Base64.getEncoder().encodeToString(credentials);
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