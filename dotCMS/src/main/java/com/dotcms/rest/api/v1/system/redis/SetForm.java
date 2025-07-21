package com.dotcms.rest.api.v1.system.redis;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = SetForm.Builder.class)
public class SetForm extends Validated {

    @NotNull
    private final String key;

    @NotNull
    private final Object value;

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    private SetForm(final SetForm.Builder builder) {

        key   = builder.key;
        value = builder.value;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty(required = true) private String key;
        @JsonProperty(required = true) private Object value;

        public SetForm.Builder key(String key) {
            this.key = key;
            return this;
        }

        public SetForm.Builder value(String value) {
            this.value = value;
            return this;
        }

        public SetForm build() {
            return new SetForm(this);
        }
    }
}
