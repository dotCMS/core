package com.dotcms.rest;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = RestTag.Builder.class)
public final class RestTag extends Validated  {

    @JsonIgnore
    public final String key;
    public final String label;


    private RestTag(Builder builder) {
        key = builder.key;
        label = builder.label;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String key;
        @JsonProperty private String label;

        public Builder() {
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public String key(){
            return key;
        }

        public Builder from(RestTag copy) {
            key = copy.key;
            return this;
        }

        public RestTag build() {
            return new RestTag(this);
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }
    }
}

