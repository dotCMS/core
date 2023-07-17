package com.dotcms.analytics.metrics;

import com.dotcms.experiments.business.result.Event;
import java.util.Collection;

/**
 * Transform the Value taken from the Event on String to be use on the {@link com.dotcms.analytics.metrics.AbstractCondition.Operator}
 * @param <T>
 */
public interface ParameterValuesTransformer<T> {
    Values transform(final Collection<T> valuesFromEvent, final AbstractCondition condition);

    class Values {
        private final String real;
        private final Collection<String> toCompare;

        public Values(final String real, final Collection<String> toCompare) {
            this.real = real;
            this.toCompare = toCompare;
        }

        public String getReal() {
            return real;
        }

        public Collection<String> getValuesToCompare() {
            return toCompare;
        }
    }
}
