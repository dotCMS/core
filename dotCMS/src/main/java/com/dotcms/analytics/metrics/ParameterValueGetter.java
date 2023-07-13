package com.dotcms.analytics.metrics;

import com.dotcms.experiments.business.result.Event;

public interface ParameterValueGetter<T> {
    T[] getValuesFromEvent(final AbstractCondition.AbstractParameter parameter, final Event event);
    String[] filterAndTransform(final T[] valuesFromEvent, final Condition condition);
}
