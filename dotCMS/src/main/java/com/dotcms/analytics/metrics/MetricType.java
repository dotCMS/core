package com.dotcms.analytics.metrics;


import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum MetricType {
    REACH_PAGE("Maximize Reaching a Page",
            Map.of("url", Parameter.builder().name("url").build(),
                    "referrer", Parameter.builder().name("referrer").build()
            ),
            Set.of(Operator.EQUALS, Operator.CONTAINS)),
    CLICK_ON_ELEMENT("Maximize Clicking on Element",
            Map.of(
                    "id", Parameter.builder().name("id").build(),
                    "class", Parameter.builder().name("class").build(),
                    "target", Parameter.builder().name("target").build()
            ),
            Set.of(Operator.EQUALS, Operator.CONTAINS)),

    BOUNCE_RATE("Minimize Bounce Rate", Collections.emptyMap(), Collections.emptySet());

    private final String goalName;

    private final Set<Operator> availableOperators;

    private final Map<String, Parameter> availableParameters;

    MetricType(final String goalName, final Map<String, Parameter> availableParameters,
            final Set<Operator> availableOperators) {
        this.goalName = goalName;
        this.availableParameters = availableParameters;
        this.availableOperators = availableOperators;
    }

    public String goalName() {
        return goalName;
    }

    @JsonIgnore
    public Set<Operator> availableOperators() {
        return availableOperators;
    }

    @JsonIgnore
    public Set<Parameter> availableParameters() {
        return new HashSet<>(availableParameters.values());
    }
}
