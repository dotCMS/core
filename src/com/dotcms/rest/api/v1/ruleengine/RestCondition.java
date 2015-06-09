package com.dotcms.rest.api.v1.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionValue;

import java.util.Date;
import java.util.List;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;

@JsonDeserialize(builder = RestCondition.Builder.class)
public class RestCondition {

    private final String id;
    private final String name;
    private final String conditionletId;
    private final String comparison;
    private final List<ConditionValue> values;
    private final Condition.Operator operator;
    private final int priority;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getConditionletId() {
        return conditionletId;
    }

    public String getComparison() {
        return comparison;
    }

    public List<ConditionValue> getValues() {
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
        conditionletId = builder.conditionletId;
        comparison = builder.comparison;
        values = builder.values;
        operator = builder.operator;
        priority = builder.priority;
    }

    public static final class Builder {
        private String id;
        private String name;
        private String conditionletId;
        private String comparison;
        private List<ConditionValue> values;
        private Condition.Operator operator;
        private int priority;

        public Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder conditionletId(String conditionletId) {
            this.conditionletId = conditionletId;
            return this;
        }

        public Builder comparison(String comparison) {
            this.comparison = comparison;
            return this;
        }

        public Builder values(List<ConditionValue> values) {
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
            conditionletId = copy.conditionletId;
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

