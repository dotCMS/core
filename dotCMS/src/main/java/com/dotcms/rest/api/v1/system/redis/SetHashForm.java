package com.dotcms.rest.api.v1.system.redis;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(builder = SetHashForm.Builder.class)
public class SetHashForm extends Validated {

    @NotNull
    private final String key;

    @NotNull
    private final Map<String, Object> fields;

    public String getKey() {
        return key;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    private SetHashForm(final SetHashForm.Builder builder) {

        key    = builder.key;
        fields = builder.fields;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty(required = true) private String key;
        @JsonProperty(required = true) private Map<String, Object> fields;

        public SetHashForm.Builder key(String key) {
            this.key = key;
            return this;
        }

        public SetHashForm.Builder fields(Map<String, Object> fields) {
            this.fields = fields;
            return this;
        }

        public SetHashForm build() {
            return new SetHashForm(this);
        }
    }
}
