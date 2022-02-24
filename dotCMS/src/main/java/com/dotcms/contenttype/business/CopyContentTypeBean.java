package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This bean contains the information to do creation of a new type based on the copy of an existing content type
 * @author jsanca
 */
public class CopyContentTypeBean {

    private final ContentType sourceContentType;

    private final String name;

    private final String newVariable;

    private final String folder;

    private final String host;

    private final String icon;

    public ContentType getSourceContentType() {
        return sourceContentType;
    }

    public String getName() {
        return name;
    }

    public String getNewVariable() {
        return newVariable;
    }

    public String getFolder() {
        return folder;
    }

    public String getHost() {
        return host;
    }

    public String getIcon() {
        return icon;
    }

    private CopyContentTypeBean(final Builder builder) {

        this.sourceContentType = builder.sourceContentType;
        this.name         = builder.name;
        this.newVariable  = builder.newVariable;
        this.folder       = builder.folder;
        this.host         = builder.host;
        this.icon         = builder.icon;
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String name;

        @JsonProperty()
        private ContentType sourceContentType;

        @JsonProperty()
        private String newVariable;

        @JsonProperty()
        private String folder;

        @JsonProperty()
        private String host;

        @JsonProperty()
        private String icon;


        public Builder name(String name) {

            this.name = name;
            return this;
        }

        public Builder sourceContentType(ContentType sourceContentType) {

            this.sourceContentType = sourceContentType;
            return this;
        }

        public Builder newVariable(String newVariable) {

            this.newVariable = newVariable;
            return this;
        }

        public Builder folder(String folder) {

            this.folder = folder;
            return this;
        }

        public Builder host(String host) {

            this.host = host;
            return this;
        }

        public Builder icon(String icon) {

            this.icon = icon;
            return this;
        }
        public CopyContentTypeBean build() {

            return new CopyContentTypeBean(this);
        }
    }
}
