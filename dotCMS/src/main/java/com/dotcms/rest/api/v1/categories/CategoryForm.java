package com.dotcms.rest.api.v1.categories;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Category Input Form
 *
 * @author Hassan Mustafa Baig
 */
@JsonDeserialize(builder = CategoryForm.Builder.class)
public class CategoryForm extends Validated {

    private String siteId;
    private String inode;
    private String parent;
    private String description;
    private String keywords;
    private String key;
    @NotNull
    private String categoryName;
    private boolean active;
    private int sortOrder;
    private String categoryVelocityVarName;

    private CategoryForm(final Builder builder) {

        this.siteId = builder.siteId;
        this.inode = builder.inode;
        this.parent = builder.parent;
        this.description = builder.description;
        this.keywords = builder.keywords;
        this.key = builder.key;
        this.categoryName = builder.categoryName;
        this.active = builder.active;
        this.sortOrder = builder.sortOrder;
        this.categoryVelocityVarName = builder.categoryVelocityVarName;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getInode() {
        return inode;
    }

    public String getParent() {
        return parent;
    }

    public String getDescription() {
        return description;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getKey() {
        return key;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getCategoryVelocityVarName() {
        return categoryVelocityVarName;
    }

    public static final class Builder {

        @JsonProperty
        private String siteId;
        @JsonProperty
        private String inode;
        @JsonProperty
        private String parent;
        @JsonProperty
        private String description;
        @JsonProperty
        private String keywords;
        @JsonProperty
        private String key;
        @JsonProperty
        private String categoryName;
        @JsonProperty
        private boolean active;
        @JsonProperty
        private int sortOrder;
        @JsonProperty
        private String categoryVelocityVarName;

        public Builder siteId(final String siteId) {

            this.siteId = siteId;
            return this;
        }

        public Builder setInode(final String inode) {
            this.inode = inode;
            return this;
        }

        public Builder setParent(final String parent) {
            this.parent = parent;
            return this;
        }

        public Builder setDescription(final String description) {
            this.description = description;
            return this;
        }

        public Builder setKeywords(final String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder setKey(final String key) {
            this.key = key;
            return this;
        }

        public Builder setCategoryName(final String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public Builder setActive(final boolean active) {
            this.active = active;
            return this;
        }

        public Builder setSortOrder(final int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder setCategoryVelocityVarName(final String categoryVelocityVarName) {
            this.categoryVelocityVarName = categoryVelocityVarName;
            return this;
        }

        public CategoryForm build() {

            return new CategoryForm(this);
        }
    }
}
