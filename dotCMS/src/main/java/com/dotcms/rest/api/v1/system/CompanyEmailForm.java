package com.dotcms.rest.api.v1.system;

import com.dotcms.repackage.javax.validation.constraints.Pattern.Flag;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.javax.validation.constraints.Pattern;

public class CompanyEmailForm extends Validated {

    //This regex should be able to capture anything like this: dotCMS Website <website@dotcms.com>
    //Where (dotCMS Website) is optional as well as the use of <..>
    public static final String SENDER_NAME_EMAIL_REGEX = "((\\s*?)(\\w*?)(\\s*?))*?(\\<*[a-zA-Z0-9._-]+\\@[a-zA-Z0-9._-]+\\>*)$";

    @NotNull
    @Pattern(regexp = SENDER_NAME_EMAIL_REGEX, flags = {Flag.CASE_INSENSITIVE}, message = "input does not match a valid e-mail pattern.")
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
