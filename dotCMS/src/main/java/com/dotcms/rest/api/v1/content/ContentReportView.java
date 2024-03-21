package com.dotcms.rest.api.v1.content;

import java.io.Serializable;

/**
 * Exposes all the properties of a Content Report View that will be displayed to the User. In its
 * most basic form, this report indicates the User what Content Types live under a given dotCMS
 * object -- i.e., a Site, a Folder, etc. -- and how many entries of each Content Type live under it
 * or associated to it.
 * <p>This report can be modified by new functionality, as long as all the implementations of
 * Content Reports can work with it.</p>
 *
 * @author Jose Castro
 * @since Mar 7th, 2024
 */
public class ContentReportView implements Serializable {

    final String contentTypeName;
    final long entries;

    /**
     * Private constructor for creating an instance of the {@link ContentReportView} object.
     *
     * @param builder The Builder instance.
     */
    private ContentReportView(final Builder builder) {
        this.contentTypeName = builder.contentTypeName;
        this.entries = builder.entries;
    }

    /**
     * Returns the name of the Content Type.
     *
     * @return The name of the Content Type.
     */
    public String getContentTypeName() {
        return contentTypeName;
    }

    /**
     * Returns the total number of contentlets of this Content Type.
     *
     * @return The total number of contentlets of this Content Type.
     */
    public long getEntries() {
        return entries;
    }

    /**
     * Builder for the {@link ContentReportView} class.
     */
    public static final class Builder {
        String contentTypeName;
        long entries;

        public Builder() {
            // Default Builder constructor
        }

        /**
         * Sets the name of the Content Type.
         *
         * @param contentTypeName The name of the Content Type.
         *
         * @return The Builder instance.
         */
        public Builder contentTypeName(String contentTypeName) {
            this.contentTypeName = contentTypeName;
            return this;
        }

        /**
         * Sets the total number of contentlets of this Content Type.
         *
         * @param numberOfEntries The total number of contentlets of this Content Type.
         *
         * @return The Builder instance.
         */
        public Builder entries(long numberOfEntries) {
            this.entries = numberOfEntries;
            return this;
        }

        /**
         * Creates an instance of the {@link ContentReportView} object.
         *
         * @return An instance of the {@link ContentReportView} object.
         */
        public ContentReportView build() {
            return new ContentReportView(this);
        }

    }

}
