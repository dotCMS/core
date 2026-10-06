package com.dotcms.rest.api.v1.company;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Form for saving company authentication type.
 *
 * @author hassandotcms
 */
@Schema(description = "Company authentication type setting")
public class CompanyAuthTypeForm extends Validated {

    @JsonProperty("authType")
    @Schema(description = "Authentication type: 'emailAddress' or 'userId'",
            example = "emailAddress",
            allowableValues = {"emailAddress", "userId"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final AuthType authType;

    @JsonCreator
    public CompanyAuthTypeForm(
            @JsonProperty("authType") final AuthType authType) {
        this.authType = authType;
    }

    @Override
    public void checkValid() {
        super.checkValid();
        if (authType == null) {
            throw new BadRequestException(
                    "Invalid or missing authType. Must be one of: emailAddress, userId");
        }
    }

    public AuthType getAuthType() {
        return authType;
    }
}
