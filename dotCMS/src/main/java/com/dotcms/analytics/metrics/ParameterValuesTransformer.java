package com.dotcms.analytics.metrics;


import java.util.Collection;


/**
 * Transform the Value taken from the Event on String to be use on the {@link com.dotcms.analytics.metrics.AbstractCondition.Operator}
 * @param <T>
 */
public interface ParameterValuesTransformer<T> {

    Values transform(final Collection<T> valuesFromEvent, final AbstractCondition<T> condition);

    T deserialize(final String value);

    /**
     * Represents the values to use on an Operator:
     *
     * - real : it is the value from the Event after transform.
     * - toCompare: Set of values to compare with the real value.
     */
    class Values {
        private final Collection<String> realValues;
        private final String conditionValue;

        public Values(final String conditionValue, final Collection<String> realValues) {
            this.realValues = realValues;
            this.conditionValue = conditionValue;
        }

        public Collection<String> getRealValues() {
            return realValues;
        }

        public String getConditionValue() {
            return conditionValue;
        }
    }
}
