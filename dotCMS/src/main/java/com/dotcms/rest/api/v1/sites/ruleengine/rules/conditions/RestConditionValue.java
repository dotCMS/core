package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = RestConditionValue.Builder.class)
public final class RestConditionValue extends Validated {

    @JsonIgnore
    public final String id;

    @NotNull
    public final String key;

    public final String value;

    public final Integer priority;

    private RestConditionValue(Builder builder) {
        id = builder.id;
        value = builder.value;
        key = builder.key;
        priority = builder.priority;
        checkValid();
    }

    public static final class Builder {

        @JsonProperty private String id;
        @JsonProperty private String key;
        @JsonProperty private String value;
        @JsonProperty private Integer priority;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder priority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public RestConditionValue build() {
            return new RestConditionValue(this);
        }
    }
}
