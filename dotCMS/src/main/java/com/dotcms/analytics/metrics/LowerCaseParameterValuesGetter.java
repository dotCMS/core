package com.dotcms.analytics.metrics;

import com.dotcms.analytics.metrics.AbstractCondition.AbstractParameter;
import com.dotcms.experiments.business.result.Event;
import java.util.Collection;
import java.util.stream.Collectors;

public class LowerCaseParameterValuesGetter implements ParameterValueGetter<String>{

    private final ParameterValueGetter<String> parameterValueGetter;

    public LowerCaseParameterValuesGetter(final ParameterValueGetter<String> parameterValueGetter) {
        this.parameterValueGetter = parameterValueGetter;
    }

    @Override
    public Collection<String> getValuesFromEvent(final AbstractParameter parameter,
            final Event event) {
        return parameterValueGetter.getValuesFromEvent(parameter, event).stream()
                .map(value -> value.toString().toLowerCase())
                .collect(Collectors.toList());
    }
}
