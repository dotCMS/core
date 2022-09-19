package com.dotcms.experiments.model;

import com.dotmarketing.portlets.rules.model.LogicalOperator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = TargetingCondition.class)
@JsonDeserialize(as = TargetingCondition.class)
public interface AbstractTargetingCondition extends Serializable {
    @JsonProperty("conditionKey")
    String conditionKey();

    @JsonProperty("comparison")
    String comparison();

    @JsonProperty("value")
    String value();

    @JsonProperty("operator")
    @Value.Default
    default LogicalOperator operator() {
        return LogicalOperator.AND;
    }
}
