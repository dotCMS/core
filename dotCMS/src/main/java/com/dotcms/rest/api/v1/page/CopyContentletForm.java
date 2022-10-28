package com.dotcms.rest.api.v1.page;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Copy Contentlet Form
 * @author jsanca
 */
@JsonDeserialize(builder = CopyContentletForm.Builder.class)
public class CopyContentletForm {

    private final String contentId;
    private final String pageId;
    private final String containerId;
    private final String containerUUID;
    private final String personalizationKey;

    private CopyContentletForm(final Builder builder) {
        this.contentId = builder.contentId;
        this.pageId = builder.pageId;
        this.containerId = builder.containerId;
        this.containerUUID = builder.containerUUID;
        this.personalizationKey = builder.personalizationKey;
    }

    public String getContentId() {
        return contentId;
    }

    public String getPageId() {
        return pageId;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getContainerUUID() {
        return containerUUID;
    }

    public String getPersonalizationKey() {
        return personalizationKey;
    }

    @Override
    public String toString() {
        return "CopyContentletForm{" +
                "contentId='" + contentId + '\'' +
                ", pageId='" + pageId + '\'' +
                ", containerId='" + containerId + '\'' +
                ", containerUUID='" + containerUUID + '\'' +
                ", personalizationKey='" + personalizationKey + '\'' +
                '}';
    }

    public static final class Builder {
        @JsonProperty(required = true)
        private String contentId;

        @JsonProperty(required = true)
        private String pageId;

        @JsonProperty(required = true)
        private String containerId;

        @JsonProperty(required = true)
        private String containerUUID;

        @JsonProperty
        private String personalizationKey;

        public Builder contentId(String contentId) {
            this.contentId = contentId;
            return this;
        }

        public Builder pageId(String pageId) {
            this.pageId = pageId;
            return this;
        }

        public Builder containerId(String containerId) {
            this.containerId = containerId;
            return this;
        }

        public Builder containerUUID(String containerUUID) {
            this.containerUUID = containerUUID;
            return this;
        }

        public Builder personalizationKey(String personalizationKey) {
            this.personalizationKey = personalizationKey;
            return this;
        }

        public CopyContentletForm build() {
            return new CopyContentletForm(this);
        }
    }
}
