package com.dotcms.analytics.metrics;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = Condition.class)
@JsonDeserialize(as = Condition.class)
public interface AbstractCondition {
    String parameter();
    Operator operator();
    String value();

    enum Operator {
        EQUALS,
        CONTAINS
    }

    @Value.Style(typeImmutable="*", typeAbstract="Abstract*")
    @Value.Immutable
    interface AbstractParameter {
        String name();
    }

}
