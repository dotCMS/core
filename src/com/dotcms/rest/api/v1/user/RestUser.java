package com.dotcms.rest.api.v1.user;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.api.Validated;

/**
 *
 * @author Geoff M. Granum
 */
@JsonDeserialize(builder = RestUser.Builder.class)
public final class RestUser extends Validated  {

    public final String userId;
    public final String givenName;
    public final String surname;
    public final String roleId;


    private RestUser(Builder builder) {
        userId = builder.userId;
        givenName = builder.givenName;
        surname = builder.surname;
        roleId = builder.roleId;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String userId;
        @JsonProperty private String givenName;
        @JsonProperty private String surname;
        @JsonProperty private String roleId;

        public Builder() {
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder givenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public Builder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public Builder roleId(String roleId) {
            this.roleId = roleId;
            return this;
        }

        public RestUser build() {
            return new RestUser(this);
        }
    }
}
 
