package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.validation.constraints.Operator;
import java.util.Map;

@JsonDeserialize(builder = RestCondition.Builder.class)
public final class RestCondition extends Validated {

    @JsonIgnore
    public final String id;

    @NotNull
    public final String owningGroup;

    @NotNull
    public final String conditionlet;

    public final Map<String, RestConditionValue> values;

    @NotNull
    @Operator
    public final String operator;

    public final int priority;

    private RestCondition(Builder builder) {
        id = builder.id;
        owningGroup = builder.owningGroup;
        conditionlet = builder.conditionlet;
        values = builder.values;
        operator = builder.operator;
        priority = builder.priority;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String owningGroup;
        @JsonProperty private String conditionlet;
        @JsonProperty private String operator;
        @JsonProperty private Map<String, RestConditionValue> values;
        @JsonProperty private int priority = 0;

        public Builder owningGroup(String owningGroup) {
            this.owningGroup = owningGroup;
            return this;
        }

        public Builder conditionlet(String conditionlet) {
            this.conditionlet = conditionlet != null ? conditionlet : "";
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

        public RestCondition build() {
            return new RestCondition(this);
        }
    }
}

