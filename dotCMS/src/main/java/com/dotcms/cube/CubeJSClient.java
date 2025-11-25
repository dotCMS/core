package com.dotcms.cube;

import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrl.Response;
import com.dotcms.metrics.timing.TimeMetric;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CubeJS Client it allow to send a Request to a Cube JS Server.
 * Example:
 *
 * <code>
 *
 * final String cubeServerIp = "127.0.0.1";
 * final int cubeJsServerPort = 5000;
 *
 * final CubeJSQuery cubeJSQuery = new Builder()
 *      .dimensions("Events.experiment", "Events.variant")
 *      .build();
 *
 * final CubeClient cubeClient =  new CubeClient(String.format("http://%s:%s", cubeServerIp, cubeJsServerPort));
 * final CubeJSResultSet cubeJSResultSet = cubeClient.send(cubeJSQuery);
 * </code>
 */
public class CubeJSClient {

    private int PAGE_SIZE = 1000;
    private final String url;
    private final AccessToken accessToken;

    public CubeJSClient(final String url, final AccessToken accessToken) {
        this.url = url;
        this.accessToken = accessToken;
    }

    /**
     * Send a request to a CubeJS Server.
     *
     * Example:
     *
     * <code>
     *
     * final String cubeServerIp = "127.0.0.1";
     * final int cubeJsServerPort = 5000;
     *
     * final CubeJSQuery cubeJSQuery = new Builder()
     *      .dimensions("Events.experiment", "Events.variant", "Events.utcTime")
     *      .build();
     *
     * final CubeClient cubeClient =  new CubeClient(String.format("http://%s:%s", cubeServerIp, cubeJsServerPort));
     * final CubeJSResultSet cubeJSResultSet = cubeClient.send(cubeJSQuery);
     *
     * for (ResultSetItem resultSetItem : cubeJSResultSet) {
     *      System.out.println("Events.experiment", resultSetItem.get("Events.experiment").get())
     *      System.out.println("Events.variant", resultSetItem.get("Events.variant").get())
     *      System.out.println("Events.utcTime", resultSetItem.get("Events.utcTime").get())
     * }
     * </code>
     *
     * @param query Query to be run in the CubeJS Server
     * @return
     */
    public CubeJSResultSet send(final CubeJSQuery query) {
        DotPreconditions.notNull(query, "Query not must be NULL");
        DotPreconditions.notNull(accessToken, "Access token not must be NULL");

        final String queryAsString = query.toString();
        return send(queryAsString);
    }

    public CubeJSResultSet send(final String queryAsString) {

        DotPreconditions.notNull(queryAsString, "Query not must be NULL");
        DotPreconditions.notNull(accessToken, "Access token not must be NULL");

        final CircuitBreakerUrl cubeJSClient;
        final String cubeJsUrl = String.format("%s/cubejs-api/v1/load", url);

        try {
            cubeJSClient = CircuitBreakerUrl.builder()
                    .setMethod(Method.GET)
                    .setHeaders(cubeJsHeaders(accessToken))
                    .setUrl(cubeJsUrl)
                    .setParams(new HashMap<>(Map.of("query", queryAsString)))
                    .setTimeout(4000)
                    .setThrowWhenNot2xx(false)
                    .build();
        } catch (AnalyticsException e) {
            throw new RuntimeException(e);
        }

        final Response<String> response = getStringResponse(cubeJSClient, cubeJsUrl, queryAsString);

        try {
            final String responseAsString = response.getResponse();
            final Map<String, Object> responseAsMap = UtilMethods.isSet(responseAsString) && !responseAsString.equals("[]") ?
                    JsonUtil.getJsonFromString(responseAsString) : new HashMap<>();
            final List<Map<String, Object>> data = (List<Map<String, Object>>) responseAsMap.get("data");

            return new CubeJSResultSetImpl(UtilMethods.isSet(data) ? data : Collections.emptyList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Response<String> getStringResponse(final CircuitBreakerUrl cubeJSClient,
                                               final String cubeJsUrl,
                                               final String queryAsString) {
            final TimeMetric timeMetric = TimeMetric.mark(getClass().getSimpleName());

        Logger.debug(this, String.format("Getting results from CubeJs [%s] with query [%s]", cubeJsUrl, queryAsString));
        final Response<String> response = cubeJSClient.doResponse();

        timeMetric.stop();

        if (!CircuitBreakerUrl.isWithin2xx(response.getStatusCode())) {

            if (400 == response.getStatusCode()) {
                throw new IllegalArgumentException(response.getResponse());
            }
            throw new RuntimeException("CubeJS Server is not available");
        }

        return response;
    }

    /**
     * Prepares access token request headers in a {@link Map} with values found in a {@link AccessToken} instance.
     *
     * @param accessToken access token
     * @return map representation of http headers
     */
    private Map<String, String> cubeJsHeaders(final AccessToken accessToken) throws AnalyticsException {
        return ImmutableMap.<String, String>builder()
            .put(HttpHeaders.AUTHORIZATION, AnalyticsHelper.get().formatBearer(accessToken))
            .build();
    }


}
