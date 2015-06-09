package com.dotcms.rest.api.v1.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.model.Condition;

import java.util.Date;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;
import static com.dotcms.rest.validation.Preconditions.checkNotNull;

@JsonDeserialize(builder = RestConditionGroup.Builder.class)
public class RestConditionGroup {

    private final String id;
    private final Condition.Operator operator;
    private final int priority;

    public String getId() {
        return id;
    }

    public Condition.Operator getOperator() {
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
        private String id;
        private Condition.Operator operator;
        private int priority=0;

        public Builder() {}

        public Builder id(String id) {
            this.id = id;
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

        public Builder from(RestConditionGroup copy) {
            id = copy.id;
            operator = copy.operator;
            priority = copy.priority;
            return this;
        }


        public void validate(){
            checkNotNull(operator, BadRequestException.class, "condition.operator is required.");
        }

        public RestConditionGroup build() {
            this.validate();
            return new RestConditionGroup(this);
        }
    }
}

