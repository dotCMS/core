package com.dotcms.rest.api.v1.sites.ruleengine.rules.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;


@JsonDeserialize(builder = RestRuleActionParameter.Builder.class)
public class RestRuleActionParameter extends Validated {

    @JsonIgnore
    public final String id;

    @NotNull
    public final String key;

    public final String value;

    private RestRuleActionParameter(Builder builder) {
        id = builder.id;
        key = builder.key;
        value = builder.value;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String key;
        @JsonProperty private String value;

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public RestRuleActionParameter build() {
            return new RestRuleActionParameter(this);
        }
    }
}
