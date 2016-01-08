package com.dotcms.rest.api.v1.personas;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;

/**
 * Note: Pretend this class exists in a separate module from the core data types, and cannot have any knowledge of those types. Because it should.
 *
 * @author Geoff M. Granum
 */
@JsonDeserialize(builder = RestPersona.Builder.class)
public final class RestPersona extends Validated  {

    @Length(min = 36, max = 36)
    @JsonIgnore
    public final String key;
    public final String name;
    public final String description;


    private RestPersona(Builder builder) {
        key = builder.key;
        name = builder.name;
        description = builder.description;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String key;
        @JsonProperty private String description;
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

        public Builder from(RestPersona copy) {
            key = copy.key;
            return this;
        }

        public RestPersona build() {
            return new RestPersona(this);
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }
    }
}
 
