package com.dotcms.rest.api.v2.languages;

import com.dotcms.rest.api.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = LanguageForm.Builder.class)
public class LanguageForm extends Validated {

    private final String languageCode;

    private final String countryCode;

    private final String language;

    private final String country;

    private final String isoCode;

    public LanguageForm(final LanguageForm.Builder builder) {
        this.languageCode = builder.languageCode;
        this.countryCode = builder.countryCode;
        this.language = builder.language;
        this.country = builder.country;
        this.isoCode = builder.isoCode;
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

    public String getIsoCode() {
        return isoCode;
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

        @JsonProperty
        private String isoCode;

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

        public Builder isoCode(final String isoCode) {
            this.isoCode = isoCode;
            return this;
        }

        public LanguageForm build() {
            return new LanguageForm(this);
        }

    }
}
