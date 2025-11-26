package com.dotcms.rest.api.v1.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

@JsonDeserialize(builder = LoginAsForm.Builder.class)
public class LoginAsForm {

    @NotNull
    @Length(min = 2, max = 100)
    private final String userId;

    @NotNull
    private final String password;

    private LoginAsForm(LoginAsForm.Builder builder) {
        userId    = builder.userId;
        password  = builder.password;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public static final class Builder {
        @JsonProperty
        private String userId;
        @JsonProperty private String password;

        public Builder() {
        }

        public LoginAsForm.Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public LoginAsForm.Builder password(String password) {
            this.password = password;
            return this;
        }

        public LoginAsForm build() {
            return new LoginAsForm(this);
        }
    }
}
