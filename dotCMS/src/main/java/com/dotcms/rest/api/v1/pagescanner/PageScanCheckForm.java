package com.dotcms.rest.api.v1.pagescanner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Request body for the Page Scanner check endpoints.
 */
@JsonDeserialize(builder = PageScanCheckForm.Builder.class)
public class PageScanCheckForm {

    private final String url;

    private PageScanCheckForm(final Builder builder) {
        this.url = builder.url;
    }

    public String getUrl() {
        return url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder {

        @JsonProperty(value = "url", required = true)
        private String url;

        public Builder url(final String url) {
            this.url = url;
            return this;
        }

        public PageScanCheckForm build() {
            return new PageScanCheckForm(this);
        }
    }
}
