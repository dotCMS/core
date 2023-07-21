package com.dotcms.analytics.metrics;

import com.dotcms.analytics.metrics.ParameterValuesTransformer.Values;
import com.dotcms.experiments.business.result.Event;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Arrays;

import java.util.Collection;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * Represents a condition for a {@link Metric}. A Metric can have zero to many Conditions.
 * <p>
 * A condition comprises three parts:
 * <p>
 * <li>The 'parameter' can be an attribute of a page element, or a page URL, referrer, etc.
 * <li>The 'operator' is whatever is chosen as a comparison. See {@link Operator} for the different values
 * <li>The 'value' is the actual value that is desired to compare against. Can be a URL, referrer, id of an element, etc.
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = Condition.class)
@JsonDeserialize(as = Condition.class)
public interface AbstractCondition {
    String parameter();
    Operator operator();
    String value();

    /**
     * Return true is the Condition is meet on the {@link Event} using the {@link MetricType}.
     * The {@link Parameter}  define how is the value taken from the {@link Event} and
     * how are this values process before by evaluate by the Condition.
     *
     * @param parameter
     * @param event
     * @return
     */
    @JsonIgnore
    default boolean isValid(final Parameter parameter, final Event event){

        final Collection values = parameter.getValueGetter().getValuesFromEvent(parameter, event);

        final Values filterAndTransformValues = parameter.type().getTransformer()
                .transform(values, this);

        final String conditionValue = filterAndTransformValues.getConditionValue();

        final boolean conditionIsValid = filterAndTransformValues.getRealValues().stream()
                .anyMatch(value -> operator().getFunction().apply(value, conditionValue)
        );

        return conditionIsValid;
    }
    enum Operator {
        EQUALS((value1, value2) -> value1.equals(value2)),
        CONTAINS((value1, value2) -> value1.toString().contains(value2.toString())),
        REGEX((value, regex) -> value.toString().matches(regex.toString())),
        EXISTS((realValue, conditionValue) -> UtilMethods.isSet(realValue));

        private OperatorFunc function;

        Operator(final OperatorFunc func){
            this.function = func;
        }

        /**
         * Return a {@link OperatorFunc} to check whether the condition is valid oor not.
         * @return
         */
        public OperatorFunc getFunction() {
            return function;
        }
    }

    /**
     * Function to compare two values with an Operator.
     * If return true means that the Operator is valid for this two values, in otherwise return false.
     */
    interface OperatorFunc {
        boolean apply(Object value1, Object value2);
    }

    @Value.Style(typeImmutable="*", typeAbstract="Abstract*")
    @Value.Immutable
    interface AbstractParameter {
        String name();

        @Default
        default boolean validate(){
            return true;
        }

        @Default
        default Type type() {
            return Type.SIMPLE;
        }

        @Default
        default ParameterValueGetter getValueGetter() {
            return new DefaultParameterValuesGetter();
        }

        /**
         * Type of the Parameter it set how its value is going to be handled before
         * try to check the Condition
         */
        enum Type {
            SIMPLE(new DefaultParameterValuesTransformer()),
            QUERY_PARAMETER(new QueryParameterValuesTransformer());

            final ParameterValuesTransformer parameterValuesTransformer;
            Type (final ParameterValuesTransformer parameterValuesTransformer) {
                this.parameterValuesTransformer = parameterValuesTransformer;
            }

            public <T> ParameterValuesTransformer<T> getTransformer() {
                return parameterValuesTransformer;
            }
        }

    }

}
