package com.dotcms.rest.api.v1.temp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 *
 *
 * @author Jose Castro
 * @since
 */
@JsonDeserialize(builder = PlainTextFileForm.Builder.class)
public class PlainTextFileForm {

    private final String fileName;
    private final String fileContent;

    public PlainTextFileForm(final Builder builder) {
        this.fileName = builder.fileName;
        this.fileContent = builder.fileContent;
    }

    public String fileContent() {
        return fileContent;
    }

    public String fileName() {
        return fileName;
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String fileName;
        @JsonProperty(required = true)
        private String fileContent;

        public PlainTextFileForm.Builder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public PlainTextFileForm.Builder file(final String fileContent) {
            this.fileContent = fileContent;
            return this;
        }

        public PlainTextFileForm build() {
            return new PlainTextFileForm(this);
        }

    }

}
