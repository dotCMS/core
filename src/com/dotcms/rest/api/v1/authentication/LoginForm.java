package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.List;

@JsonDeserialize(builder = LoginForm.Builder.class)
public class LoginForm implements Serializable {

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

    private LoginForm(Builder builder) {

        this.language = builder.language;
        this.country  = builder.country;
        this.messagesKey = builder.messagesKey;
    }

    public static final class Builder {
        @JsonProperty private String language; // not present on create
        @JsonProperty private String country;
        @JsonProperty private List<String> messagesKey;

        public LoginForm.Builder language(String language) {
            this.language = language;
            return this;
        }

        public LoginForm.Builder country(String country) {
            this.country = country;
            return this;
        }

        public LoginForm.Builder messagesKey(List<String> messagesKey) {
            this.messagesKey = messagesKey;
            return this;
        }

        public LoginForm build() {
            return new LoginForm(this);
        }
    }
} // E:O:F:LoginForm.
