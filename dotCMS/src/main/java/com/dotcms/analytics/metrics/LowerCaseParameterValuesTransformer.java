package com.dotcms.analytics.metrics;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Transform the values from the Event and {@link AbstractCondition} to lowercase
 */
public class LowerCaseParameterValuesTransformer implements ParameterValuesTransformer<String> {

    @Override
    public Values transform(final Collection<String> valuesFromEvent, final AbstractCondition<String> condition) {
        final Set<String> valuesFromLowercase = valuesFromEvent.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return new Values(condition.value().toLowerCase(), valuesFromLowercase);
    }

    @Override
    public String deserialize(final String value) {
        return value.toLowerCase();
    }
}
