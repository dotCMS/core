package com.dotcms.rest.api.v1.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.model.Condition;

import static com.dotcms.rest.validation.Preconditions.checkNotNull;

@JsonDeserialize(builder = RestRuleAction.Builder.class)
public class RestRuleAction {

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

    private RestRuleAction(Builder builder) {
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

        public Builder from(RestRuleAction copy) {
            id = copy.id;
            operator = copy.operator;
            priority = copy.priority;
            return this;
        }


        public void validate(){
            checkNotNull(operator, BadRequestException.class, "conditionGroup.operator is required.");
        }

        public RestRuleAction build() {
            this.validate();
            return new RestRuleAction(this);
        }
    }
}

