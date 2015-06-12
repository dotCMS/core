package com.dotcms.rest.api.v1.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.model.Condition;

import java.util.List;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;

@JsonDeserialize(builder = RestCondition.Builder.class)
public class RestCondition {

    private final String id;
    private final String name;
    private final String conditionlet;
    private final String comparison;
    private final List<RestConditionValue> values;
    private final Condition.Operator operator;
    private final int priority;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getConditionlet() {
        return conditionlet;
    }

    public String getComparison() {
        return comparison;
    }

    public List<RestConditionValue> getValues() {
        return values;
    }

    public Condition.Operator getOperator() {
        return operator;
    }

    public int getPriority() {
        return priority;
    }

    private RestCondition(Builder builder) {
        id = builder.id;
        name = builder.name;
        conditionlet = builder.conditionlet;
        comparison = builder.comparison;
        values = builder.values;
        operator = builder.operator;
        priority = builder.priority;
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String name;
        @JsonProperty private String conditionlet;
        @JsonProperty private String comparison;
        @JsonProperty private List<RestConditionValue> values;
        @JsonProperty private Condition.Operator operator;
        @JsonProperty private int priority;

        public Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder conditionlet(String conditionlet) {
            this.conditionlet = conditionlet;
            return this;
        }

        public Builder comparison(String comparison) {
            this.comparison = comparison;
            return this;
        }

        public Builder values(List<RestConditionValue> values) {
            this.values = values;
            return this;
        }

        public Builder operator(Condition.Operator operator) {
            this.operator = operator;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder from(RestCondition copy) {
            id = copy.id;
            name = copy.name;
            conditionlet = copy.conditionlet;
            comparison = copy.comparison;
            values = copy.values;
            operator = copy.operator;
            priority = copy.priority;
            return this;
        }


        public void validate(){
            checkNotEmpty(name, BadRequestException.class, "condition.name is required.");
        }

        public RestCondition build() {
            this.validate();
            return new RestCondition(this);
        }
    }
}

