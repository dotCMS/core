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

/**
 * @{link ParameterValuesTransformer} to transform the {@link QueryParameter} into String to be used
 * on {@link com.dotcms.analytics.metrics.AbstractCondition.Operator}.
 *
 * We need to transform any values type to String to be use on the Operators, so what this class do
 * is the follow:
 *
 * - Read the {@link Condition}'s value, it should come with the follow Json format:
 *
 * <code>{
 *     name: [Parameter Name],
 *     value: [Parameter Value]
 * }</code>
 *
 * - Filter the set of QueryParameters by the name read from {@link Condition}'s value.
 * - Return just the value of the QueryParameters filtered.
 *
 * @see QueryParameterValuesTransformer#transform(Collection, AbstractCondition)
 */
public class QueryParameterValuesTransformer implements ParameterValuesTransformer<QueryParameter> {

    @Override
    public Values transform(
            final Collection<QueryParameter> valuesFromEvent,
            final AbstractCondition<QueryParameter> condition) {

        final QueryParameter configuration = condition.value();
        final String name = configuration.getName();
        final String conditionValue = UtilMethods.isSetOrGet(configuration.getValue(), StringPool.BLANK)
                .toString();

        final Set<String> realValues = valuesFromEvent.stream()
                .filter(queryParameter -> queryParameter.getName().equals(name))
                .map(queryParameter -> queryParameter.getValue())
                .collect(Collectors.toSet());
        return new Values(conditionValue, realValues);

    }

    @Override
    public QueryParameter deserialize(final String jsonStringValue) {
        try {
            return JsonUtil.getObjectFromJson(jsonStringValue, QueryParameter.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
