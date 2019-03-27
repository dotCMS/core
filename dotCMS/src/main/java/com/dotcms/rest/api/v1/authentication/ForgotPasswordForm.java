package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;

@JsonDeserialize(builder = ForgotPasswordForm.Builder.class)
public class ForgotPasswordForm extends Validated {

    @NotNull
    @Length(min = 2, max = 100)
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

