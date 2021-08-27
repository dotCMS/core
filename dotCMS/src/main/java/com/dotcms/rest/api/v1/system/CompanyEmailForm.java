package com.dotcms.rest.api.v1.system;

import com.dotcms.repackage.javax.validation.constraints.Pattern.Flag;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.javax.validation.constraints.Pattern;

public class CompanyEmailForm extends Validated {

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
