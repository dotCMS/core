package com.dotmarketing.portlets.categories.model;

import java.io.Serializable;

/**
 * Represents a {@link Category}, but only contains the most important data:
 *
 * - Category's name
 * - Category's key
 * - Category's inode
 */
public class ShortCategory implements Serializable {

    private String name;
    private String inode;
    private String key;

    private ShortCategory(final Builder builder) {
        this.name = builder.name;
        this.inode = builder.inode;
        this.key = builder.key;
    }

    public String getName() {
        return name;
    }

    public String getInode() {
        return inode;
    }

    public String getKey() {
        return key;
    }

    public static class Builder {
        private String name;
        private String inode;
        private String key;

        public Builder setName(String name) {
            this.name = name;
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
