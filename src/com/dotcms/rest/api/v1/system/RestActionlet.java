package com.dotcms.rest.api.v1.system;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = RestActionlet.Builder.class)
public final class RestActionlet extends Validated {

    @JsonIgnore
    public final String id;

    @NotNull
    public final String name;

    public final String i18nKey;

    private RestActionlet(Builder builder) {
        id = builder.id;
        name = builder.name;
        i18nKey = builder.i18nKey;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String id;
        @JsonProperty private String name;
        @JsonProperty private String i18nKey;

        /*
            RestActionlet restActionlet = new RestActionlet.Builder()
            .id( input.getId() )
            .name( input.getName() )
            .i18nKey( input.getI18nKey() )
            .build();
        */
        public Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder i18nKey(String i18nKey) {
            this.i18nKey = i18nKey;
            return this;
        }

        public Builder from(RestActionlet copy) {
            id = copy.id;
            name = copy.name;
            i18nKey = copy.i18nKey;
            return this;
        }

        public RestActionlet build() {
            return new RestActionlet(this);
        }
    }
}

