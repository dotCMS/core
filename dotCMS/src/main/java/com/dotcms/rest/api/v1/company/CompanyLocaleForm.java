package com.dotcms.rest.api.v1.company;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

/**
 * Form for saving company locale information (language and timezone).
 *
 * @author hassandotcms
 */
@Schema(description = "Company locale settings (language and timezone)")
public class CompanyLocaleForm extends Validated {

    @JsonProperty("languageId")
    @Schema(description = "Java locale string", example = "en_US",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "languageId is required")
    private final String languageId;

    @JsonProperty("timeZoneId")
    @Schema(description = "Java TimeZone ID", example = "America/New_York",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "timeZoneId is required")
    private final String timeZoneId;

    @JsonCreator
    public CompanyLocaleForm(
            @JsonProperty("languageId") final String languageId,
            @JsonProperty("timeZoneId") final String timeZoneId) {
        this.languageId = languageId;
        this.timeZoneId = timeZoneId;
    }

    @Override
    public void checkValid() {
        super.checkValid();

        if (!UtilMethods.isSet(languageId)) {
            throw new BadRequestException("languageId is required");
        }
        if (!UtilMethods.isSet(timeZoneId)) {
            throw new BadRequestException("timeZoneId is required");
        }
    }

    public String getLanguageId() {
        return languageId;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }
}
