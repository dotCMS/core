package com.dotcms.rest.api.v1.company;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;
import java.util.Set;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.exception.BadRequestException;

/**
 * Form for saving company locale information (language and timezone).
 *
 * @author hassandotcms
 */
@Schema(description = "Company locale settings (language and timezone)")
public class CompanyLocaleForm extends Validated {

    private static final Set<String> VALID_LANGUAGES = Set.of(Locale.getISOLanguages());

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

        // Validate the language part is a known ISO 639 code
        final String languagePart = languageId.split("_")[0].toLowerCase();
        if (!VALID_LANGUAGES.contains(languagePart)) {
            throw new BadRequestException(
                    "Invalid languageId: '" + languageId
                            + "'. Language must be a valid ISO 639 code");
        }
    }

    public String getLanguageId() {
        return languageId;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }
}
