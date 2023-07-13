package com.dotcms.analytics.metrics;

import com.dotcms.analytics.metrics.AbstractCondition.AbstractParameter;
import com.dotcms.experiments.business.result.Event;


public class DefaultParameterValueGetter implements ParameterValueGetter<String> {

    @Override
    public String[] getValuesFromEvent(final AbstractParameter parameter, final Event event) {
        final String eventValue = event.get(parameter.name())
                .map(value -> value.toString())
                .orElseThrow(() -> new RuntimeException());

        return new String[]{eventValue};
    }

    @Override
    public String[] filterAndTransform(final String[] valuesFromEvent, final Condition condition) {
        return valuesFromEvent;
    }
}
