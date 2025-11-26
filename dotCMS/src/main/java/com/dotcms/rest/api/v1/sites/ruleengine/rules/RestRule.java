package com.dotcms.rest.api.v1.sites.ruleengine.rules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.RestConditionGroup;
import com.dotcms.rest.validation.constraints.FireOn;
import com.dotmarketing.portlets.rules.model.Rule;
import java.util.Map;


/**
 * Note: Pretend this class exists in a separate module from the core data types, and cannot have any knowledge of those types. Because it should.
 *
 * @author Geoff M. Granum
 */
@JsonDeserialize(builder = RestRule.Builder.class)
public final class RestRule extends Validated  {

    @Length(min = 36, max = 36)
    @JsonIgnore
    public final String key;

    @NotBlank
    public final String name;

    @FireOn
    public final String fireOn;

    public final Boolean shortCircuit;

    public final Integer priority;

    public final Boolean enabled;

    public final Map<String, RestConditionGroup> conditionGroups;

    public final Map<String, Boolean> ruleActions;

    private RestRule(Builder builder) {
        key = builder.key;
        name = builder.name;
        fireOn = builder.fireOn;
        shortCircuit = builder.shortCircuit;
        priority = builder.priority;
        enabled = builder.enabled;
        conditionGroups = builder.conditionGroups;
        ruleActions = builder.ruleActions;
        checkValid();
    }

    @JsonIgnoreProperties({"groups", "actions"})
    public static final class Builder {
        @JsonProperty private String key;
        @JsonProperty private String name;
        @JsonProperty private String fireOn = Rule.FireOn.EVERY_PAGE.name();
        @JsonProperty private Boolean shortCircuit = false;
        @JsonProperty private Integer priority = 0;
        @JsonProperty private Boolean enabled = false;
        @JsonProperty private Map<String, RestConditionGroup> conditionGroups = ImmutableMap.of();
        @JsonProperty private Map<String, Boolean> ruleActions = ImmutableMap.of();

        public Builder() {
        }

        public Builder name( String name) {
            this.name = name;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public String key(){
            return key;
        }

        public Builder fireOn(String fireOn) {
            this.fireOn = fireOn;
            return this;
        }

        public Builder shortCircuit(boolean shortCircuit) {
            this.shortCircuit = shortCircuit;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder conditionGroups(Map<String, RestConditionGroup> conditionGroups) {
            this.conditionGroups = ImmutableMap.copyOf(conditionGroups);
            return this;
        }

        public Builder ruleActions(Map<String, Boolean> ruleActions) {
            this.ruleActions = ImmutableMap.copyOf(ruleActions);
            return this;
        }

        public Builder from(RestRule copy) {
            key = copy.key;
            name = copy.name;
            fireOn = copy.fireOn;
            shortCircuit = copy.shortCircuit;
            priority = copy.priority;
            enabled = copy.enabled;
            conditionGroups = ImmutableMap.copyOf(copy.conditionGroups);
            ruleActions = ImmutableMap.copyOf(copy.ruleActions);
            return this;
        }

        public RestRule build() {
            return new RestRule(this);
        }
    }
}
 
