package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.rules.model.Rule;
import java.util.List;
import java.util.Map;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;

/**
 * Note: Pretend this class exists in a separate module from the core data types, and cannot have any knowledge of those types. Because it should.
 *
 * @author Geoff M. Granum
 */
@JsonDeserialize(builder = RestRule.Builder.class)
public class RestRule {

    public final String key;
    public final String name;
    public final String fireOn;
    public final Boolean shortCircuit;
    public final Integer priority;
    public final Boolean enabled;
    public final Map<String, RestConditionGroup> groups;

    private RestRule(Builder builder) {
        key = builder.key;
        name = builder.name;
        fireOn = builder.fireOn;
        shortCircuit = builder.shortCircuit;
        priority = builder.priority;
        enabled = builder.enabled;
        groups = builder.groups;
    }

    @JsonIgnoreProperties({"groups", "actions"})
    public static final class Builder {
        @JsonProperty private String key;
        @JsonProperty private String name;
        @JsonProperty private String fireOn = Rule.FireOn.EVERY_PAGE.name();
        @JsonProperty private Boolean shortCircuit = false;
        @JsonProperty private Integer priority = 0;
        @JsonProperty private Boolean enabled = false;
        @JsonProperty private Map<String, RestConditionGroup> groups = ImmutableMap.of();


        /*
            RestRule restRule = new RestRule.Builder()
            .key( input.getId() )
            .name( input.getName() )
            .fireOn( input.getFireOn() )
            .shortCircuit( input.getShortCircuit() )
            .site( input.getsite() )
            .folder( input.getFolder() )
            .priority( input.getPriority() )
            .enabled( input.getEnabled() )
            .modDate( input.getModDate() )
            .build();
        */

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

        public Builder groups(Map<String, RestConditionGroup> groups) {
            this.groups = ImmutableMap.copyOf(groups);
            return this;
        }

        public Builder from(RestRule copy) {
            key = copy.key;
            name = copy.name;
            fireOn = copy.fireOn;
            shortCircuit = copy.shortCircuit;
            priority = copy.priority;
            enabled = copy.enabled;
            groups = ImmutableMap.copyOf(copy.groups);
            return this;
        }

        public RestRule build() {
            this.validate();
            return new RestRule(this);
        }

        public void validate() {
            checkNotEmpty(name, BadRequestException.class, "rule.name is required.");
        }
    }
}
 
