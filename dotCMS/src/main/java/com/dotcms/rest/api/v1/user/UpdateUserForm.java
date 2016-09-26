package com.dotcms.rest.api.v1.user;

import com.dotcms.repackage.com.drew.lang.annotations.NotNull;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.org.hibernate.validator.constraints.NotBlank;
import com.dotcms.rest.api.Validated;

/**
 * Encapsulates the minimal information for the RestUser
 * @author Geoff M. Granum
 */
@JsonDeserialize(builder = UpdateUserForm.Builder.class)
public final class UpdateUserForm extends Validated  {

    @NotNull
    @NotBlank
    private final String userId;

    @NotNull
    @NotBlank
    private final String givenName;
    private final String email;

    @NotNull
    @NotBlank
    private final String surname;
    private final String password;


    private UpdateUserForm(UpdateUserForm.Builder builder) {
        userId    = builder.userId;
        givenName = builder.givenName;
        surname   = builder.surname;
        password  = builder.password;
        email     = builder.email;
        checkValid();
    }

    public String getUserId() {
        return userId;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getEmail() {
        return email;
    }

    public String getSurname() {
        return surname;
    }

    public String getPassword() {
        return password;
    }

    public static final class Builder {
        @JsonProperty private String userId;
        @JsonProperty private String givenName;
        @JsonProperty private String surname;
        @JsonProperty private String password;
        @JsonProperty private String email;

        public Builder() {
        }

        public UpdateUserForm.Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public UpdateUserForm.Builder givenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public UpdateUserForm.Builder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public UpdateUserForm.Builder password(String password) {
            this.password = password;
            return this;
        }

        public UpdateUserForm.Builder email(String email) {
            this.email = email;
            return this;
        }

        public UpdateUserForm build() {
            return new UpdateUserForm(this);
        }
    }
}
 
