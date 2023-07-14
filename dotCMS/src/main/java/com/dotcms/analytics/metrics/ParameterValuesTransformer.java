package com.dotcms.analytics.metrics;

import com.dotcms.experiments.business.result.Event;

/**
 * Transform the Value taken from the Event on String to be use on the {@link com.dotcms.analytics.metrics.AbstractCondition.Operator}
 * @param <T>
 */
public interface ParameterValuesTransformer<T> {
    String[] transform(final T[] valuesFromEvent, final AbstractCondition condition);
}
