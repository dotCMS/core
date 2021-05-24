package com.dotcms.rest.api.v1.browser;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Form to set the open folder on the site browser.
 * @author jsanca
 */
@JsonDeserialize(builder = OpenFolderForm.Builder.class)
public class OpenFolderForm {

    private final String path;

    public OpenFolderForm(final Builder builder) {
        this.path = builder.path;
    }

    public String getPath() {
        return path;
    }

    public static final class Builder {

        @JsonProperty
        private String path   = "/";

        private Builder() {}

        public Builder path(final String path) {
            this.path = path;
            return this;
        }

        public OpenFolderForm build() {
            return new OpenFolderForm(this);
        }
    }
}
