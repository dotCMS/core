package com.dotcms.rest.api.v1.sites.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.api.v1.sites.rules.RestConditionValue;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.model.Condition;
import java.util.Map;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;

@JsonDeserialize(builder = RestCondition.Builder.class)
public final class RestCondition {

    public final String id;
    public final String name;
    public final String rule;
    public final String conditionGroup;
    public final String conditionlet;
    public final String comparison;
    public final Map<String, RestConditionValue> values;
    public final String operator;
    public final int priority;

    private RestCondition(Builder builder) {
        id = builder.id;
        name = builder.name;
        rule = builder.rule;
        conditionGroup = builder.conditionGroup;
        conditionlet = builder.conditionlet;
        comparison = builder.comparison;
        values = builder.values;
        operator = builder.operator;
        priority = builder.priority;
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String name;
        @JsonProperty private String rule;
        @JsonProperty private String conditionGroup;
        @JsonProperty private String conditionlet;
        @JsonProperty private String comparison;
        @JsonProperty private String operator;
        @JsonProperty private Map<String, RestConditionValue> values;
        @JsonProperty private int priority = 0;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder rule(String rule) {
            this.rule = rule;
            return this;
        }

        public Builder conditionGroup(String conditionGroup) {
            this.conditionGroup = conditionGroup;
            return this;
        }

        public Builder conditionlet(String conditionlet) {
            this.conditionlet = conditionlet != null ? conditionlet : "";
            return this;
        }

        public Builder comparison(String comparison) {
            this.comparison = comparison;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder values(Map<String, RestConditionValue> values) {
            this.values = values;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public void validate(){
            checkNotEmpty(name, BadRequestException.class, "condition.name is required.");
            checkNotEmpty(rule, BadRequestException.class, "condition.rule is required.");
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

