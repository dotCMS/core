package com.dotcms.cube;

import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrl.Response;
import com.dotcms.metrics.timing.TimeMetric;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.liferay.util.StringPool.BLANK;

/**
 * This CubeJS Client allows you to send a Request to a Cube JS Server. The request is composed of a
 * CubeJS query that will be used to extract information from ClickHouse. Here's an example of how
 * this can be accomplished in Java:
 *
 * <pre>
 * {@code
 * final String cubeServerIp = "127.0.0.1";
 * final int cubeJsServerPort = 5000;
 *
 * final CubeJSQuery cubeJSQuery = new Builder()
 *      .dimensions("Events.experiment", "Events.variant")
 *      .build();
 *
 * final CubeClient cubeClient =  new CubeClient(String.format("http://%s:%s", cubeServerIp, cubeJsServerPort));
 * final CubeJSResultSet cubeJSResultSet = cubeClient.send(cubeJSQuery);
 * }
 * </pre>
 *
 * @author Freddy Rodriguez
 * @since Jan 27th, 2023
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
     * Sends a request to a CubeJS Server. Example:
     *
     * <pre>
     * {@code
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
     * }
     * </pre>
     *
     * @param query Query to be run in the CubeJS Server
     *
     * @return A {@link CubeJSResultSet} object containing the results of the query.
     */
    public CubeJSResultSet send(final CubeJSQuery query) {
        DotPreconditions.notNull(query, "Query not must be NULL");
        DotPreconditions.notNull(accessToken, "Access token not must be NULL");

        final String queryAsString = query.toString();
        return send(queryAsString);
    }

    /**
     * Sends the CubeJS query to the CubeJS server.
     *
     * @param queryAsString The query as a String.
     *
     * @return A {@link CubeJSResultSet} object containing the results of the query.
     */
    @SuppressWarnings("unchecked")
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
        } catch (final IOException e) {
            throw new DotRuntimeException("Failed to parse JSON response from CubeJS Server", e);
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

    /**
     * Sends the specified CubeJS query to our CubeJS Server, and returns the response as a string.
     *
     * @param cubeJSClient  The {@link CircuitBreakerUrl} object with the information required to
     *                      connect to the CubeJS service.
     * @param cubeJsUrl     The REST Endpoint used to connect to the CubeJS service.
     * @param queryAsString The CubeJS query used to retrieve data from ClickHouse.
     *
     * @return The response as a string.
     */
    private Response<String> getStringResponse(final CircuitBreakerUrl cubeJSClient,
                                               final String cubeJsUrl,
                                               final String queryAsString) {
        final TimeMetric timeMetric = TimeMetric.mark(getClass().getSimpleName());
        Logger.debug(this, String.format("Getting results from CubeJs [%s] with query [%s]", cubeJsUrl, queryAsString));
        final Response<String> response = cubeJSClient.doResponse();
        timeMetric.stop();
        if (cubeJSClient.isError()) {
            if (HttpStatus.SC_BAD_REQUEST == response.getStatusCode()) {
                throw new IllegalArgumentException(this.parseErrorMsg(response.getResponse()));
            } else if (HttpStatus.SC_INTERNAL_SERVER_ERROR == response.getStatusCode()) {
                throw new DotRuntimeException(this.parseErrorMsg(response.getResponse()));
            }
            final User user = this.getUser();
            final String errorMsg = Try.of(() -> LanguageUtil.get(user, "analytics.app.cubejs.response.failed"))
                    .getOrElse("CubeJS Server is not available. Please check that the parameters in the Experiments App and the current configuration in the Content Analytics Infrastructure are correct.");
            Logger.error(this, String.format("Failed to connect to service '%s'. %s", cubeJsUrl, errorMsg));
            throw new DotRuntimeException(errorMsg);
        }
        return response;
    }

    /**
     * Parses the error message from the CubeJS response in order to provide as many details as
     * possible. The error is usually a JSON object with an "error" field.
     *
     * @param response The CubeJS response.
     *
     * @return The actual error message.
     */
    private String parseErrorMsg(final String response) {
        if (JsonUtil.isValidJSON(response)) {
            try {
                final Map<String, Object> errorData = JsonUtil.getJsonFromString(response);
                return errorData.getOrDefault("error", BLANK).toString();
            } catch (final IOException e) {
                throw new DotRuntimeException("Could not extract error message from failed CubeJS request", e);
            }
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

    /**
     * Utility method used to retrieve the current User from the HTTP Request object in the Thread
     * Local context. If not available, the System User will be returned instead.
     *
     * @return The {@link User} in the HTTP Request.
     */
    private User getUser() {
        return Try.of(() -> {
            final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            if (request != null) {
                return WebAPILocator.getUserWebAPI().getLoggedInUser(request);
            }
            return APILocator.systemUser();
        }).getOrElse(APILocator.systemUser());
    }

}
