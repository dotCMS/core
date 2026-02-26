package com.dotcms.rest.api.v1.company;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import java.util.Set;

/**
 * Form for saving company authentication type.
 *
 * @author hassandotcms
 */
@Schema(description = "Company authentication type setting")
public class CompanyAuthTypeForm extends Validated {

    private static final Set<String> VALID_AUTH_TYPES = Set.of(
            Company.AUTH_TYPE_EA, Company.AUTH_TYPE_ID);

    @JsonProperty("authType")
    @Schema(description = "Authentication type: 'emailAddress' or 'userId'",
            example = "emailAddress",
            allowableValues = {"emailAddress", "userId"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "authType is required")
    private final String authType;

    @JsonCreator
    public CompanyAuthTypeForm(
            @JsonProperty("authType") final String authType) {
        this.authType = authType;
    }

    @Override
    public void checkValid() {
        super.checkValid();

        if (!UtilMethods.isSet(authType)) {
            throw new BadRequestException("authType is required");
        }
        if (!VALID_AUTH_TYPES.contains(authType)) {
            throw new BadRequestException(
                    "authType must be one of: " + String.join(", ", VALID_AUTH_TYPES));
        }
    }

    public String getAuthType() {
        return authType;
    }
}
