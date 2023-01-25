package com.dotcms.cube;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrl.Response;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotcms.jitsu.EventLogRunnable;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CubeJS Client.
 */
public class CubeClient {
    private String url;

    public CubeClient(final String url) {
        this.url = url;
    }

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

            return new CubeJSResultSet(UtilMethods.isSet(data) ? data : Collections.emptyList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
