package com.dotcms.analytics.metrics;


import java.util.Collection;

/**
 * No make any transformation to the values.
 */
public class DefaultParameterValuesTransformer implements ParameterValuesTransformer<String> {

    @Override
    public Values transform(final Collection<String> valuesFromEvent, final AbstractCondition<String> condition) {
        return new Values(condition.value().toString(), valuesFromEvent);
    }

    @Override
    public String deserialize(String value) {
        return value;
    }
}
