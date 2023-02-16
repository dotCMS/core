package com.dotcms.rest.api.v1.categories;

import java.util.Date;
/**
 * Data transferring object for any category
 *
 * @author Hassan Mustafa Baig
 */
public class CategoryView {

    private final String inode;
    private final String parent;
    private final String description;
    private final String keywords;
    private final String key;
    private final String categoryName;
    private final boolean active;
    private final int sortOrder;
    private final String categoryVelocityVarName;
    private final Date modDate;

    private CategoryView(final Builder builder) {

        this.inode = builder.inode;
        this.parent = builder.parent;
        this.description = builder.description;
        this.keywords = builder.keywords;
        this.key = builder.key;
        this.categoryName = builder.categoryName;
        this.active = builder.active;
        this.sortOrder = builder.sortOrder;
        this.categoryVelocityVarName = builder.categoryVelocityVarName;
        this.modDate = builder.modDate;
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

    public Date getModDate() {
        return modDate;
    }

    public static final class Builder {

        private String inode;
        private String parent;
        private String description;
        private String keywords;
        private String key;
        private String categoryName;
        private boolean active;
        private int sortOrder;
        private String categoryVelocityVarName;
        private Date modDate;
        private Integer childrenCount;

        public Builder inode(final String inode) {
            this.inode = inode;
            return this;
        }

        public Builder parent(final String parent) {
            this.parent = parent;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder keywords(final String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder key(final String key) {
            this.key = key;
            return this;
        }

        public Builder categoryName(final String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public Builder active(final boolean active) {
            this.active = active;
            return this;
        }

        public Builder sortOrder(final int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder categoryVelocityVarName(final String categoryVelocityVarName) {
            this.categoryVelocityVarName = categoryVelocityVarName;
            return this;
        }

        public Builder modDate(final Date modDate) {
            this.modDate = modDate;
            return this;
        }

        public CategoryView build() {

            return new CategoryView(this);
        }
    }
}
