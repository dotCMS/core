package com.dotcms.rest.api.v1.user;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.api.Validated;

/**
 * Encapsulates the minimal information for the RestUser
 * @author Geoff M. Granum
 */
@JsonDeserialize(builder = RestUser.Builder.class)
public final class RestUser extends Validated  {

    public final String userId;
    public final String givenName;
    public final String email;
    public final String surname;
    public final String roleId;


    private RestUser(RestUser.Builder builder) {
        userId    = builder.userId;
        givenName = builder.givenName;
        surname   = builder.surname;
        roleId    = builder.roleId;
        email     = builder.email;

        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String userId;
        @JsonProperty private String givenName;
        @JsonProperty private String surname;
        @JsonProperty private String roleId;
        @JsonProperty private String email;

        public Builder() {
        }

        public RestUser.Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public RestUser.Builder givenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public RestUser.Builder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public RestUser.Builder roleId(String roleId) {
            this.roleId = roleId;
            return this;
        }

        public RestUser.Builder email(String email) {
            this.email = email;
            return this;
        }

        public RestUser build() {
            return new RestUser(this);
        }
    }
}
 
