package com.dotcms.rest.api.v1.sites.ruleengine.rules.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import com.dotcms.rest.api.Validated;

import com.dotmarketing.portlets.rules.model.ParameterModel;

import static com.dotcms.util.DotPreconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(builder = RestRuleAction.Builder.class)
public class RestRuleAction extends Validated {

    @Length(min = 36, max = 36)
    @JsonIgnore
    public final String id;

    @NotNull
    @Length(min = 36, max = 36)
    public final String owningRule;

    public final int priority;

    @NotBlank
    public final String actionlet;

    public final Map<String, ParameterModel> parameters;

    private RestRuleAction(Builder builder) {
        id = builder.id;
        owningRule = builder.owningRule;
        priority = builder.priority;
        actionlet = builder.actionlet;
        parameters = builder.parameters;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String id; // not present on create
        @JsonProperty(required = true) private String name;
        @JsonProperty(required = true) private String owningRule;
        @JsonProperty(required = true) private String actionlet;
        @JsonProperty private Map<String, ParameterModel> parameters = new HashMap<>();
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

        public Builder parameters(Map<String, ParameterModel> parameters) {
            this.parameters = parameters;
            return this;
        }

        public RestRuleAction build() {
            return new RestRuleAction(this);
        }
    }
}

