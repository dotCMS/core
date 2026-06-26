package com.dotcms.rest.api.v1.a11yagent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Request body for {@code POST /api/v1/a11y-agent/fix} and {@code POST /api/v1/a11y-agent/fix/stream}.
 *
 * <p>The proxy resolves the identifier to a live URL, URI, and hostId before forwarding to the
 * agent service — the agent receives a fully-resolved payload (plan §8.2) and never performs
 * its own page resolution.
 */
@JsonDeserialize(builder = A11yAgentFixForm.Builder.class)
public class A11yAgentFixForm {

    private final String identifier;
    private final int languageId;
    private final boolean skipCss;

    private A11yAgentFixForm(final Builder builder) {
        this.identifier = builder.identifier;
        this.languageId = builder.languageId;
        this.skipCss = builder.skipCss;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getLanguageId() {
        return languageId;
    }

    public boolean isSkipCss() {
        return skipCss;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder {

        @JsonProperty(value = "identifier", required = true)
        private String identifier;

        @JsonProperty("languageId")
        private int languageId = 1;

        @JsonProperty("skipCss")
        private boolean skipCss = false;

        public Builder identifier(final String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder languageId(final int languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder skipCss(final boolean skipCss) {
            this.skipCss = skipCss;
            return this;
        }

        public A11yAgentFixForm build() {
            return new A11yAgentFixForm(this);
        }
    }
}
