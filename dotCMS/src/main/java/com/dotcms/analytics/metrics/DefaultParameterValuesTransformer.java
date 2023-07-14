package com.dotcms.analytics.metrics;



/**
 * No make any transformation to the values.
 */
public class DefaultParameterValuesTransformer implements ParameterValuesTransformer<String> {

    @Override
    public String[] transform(final String[] valuesFromEvent, final AbstractCondition condition) {
        return valuesFromEvent;
    }
}
