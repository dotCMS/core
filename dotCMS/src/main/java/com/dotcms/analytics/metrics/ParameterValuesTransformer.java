package com.dotcms.analytics.metrics;

import com.dotcms.experiments.business.result.Event;
import java.util.Collection;
import java.util.List;

/**
 * Transform the Value taken from the Event on String to be use on the {@link com.dotcms.analytics.metrics.AbstractCondition.Operator}
 * @param <T>
 */
public interface ParameterValuesTransformer<T> {
    Collection<String> transform(final Collection<T> valuesFromEvent, final AbstractCondition condition);

    class Values<T> {
        private final String real;
        private final List<String> toCompare;

        public Values(final String real, List<String> toCompare) {
            this.real = real;
            this.toCompare = toCompare;
        }

        public String getReal() {
            return real;
        }

        public List<String> getToCompare() {
            return toCompare;
        }
    }
}
