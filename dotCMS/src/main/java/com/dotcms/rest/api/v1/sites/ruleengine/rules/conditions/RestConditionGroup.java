package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.validation.constraints.Operator;

import static com.dotcms.util.DotPreconditions.checkNotNull;

import java.util.Map;

@JsonDeserialize(builder = RestConditionGroup.Builder.class)
public class RestConditionGroup extends Validated {

    @JsonIgnore
    public final String id;

    @NotNull
    @Operator
    public final String operator;

    public final int priority;
    public final Map<String, Boolean> conditions;

    private RestConditionGroup(Builder builder) {
        id = builder.id;
        operator = builder.operator;
        priority = builder.priority;
        conditions = builder.conditions;
        checkValid();
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

        public RestConditionGroup build() {
            return new RestConditionGroup(this);
        }
    }
}

