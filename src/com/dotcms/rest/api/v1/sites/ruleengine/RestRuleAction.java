package com.dotcms.rest.api.v1.sites.ruleengine;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.exception.BadRequestException;

import java.util.Map;

import static com.dotcms.rest.validation.Preconditions.checkNotNull;

@JsonDeserialize(builder = RestRuleAction.Builder.class)
public class RestRuleAction {

    public final String id;
    public final String name;
    public final String owningRule;
    public final int priority;
    public final String actionlet;
    public final Map<String, RestRuleActionParameter> parameters;

    private RestRuleAction(Builder builder) {
        id = builder.id;
        name = builder.name;
        owningRule = builder.owningRule;
        priority = builder.priority;
        actionlet = builder.actionlet;
        parameters = builder.parameters;
    }

    public static final class Builder {
        @JsonProperty private String id; // not present on create
        @JsonProperty(required = true) private String name;
        @JsonProperty(required = true) private String owningRule;
        @JsonProperty(required = true) private String actionlet;
        @JsonProperty private Map<String, RestRuleActionParameter> parameters;
        @JsonProperty private int priority=0;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder owningRule(String owningRule) {
            this.owningRule = owningRule;
            return this;
        }

        public Builder actionlet(String actionlet) {
            this.actionlet = actionlet;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder parameters(Map<String, RestRuleActionParameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public void validate(){
            checkNotNull(name, BadRequestException.class, "ruleAction.name is required.");
            checkNotNull(actionlet, BadRequestException.class, "ruleAction.actionlet is required.");
            checkNotNull(owningRule, BadRequestException.class, "ruleAction.owningRule is required.");
        }

        public RestRuleAction build() {
            this.validate();
            return new RestRuleAction(this);
        }
    }
}

