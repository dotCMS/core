package com.dotcms.rest.api.v1.languages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;

public class RestLanguage extends Validated {
    @Length(min = 2)
    @JsonIgnore
    public final String key;
    public final String name;


    private RestLanguage(Builder builder) {
        key = builder.key;
        name = builder.name;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String key;
        @JsonProperty private String name;

        public Builder() {
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public String key(){
            return key;
        }

        public Builder from(RestLanguage copy) {
            key = copy.key;
            return this;
        }

        public RestLanguage build() {
            return new RestLanguage(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }
    }
}
