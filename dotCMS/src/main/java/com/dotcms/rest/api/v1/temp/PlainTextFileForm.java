package com.dotcms.rest.api.v1.temp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Provides all the required information of a File Asset that is being provided as plain text. For instance, this can
 * be used by the Temp File Resource to create a file with the specified contents, which is what allows Users to create
 * a text file -- i.e, TXT, VTL, JS, CSS, and so on -- directly from the dotCMS back-end.
 *
 * @author Jose Castro
 * @since Feb 22nd, 2023
 */
@JsonDeserialize(builder = PlainTextFileForm.Builder.class)
public class PlainTextFileForm {

    private final String fileName;
    private final String fileContent;

    private PlainTextFileForm(final Builder builder) {
        this.fileName = builder.fileName;
        this.fileContent = builder.fileContent;
    }

    public String fileContent() {
        return fileContent;
    }

    public String fileName() {
        return fileName;
    }

    /**
     * Allows you to build an instance of the {@link PlainTextFileForm} class.
     */
    public static final class Builder {

        @JsonProperty(required = true)
        private String fileName;
        @JsonProperty(required = true)
        private String fileContent;

        /**
         * Sets the name of the plain text file.
         *
         * @param fileName The file name.
         *
         * @return An instance of the class' builder.
         */
        public PlainTextFileForm.Builder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * Sets the content of the plain text file.
         *
         * @param fileContent The file's content.
         *
         * @return An instance of the class' builder.
         */
        public PlainTextFileForm.Builder file(final String fileContent) {
            this.fileContent = fileContent;
            return this;
        }

        /**
         * Creates an instance of the {@link PlainTextFileForm} class.
         *
         * @return The instantiated class.
         */
        public PlainTextFileForm build() {
            return new PlainTextFileForm(this);
        }

    }

}
