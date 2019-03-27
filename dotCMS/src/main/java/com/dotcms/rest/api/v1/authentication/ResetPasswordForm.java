package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;
import com.dotmarketing.util.Config;

@JsonDeserialize(builder = ResetPasswordForm.Builder.class)
public class ResetPasswordForm extends Validated {

    @NotNull
    private final String token;

    @NotNull
    private final String password;

    public String getToken() {
        return token;
    }

    public String getPassword() {
        return password;
    }

    private ResetPasswordForm(Builder builder) {
        token = builder.token;
        password = builder.password;

        checkValid();
    }

    public static final class Builder {
        @JsonProperty(required = true) private String token;
        @JsonProperty(required = true) private String password;


        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public ResetPasswordForm build() {
            return new ResetPasswordForm(this);
        }
    }
}
