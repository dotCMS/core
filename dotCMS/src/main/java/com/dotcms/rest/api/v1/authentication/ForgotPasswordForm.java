package com.dotcms.rest.api.v1.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = ForgotPasswordForm.Builder.class)
public class ForgotPasswordForm extends Validated {

    @NotNull
    private final String userId;

    public String getUserId() {
        return userId;
    }

    private ForgotPasswordForm(Builder builder) {
        userId = builder.userId;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty(required = true) private String userId; // not present on create

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public ForgotPasswordForm build() {
            return new ForgotPasswordForm(this);
        }
    }
}

