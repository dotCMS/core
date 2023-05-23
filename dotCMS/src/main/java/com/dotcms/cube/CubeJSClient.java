package com.dotcms.cube;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrl.Response;
import com.dotcms.jitsu.EventLogRunnable;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    private String url;

    public CubeJSClient(final String url) {
        this.url = url;
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
     * This method use Pagination to get the Data from CubeJs Server.
     *
     * @param query Query to be run in the CubeJS Server
     * @return
     */
    public CubeJSResultSet sendWithPagination(final CubeJSQuery query) {

        DotPreconditions.notNull(query, "Query not must be NULL");

        final CubeJSQuery countQuery = query.builder()
                .measures("Events.count")
                .filters(Arrays.asList(query.filters()))
                .orders(Arrays.asList(query.orders()))
                .dimensions(null)
                .build();

        final CubeJSResultSet countResultSet = this.send(countQuery);

        final long totalItems = countResultSet.iterator().next()
                .get("Events.count")
                .map(value -> Long.parseLong(value.toString()))
                .orElseThrow();

        if (totalItems == 0) {
            return new CubeJSResultSetImpl(Collections.emptyList());
        }

        return totalItems > PAGE_SIZE ?
                new PaginationCubeJSResultSet(this, query, totalItems, PAGE_SIZE) :
                this.send(query);
    }


    /**
     * It the same of the {@link #sendWithPagination(CubeJSQuery)} but it doesn't use pagination.
     *
     * @param query
     * @return
     *
     * @see #sendWithPagination(CubeJSQuery)
     */
    public CubeJSResultSet send(final CubeJSQuery query) {
        DotPreconditions.notNull(query, "Query not must be NULL");
        
        final CircuitBreakerUrl cubeJSClient = CircuitBreakerUrl.builder()
                .setMethod(Method.GET)
                .setUrl(String.format("%s/cubejs-api/v1/load", url))
                .setParams(map("query", query.toString()))
                .setTimeout(4000)
                .build();

        final Response<String> response = Try.of(cubeJSClient::doResponse)
                .onFailure(e -> Logger.warnAndDebug(EventLogRunnable.class, e.getMessage(), e))
                .getOrElse(CircuitBreakerUrl.EMPTY_RESPONSE);

        try {
            final String responseAsString = UtilMethods.isSet(response) ? response.getResponse() :
                    StringPool.BLANK;
            final Map<String, Object> responseAsMap = UtilMethods.isSet(responseAsString) ?
                   JsonUtil.getJsonFromString(responseAsString) : new HashMap<>();
            final List<Map<String, Object>> data = (List<Map<String, Object>>) responseAsMap.get("data");

            return new CubeJSResultSetImpl(UtilMethods.isSet(data) ? data : Collections.emptyList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
