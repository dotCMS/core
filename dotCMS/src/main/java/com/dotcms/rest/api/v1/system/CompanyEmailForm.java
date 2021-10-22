package com.dotcms.rest.api.v1.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CompanyEmailForm {

    private final String senderAndEmail;

    @JsonCreator
    public CompanyEmailForm(@JsonProperty("senderAndEmail") final String email) {
        super();
        this.senderAndEmail = email;
    }

    public String getSenderAndEmail() {
        return senderAndEmail;
    }

    @Override
    public String toString() {
        return "CompanyEmailForm{" +
                "email='" + senderAndEmail + '\'' +
                '}';
    }
}
