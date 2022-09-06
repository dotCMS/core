package com.dotcms.rest.api.v1.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(builder = ContentForm.Builder.class)
public class ContentForm {

    private final Map<String, Object> contentlet;

    public ContentForm(final ContentForm.Builder builder) {

        this.contentlet    = builder.contentlet;
    }

    public Map<String, Object> getContentlet() {
        return contentlet;
    }

    public static class Builder {

        @JsonProperty("contentlet")
        private Map<String, Object> contentlet;

        public ContentForm.Builder contentlet(final  Map<String, Object> contentletFormData) {
            this.contentlet = contentletFormData;
            return this;
        }

        public ContentForm build() {
            return new ContentForm(this);
        }
    }
}
