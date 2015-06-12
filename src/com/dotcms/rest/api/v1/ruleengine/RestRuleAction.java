package com.dotcms.rest.api.v1.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonCreator;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import java.util.Collections;
import java.util.List;

import static com.dotcms.rest.validation.Preconditions.checkNotNull;

@JsonDeserialize(builder = RestRuleAction.Builder.class)
public class RestRuleAction {

    private final String id;
    private final String name;
    private final int priority;
    private final String actionlet;
    private final List<RuleActionParameter> parameters;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getActionlet() {
        return actionlet;
    }

    public List<RuleActionParameter> getParameters() {
        return parameters;
    }

    public int getPriority() {
        return priority;
    }

    private RestRuleAction(Builder builder) {
        id = builder.id;
        name = builder.name;
        priority = builder.priority;
        actionlet = builder.actionlet;
        parameters = builder.parameters;
    }

    public static final class Builder {
        private String id; // optional - not present when creating - present when updating
        private final String name; // required
        private final String actionlet; // required
        private List<RuleActionParameter> parameters; // optional
        private int priority=0; // optional - default value

        @JsonCreator // needed for non default constructors
        public Builder(@JsonProperty("name") String name,
                       @JsonProperty("actionlet") String actionlet) {
            this.name = name;
            this.actionlet = actionlet;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder parameters(List<RuleActionParameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public void validate(){
            checkNotNull(name, BadRequestException.class, "ruleAction.name is required.");
            checkNotNull(actionlet, BadRequestException.class, "ruleAction.actionlet is required.");
        }

        public RestRuleAction build() {
            this.validate();
            return new RestRuleAction(this);
        }
    }
}

