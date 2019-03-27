package com.dotcms.rest.api.v1;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.List;

@JsonDeserialize(builder = I18NForm.Builder.class)
public class I18NForm implements Serializable {

    private final String language;

    private final String country;

    private final List<String> messagesKey;

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    public List<String> getMessagesKey() {
        return messagesKey;
    }

    private I18NForm(Builder builder) {

        this.language = builder.language;
        this.country  = builder.country;
        this.messagesKey = builder.messagesKey;
    }

    public static final class Builder {
        @JsonProperty private String language; // not present on create
        @JsonProperty private String country;
        @JsonProperty private List<String> messagesKey;

        public I18NForm.Builder language(String language) {
            this.language = language;
            return this;
        }

        public I18NForm.Builder country(String country) {
            this.country = country;
            return this;
        }

        public I18NForm.Builder messagesKey(List<String> messagesKey) {
            this.messagesKey = messagesKey;
            return this;
        }

        public I18NForm build() {
            return new I18NForm(this);
        }
    }
} // E:O:F:I18NForm.
