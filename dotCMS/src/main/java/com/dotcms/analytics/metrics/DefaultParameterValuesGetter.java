package com.dotcms.analytics.metrics;

import com.dotcms.analytics.metrics.AbstractCondition.AbstractParameter;
import com.dotcms.experiments.business.result.Event;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;

/**
 * Take the value from the {@link Event}'s attribute that has the same name that the {@link Parameter#name()}
 */
public class DefaultParameterValuesGetter implements ParameterValueGetter<String> {

    @Override
    public Collection<String> getValuesFromEvent(final AbstractParameter parameter, final Event event) {
        final String eventValue = event.get(parameter.name())
                .map(value -> value.toString())
                .orElseThrow(() -> new RuntimeException());

        return ImmutableSet.of(eventValue);
    }

}
