package com.dotcms.analytics.metrics;


import static com.dotcms.util.HttpRequestDataUtil.getQueryParams;

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

/**
 * {@link ParameterValueGetter} to get the Query Parameters from the Event's Url.
 */
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
}
