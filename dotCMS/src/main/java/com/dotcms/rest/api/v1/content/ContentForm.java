package com.dotcms.rest.api.v1.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

/**
 * Represents the data map of a Contentlet in dotCMS that is being passed down to the REST endpoint for processing.
 *
 * @author Jonathan Sanchez
 * @since Sep 6th, 2022
 */
@JsonDeserialize(builder = ContentForm.Builder.class)
public class ContentForm {

    private final Map<String, Object> contentlet;

    public ContentForm(final ContentForm.Builder builder) {
        this.contentlet = builder.contentlet;
    }

    public Map<String, Object> getContentlet() {
        return this.contentlet;
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
