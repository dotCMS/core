package com.dotcms.analytics.metrics;

import com.dotcms.analytics.metrics.AbstractCondition.AbstractParameter;
import com.dotcms.experiments.business.result.Event;
import com.dotmarketing.exception.DotRuntimeException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryParameterValuesGetter implements ParameterValueGetter<QueryParameter> {
    @Override
    public Collection<QueryParameter> getValuesFromEvent(
            final AbstractParameter parameter,
            final Event event) {

        final String urlValue = event.get("url").map(Object::toString).orElseThrow();

        try {
            URI uri = new URI(urlValue);
            String query = uri.getQuery();

            return getQueryParams(query).entrySet().stream()
                    .map(entry -> new QueryParameter(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
        } catch (URISyntaxException e) {
            throw new DotRuntimeException(e);
        }
    }

    private static Map<String, String> getQueryParams(String query) {
        Map<String, String> queryParams = new HashMap<>();

        if (query != null) {
            String[] params = query.split("&");

            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = decodeURL(keyValue[0]);
                    String value = decodeURL(keyValue[1]);
                    queryParams.put(key, value);
                }
            }
        }

        return queryParams;
    }

    private static String decodeURL(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
