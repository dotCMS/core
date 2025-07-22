package com.dotcms.rest.api.v1.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;
import com.dotcms.rest.api.Validated;

/**
 * Encapsulates the minimal information for the RestUser
 * @author Geoff M. Granum
 */
@JsonDeserialize(builder = UpdateCurrentUserForm.Builder.class)
public final class UpdateCurrentUserForm extends Validated  {

    @NotBlank
    private final String userId;

    @NotBlank
    private final String givenName;
    private final String email;

    @NotBlank
    private final String surname;
    private final String currentPassword;
    private final String newPassword;

    private UpdateCurrentUserForm(UpdateCurrentUserForm.Builder builder) {
        userId    = builder.userId;
        givenName = builder.givenName;
        surname   = builder.surname;
        currentPassword  = builder.currentPassword;
        email     = builder.email;
        newPassword = builder.newPassword;

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

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public static final class Builder {
        @JsonProperty private String userId;
        @JsonProperty private String givenName;
        @JsonProperty private String surname;
        @JsonProperty private String currentPassword;
        @JsonProperty private String newPassword;
        @JsonProperty private String email;

        public Builder() {
        }

        public UpdateCurrentUserForm.Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public UpdateCurrentUserForm.Builder givenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public UpdateCurrentUserForm.Builder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public UpdateCurrentUserForm.Builder currentPassword(String password) {
            this.currentPassword = password;
            return this;
        }

        public UpdateCurrentUserForm.Builder newPassword(String newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        public UpdateCurrentUserForm.Builder email(String email) {
            this.email = email;
            return this;
        }

        public UpdateCurrentUserForm build() {
            return new UpdateCurrentUserForm(this);
        }
    }
}
 
