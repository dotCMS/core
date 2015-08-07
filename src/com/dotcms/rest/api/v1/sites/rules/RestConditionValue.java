package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;

import static com.dotcms.rest.validation.Preconditions.checkNotNull;

@JsonDeserialize(builder = RestConditionValue.Builder.class)
public final class RestConditionValue extends Validated {

    @JsonIgnore
    public final String id;

    @NotNull
    public final String value;

    public final int priority;

    private RestConditionValue(Builder builder) {
        id = builder.id;
        value = builder.value;
        priority = builder.priority;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String value;
        @JsonProperty private int priority = 0;

        public Builder value(String value) {
            this.value = value;
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

        public RestConditionValue build() {
            return new RestConditionValue(this);
        }
    }
}
