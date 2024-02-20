package com.dotcms.analytics.metrics;


import com.dotcms.analytics.metrics.AbstractCondition.AbstractParameter;
import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A MetricType is anything that is desired to be measured on pages or sites in order to get analytic
 * insights. Examples can be Bounce Rate, Conversions, Custom Events, etc.
 *
 */

public enum MetricType {
    REACH_PAGE(new Builder()
            .label("Reaching a Page")
            .allRequiredParameters (Parameters.URL)
            .availableOperators(Operator.EQUALS, Operator.CONTAINS)
            .regexParameterName(Parameters.URL.name())),
    CLICK_ON_ELEMENT(new Builder()
            .label("Clicking on Element")
            .allRequiredParameters(Parameter.builder().name("pageUrl").build())
            .anyRequiredParameters(
                    Parameter.builder().name("id").build(),
                    Parameter.builder().name("class").build(),
                    Parameter.builder().name("target").build()
            )
            .availableOperators(Operator.EQUALS, Operator.CONTAINS)),
    EXIT_RATE(new Builder()
            .label("Exit Rate")
            .optionalParameters(Parameters.URL)),

    BOUNCE_RATE(new Builder()
            .label("Bounce Rate")
            .optionalParameters(Parameters.URL)),

    URL_PARAMETER(new Builder()
            .label("Url Parameter")
            .allRequiredParameters(Parameters.QUERY_PARAMETER)
            .regexParameterName(Parameters.QUERY_PARAMETER.name())
    );

    private final String label;

    private final Set<Operator> availableOperators;

    private final Set<Parameter> allRequiredParameters;

    private final Set<Parameter> anyRequiredParameters;

    private final Set<Parameter> optionalParameters;
    private final String regexParameterName;

    private static class Builder {
        private String label;
        private final Set<Operator> availableOperators = new HashSet<>();
        private final Set<Parameter> allRequiredParameters = new HashSet<>();
        private final Set<Parameter> anyRequiredParameters= new HashSet<>();
        private final Set<Parameter> optionalParameters= new HashSet<>();
        private String regexParameterName;

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder availableOperators(Operator...availableOperators) {
            this.availableOperators.addAll(Set.of(availableOperators));
            return this;
        }

        public Builder allRequiredParameters(Parameter...allRequiredParameters) {
            this.allRequiredParameters.addAll(Set.of(allRequiredParameters));
            return this;
        }

        public Builder anyRequiredParameters(Parameter...anyRequiredParameters) {
            this.anyRequiredParameters.addAll(Set.of(anyRequiredParameters));
            return this;
        }

        public Builder optionalParameters(Parameter...optionalParameters) {
            this.optionalParameters.addAll(Set.of(optionalParameters));
            return this;
        }

        public Builder regexParameterName(final String name) {
            this.regexParameterName = name;
            return this;
        }
    }

    MetricType(final Builder builder) {
        this.label = builder.label;
        this.allRequiredParameters = builder.allRequiredParameters;
        this.anyRequiredParameters = builder.anyRequiredParameters;
        this.optionalParameters = builder.optionalParameters;
        this.availableOperators = builder.availableOperators;
        this.regexParameterName = builder.regexParameterName;
    }

    public String label() {
        return label;
    }

    public Optional<String> getRegexParameterName() {
        return Optional.ofNullable(regexParameterName);
    }

    @JsonIgnore
    public Set<Parameter> getAllRequiredParameters() {
        return allRequiredParameters;
    }

    public Set<Parameter> getAnyRequiredParameters() {
        return anyRequiredParameters;
    }

    @JsonIgnore
    public Set<Operator> availableOperators() {
        return availableOperators;
    }

    @JsonIgnore
    public Set<Parameter> availableParameters() {
        final Set<Parameter> availableParameters = new HashSet<>(allRequiredParameters);
        availableParameters.addAll(optionalParameters);
        availableParameters.addAll(anyRequiredParameters);
        return availableParameters;
    }

    @JsonIgnore
    public Optional<Parameter> getParameter(final String parameterName) {
        return availableParameters().stream()
                .filter(parameter -> parameter.name().equals(parameterName))
                .limit(1)
                .findFirst();
    }
}
