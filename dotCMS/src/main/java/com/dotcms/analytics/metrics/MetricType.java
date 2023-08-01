package com.dotcms.analytics.metrics;


import com.dotcms.analytics.metrics.AbstractCondition.AbstractParameter;
import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A MetricType is anything that is desired to be measured on pages or sites in order to get analytic
 * insights. Examples can be Bounce Rate, Conversions, Custom Events, etc.
 *
 */

public enum MetricType {
    REACH_PAGE(new Builder()
            .label("Reaching a Page")
            .allRequiredParameters (Parameters.URL) //TODO we can create singletons of these Parameters in order to reuse
            .optionalParameters(Parameter.builder().name("referer").build())
            .availableOperators(Operator.EQUALS, Operator.CONTAINS)),
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
            .allRequiredParameters(
                    Parameter.builder().name("queryParameter")
                        .valueGetter(new QueryParameterValuesGetter())
                        .type(AbstractParameter.Type.QUERY_PARAMETER)
                        .build()
            )
            .optionalParameters(Parameters.VISIT_BEFORE)
    );

    private final String label;

    private final Set<Operator> availableOperators;

    private final Set<Parameter> allRequiredParameters;

    private final Set<Parameter> anyRequiredParameters;

    private final Set<Parameter> optionalParameters;

    private static class Builder {
        private String label;
        private final Set<Operator> availableOperators = new HashSet<>();
        private final Set<Parameter> allRequiredParameters = new HashSet<>();
        private final Set<Parameter> anyRequiredParameters= new HashSet<>();
        private final Set<Parameter> optionalParameters= new HashSet<>();

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
    }

    MetricType(final Builder builder) {
        this.label = builder.label;
        this.allRequiredParameters = builder.allRequiredParameters;
        this.anyRequiredParameters = builder.anyRequiredParameters;
        this.optionalParameters = builder.optionalParameters;
        this.availableOperators = builder.availableOperators;
    }

    public String label() {
        return label;
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
