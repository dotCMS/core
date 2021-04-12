package com.dotcms.rest.api.v1.browser;

import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

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
