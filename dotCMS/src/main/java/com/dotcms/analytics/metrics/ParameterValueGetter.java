package com.dotcms.analytics.metrics;

import com.dotcms.experiments.business.result.Event;
import java.util.Collection;

/**
 * Define how the Values are taken from the Event later this value are processed by any {@link ParameterValuesTransformer}.
 * @param <T>
 */
public interface ParameterValueGetter<T> {
    Collection<T> getValuesFromEvent(final AbstractCondition.AbstractParameter parameter, final Event event);
}
