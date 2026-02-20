package com.dotcms.rest.api.v1.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form for saving company locale information (language and timezone).
 *
 * @author hassandotcms
 */
public class CompanyLocaleForm {

    private final String languageId;
    private final String timeZoneId;

    @JsonCreator
    public CompanyLocaleForm(
            @JsonProperty("languageId") final String languageId,
            @JsonProperty("timeZoneId") final String timeZoneId) {
        this.languageId = languageId;
        this.timeZoneId = timeZoneId;
    }

    public String getLanguageId() {
        return languageId;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    @Override
    public String toString() {
        return "CompanyLocaleForm{" +
                "languageId='" + languageId + '\'' +
                ", timeZoneId='" + timeZoneId + '\'' +
                '}';
    }
}
