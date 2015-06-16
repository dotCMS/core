package com.dotcms.rest.api.v1.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.model.Condition;

import java.util.Date;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;
import static com.dotcms.rest.validation.Preconditions.checkNotNull;

@JsonDeserialize(builder = RestConditionGroup.Builder.class)
class RestConditionGroup {

    private final String id;
    private final String operator;
    private final int priority;

    public String getId() {
        return id;
    }

    public String getOperator() {
        return operator;
    }

    public int getPriority() {
        return priority;
    }

    private RestConditionGroup(Builder builder) {
        id = builder.id;
        operator = builder.operator;
        priority = builder.priority;
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String operator;
        @JsonProperty private int priority=0;

        public Builder() {}

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

        public Builder from(RestConditionGroup copy) {
            id = copy.id;
            operator = copy.operator;
            priority = copy.priority;
            return this;
        }


        public void validate(){
            checkNotNull(operator, BadRequestException.class, "conditionGroup.operator is required.");

            try {
                Condition.Operator.valueOf(operator);
            } catch(IllegalArgumentException iae) {
                throw new BadRequestException(iae, "conditionGroup.operator is invalid.");
            }
        }

        public RestConditionGroup build() {
            this.validate();
            return new RestConditionGroup(this);
        }
    }
}

