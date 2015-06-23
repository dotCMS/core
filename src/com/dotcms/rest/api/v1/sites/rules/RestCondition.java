package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonCreator;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.model.Condition;

import java.util.List;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;

@JsonDeserialize(builder = RestCondition.Builder.class)
class RestCondition {

    private final String id;
    private final String name;
    private final String conditionlet;
    private final String comparison;
    private final List<RestConditionValue> values;
    private final String operator;
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

    public String getOperator() {
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
        @JsonProperty private final String name;
        @JsonProperty private final String conditionlet;
        @JsonProperty private final String comparison;
        @JsonProperty private final String operator;
        @JsonProperty private List<RestConditionValue> values;
        @JsonProperty private int priority = 0;

        @JsonCreator // needed for non default constructors
        public Builder(@JsonProperty("name") String name,
                       @JsonProperty("conditionlet") String conditionlet,
                       @JsonProperty("comparison") String comparison,
                       @JsonProperty("operator") String operator) {
            this.name = name;
            this.conditionlet = conditionlet;
            this.comparison = comparison;
            this.operator = operator;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder values(List<RestConditionValue> values) {
            this.values = values;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public void validate(){
            checkNotEmpty(name, BadRequestException.class, "condition.name is required.");
            checkNotEmpty(conditionlet, BadRequestException.class, "condition.conditionlet is required.");
            checkNotEmpty(comparison, BadRequestException.class, "condition.comparison is required.");
            checkNotEmpty(operator, BadRequestException.class, "condition.operator is required.");

            try {
                Condition.Operator.valueOf(operator);
            } catch(IllegalArgumentException iae) {
                throw new BadRequestException(iae, "conditionGroup.operator is invalid.");
            }
        }

        public RestCondition build() {
            this.validate();
            return new RestCondition(this);
        }
    }
}

