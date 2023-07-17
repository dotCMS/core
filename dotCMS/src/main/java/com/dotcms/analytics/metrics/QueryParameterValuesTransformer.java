package com.dotcms.analytics.metrics;

import com.dotcms.util.JsonUtil;
import com.dotmarketing.exception.DotRuntimeException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryParameterValuesTransformer implements ParameterValuesTransformer<QueryParameter> {

    @Override
    public Collection<String> transform(
            final Collection<QueryParameter> valuesFromEvent,
            final AbstractCondition condition) {

        try {
            final Map<String, Object> configuration = JsonUtil.getJsonFromString(condition.value());
            final String name = configuration.get("name").toString().toLowerCase();
            final String value = configuration.get("value").toString().toLowerCase();

            return valuesFromEvent.stream()
                    .filter(queryParameter -> queryParameter.getName().toLowerCase().equals(name))
                    .map(queryParameter -> queryParameter.getValue())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }

    }
}
