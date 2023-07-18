package com.dotcms.analytics.metrics;

import com.dotcms.experiments.business.result.Event;
import java.util.Collection;

/**
 * Transform the Value taken from the Event on String to be use on the {@link com.dotcms.analytics.metrics.AbstractCondition.Operator}
 * @param <T>
 */
public interface ParameterValuesTransformer<T> {
    Values transform(final Collection<T> valuesFromEvent, final AbstractCondition condition);

    /**
     * Represents the values to use on an Operator:
     *
     * - real : it is the value from the Event after transform.
     * - toCompare: Set of values to compare with the real value.
     */
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
