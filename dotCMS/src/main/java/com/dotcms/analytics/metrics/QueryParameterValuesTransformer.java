package com.dotcms.analytics.metrics;

import com.dotcms.util.JsonUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryParameterValuesTransformer implements ParameterValuesTransformer<QueryParameter> {

    @Override
    public Values transform(
            final Collection<QueryParameter> valuesFromEvent,
            final AbstractCondition condition) {

        try {
            final Map<String, Object> configuration = JsonUtil.getJsonFromString(condition.value());
            final String name = configuration.get("name").toString().toLowerCase();
            final String conditionValue = UtilMethods.isSetOrGet(configuration.get("value"), StringPool.BLANK).toString();

            final Set<String> realValues = valuesFromEvent.stream()
                    .filter(queryParameter -> queryParameter.getName().toLowerCase().equals(name))
                    .map(queryParameter -> queryParameter.getValue())
                    .collect(Collectors.toSet());
            return new Values(conditionValue, realValues);

        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }

    }
}
