package com.dotmarketing.portlets.categories.model;

/**
 * Represents a {@link Category}, but only contains the most important data:
 *
 * - Category's name
 * - Category's key
 * - Category's inode
 */
public class ShortCategory {

    private String categoryName;
    private String inode;
    private String key;

    private ShortCategory(final Builder builder) {
        this.categoryName = builder.categoryName;
        this.inode = builder.inode;
        this.key = builder.key;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getInode() {
        return inode;
    }

    public String getKey() {
        return key;
    }

    public static class Builder {
        private String categoryName;
        private String inode;
        private String key;

        public Builder setCategoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public Builder setInode(String inode) {
            this.inode = inode;
            return this;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public ShortCategory build() {
            return new ShortCategory(this);
        }
    }
}
