package com.dotcms.rest.api.v1.page;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;

/**
 * Copy Contentlet Form
 * @author jsanca
 */
@JsonDeserialize(builder = CopyContentletForm.Builder.class)
public class CopyContentletForm {

    private final String contentId;
    private final String pageId;
    private final String containerId;
    private final String relationType;
    private final String personalization;
    private final String variantId;
    private final int treeOrder;
    private final Map<String, Object> styleProperties;

    private CopyContentletForm(final Builder builder) {
        this.contentId = builder.contentId;
        this.pageId = builder.pageId;
        this.containerId = builder.containerId;
        this.relationType = builder.relationType;
        this.personalization = builder.personalization;
        this.variantId = builder.variantId;
        this.treeOrder = builder.treeOrder;
        this.styleProperties = builder.styleProperties;
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

    public String getRelationType() {
        return relationType;
    }

    public String getPersonalization() {
        return personalization;
    }

    public String getVariantId() {
        return variantId;
    }

    public int getTreeOrder() {
        return treeOrder;
    }

    public Map<String, Object> getStyleProperties() {
        return styleProperties;
    }


    @Override
    public String toString() {
        return "CopyContentletForm{" +
                "contentId='" + contentId + '\'' +
                ", pageId='" + pageId + '\'' +
                ", containerId='" + containerId + '\'' +
                ", relationType='" + relationType + '\'' +
                ", personalization='" + personalization + '\'' +
                ", variantId='" + variantId + '\'' +
                ", treeOrder=" + treeOrder + '\'' +
                ", styleProperties=" + styleProperties +
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
        private String relationType;

        @JsonProperty
        private String personalization;

        @JsonProperty
        private String variantId;

        @JsonProperty
        private int treeOrder;

        @JsonProperty
        private Map<String, Object> styleProperties;

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

        public Builder relationType(String relationType) {
            this.relationType = relationType;
            return this;
        }

        public Builder personalization(String personalization) {
            this.personalization = personalization;
            return this;
        }

        public Builder variantId(String variantId) {
            this.variantId = variantId;
            return this;
        }

        public Builder treeOrder(int treeOrder) {
            this.treeOrder = treeOrder;
            return this;
        }

        public Builder styleProperties(Map<String, Object> styleProperties) {
            this.styleProperties = styleProperties;
            return this;
        }

        public CopyContentletForm build() {
            return new CopyContentletForm(this);
        }
    }
}
