package com.dotcms.analytics.metrics;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


import java.io.Serializable;
import java.util.function.Supplier;

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
public interface AbstractCondition<T> extends Serializable {
    String parameter();
    Operator operator();
    T value();

    enum Operator {
        EQUALS(() -> "%s"),
        CONTAINS(() -> ".*%s.*"),
        EXISTS(() -> ".*%s");

        private Supplier<String> regexSupplier;

        Operator(final Supplier<String> regexSupplier){
            this.regexSupplier = regexSupplier;
        }

        public String regex() {
            return regexSupplier.get();
        }
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

        /**
         * Type of the Parameter it set how its value is going to be handled before
         * try to check the Condition
         */
        enum Type {
            SIMPLE(new DefaultParameterValuesTransformer(), (conditionValue, operatorRegex) -> String.format(operatorRegex, conditionValue.toString())),
            QUERY_PARAMETER(new QueryParameterValuesTransformer(), (conditionValue, operatorRegex) -> getQueryParameterRegex((QueryParameter) conditionValue, operatorRegex));

            final RegexCalculator regexCalculator;
            final ParameterValuesTransformer parameterValuesTransformer;
            Type (final ParameterValuesTransformer parameterValuesTransformer, final RegexCalculator regexCalculator) {
                this.parameterValuesTransformer = parameterValuesTransformer;
                this.regexCalculator = regexCalculator;
            }

            public <T> ParameterValuesTransformer<T> getTransformer() {
                return parameterValuesTransformer;
            }

            public String regex(final Object conditionValue, final String operatorRegex) {
                return regexCalculator.apply(conditionValue, operatorRegex);
            }

            private static String getQueryParameterRegex(final QueryParameter queryParameter, String operatorRegex) {
                return String.format(".*\\?(.*&)?%s=%s(&.*)*", queryParameter.getName(),
                        String.format(operatorRegex, queryParameter.getValue()));
            }
        }

    }

    interface RegexCalculator {
        String apply(Object conditionValue, String operatorRegex);
    }

}
