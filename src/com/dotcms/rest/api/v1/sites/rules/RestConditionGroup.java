package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonCreator;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.model.Condition;

import java.util.List;
import java.util.Map;

import static com.dotcms.rest.validation.Preconditions.checkNotNull;

@JsonDeserialize(builder = RestConditionGroup.Builder.class)
public class RestConditionGroup {

    public final String id;
    public final String operator;
    public final int priority;
    public final Map<String, Boolean> conditions;

    private RestConditionGroup(Builder builder) {
        id = builder.id;
        operator = builder.operator;
        priority = builder.priority;
        conditions = builder.conditions;
    }

    public static final class Builder {
        @JsonProperty private String operator;
        @JsonProperty private String id;
        @JsonProperty private int priority = 0;
        @JsonProperty private  Map<String, Boolean> conditions;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder conditions( Map<String, Boolean> conditions) {
            this.conditions = ImmutableMap.copyOf(conditions);
            return this;
        }

        public void validate() {
            checkNotNull(operator, BadRequestException.class, "conditionGroup.operator is required.");

            try {
                Condition.Operator.valueOf(operator);
            } catch (IllegalArgumentException iae) {
                throw new BadRequestException(iae, "conditionGroup.operator is invalid.");
            }
        }

        public RestConditionGroup build() {
            this.validate();
            return new RestConditionGroup(this);
        }
    }
}

