package com.dotcms.cube;

import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrl.Response;
import com.dotcms.metrics.timing.TimeMetric;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CubeJS Client it allows to send a Request to a Cube JS Server.
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
public class CubeJSClient implements EventSubscriber<SystemTableUpdatedKeyEvent> {

    private static final String CUBEJS_CLIENT_TIMEOUT_KEY = "CUBEJS_CLIENT_TIMEOUT";

    private final String url;
    private final AccessToken accessToken;
    private final AtomicLong cubeJsClientTimeout = new AtomicLong(resolveClientTimeout());

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
                    .setTimeout(cubeJsClientTimeout.get())
                    .setThrowWhenError(false)
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

    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {
        if (event.getKey().contains(CUBEJS_CLIENT_TIMEOUT_KEY)) {
            cubeJsClientTimeout.set(resolveClientTimeout());
        }
    }

    /**
     * Determines the connection timeout set for executing a CubeJS query. After that, an error will
     * be thrown. Keep in mind that a ClickHouse Database with thousands if not millions of records
     * may take a long time to retrieve results.
     * <p>
     * It's also important to note that this timeout should match the value of the
     * {@code continueWaitTimeout} configuration property in the {@code cube.js} file for
     * ClickHouse, which has a maximum value of {@code 90} -- in seconds. For more information, you
     * can refer to
     * <a href="https://cube.dev/docs/reference/configuration/config#orchestrator_options">The
     * official documentation</a>.</p>
     *
     * @return The connection timeout for executing queries, in milliseconds.
     */
    private long resolveClientTimeout() {
        return Config.getLongProperty(CUBEJS_CLIENT_TIMEOUT_KEY, 30000);
    }

    private Response<String> getStringResponse(final CircuitBreakerUrl cubeJSClient,
                                               final String cubeJsUrl,
                                               final String queryAsString) {
        final TimeMetric timeMetric = TimeMetric.mark(getClass().getSimpleName());

        Logger.debug(this, String.format("Getting results from CubeJs [%s] with query [%s]", cubeJsUrl, queryAsString));
        final Response<String> response = cubeJSClient.doResponse();

        timeMetric.stop();

        if (cubeJSClient.isError()) {
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
            .put(HttpHeaders.AUTHORIZATION, AnalyticsHelper.get().formatToken(accessToken, null))
            .build();
    }

}
