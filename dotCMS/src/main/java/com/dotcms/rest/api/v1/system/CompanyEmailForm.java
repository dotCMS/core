package com.dotcms.rest.api.v1.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

public class CompanyEmailForm extends Validated {

    @NotNull
    private final String email;

    @JsonCreator
    public CompanyEmailForm(@JsonProperty("email") final String email) {
        super();
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "CompanyEmailForm{" +
                "email='" + email + '\'' +
                '}';
    }
}
