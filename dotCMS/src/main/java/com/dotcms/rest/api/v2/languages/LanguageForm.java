package com.dotcms.rest.api.v2.languages;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.javax.validation.constraints.Size;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = LanguageForm.Builder.class)
public class LanguageForm extends Validated {

    @NotNull
    @Size(min = 2, max = 100)
    private final String languageCode;

    private final String countryCode;

    private final String language;

    private final String country;

    public LanguageForm(final LanguageForm.Builder builder) {
        this.languageCode = builder.languageCode;
        this.countryCode = builder.countryCode;
        this.language = builder.language;
        this.country = builder.country;
        this.checkValid();
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    public static final class Builder {

        @JsonProperty
        private String languageCode;

        @JsonProperty
        private String countryCode;

        @JsonProperty
        private String language;

        @JsonProperty
        private String country;

        public Builder languageCode(final String languageCode) {
            this.languageCode = languageCode;
            return this;
        }

        public Builder countryCode(final String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder language(final String language) {
            this.language = language;
            return this;
        }

        public Builder country(final String country) {
            this.country = country;
            return this;
        }

        public LanguageForm build() {
            return new LanguageForm(this);
        }

    }
}
