package com.dotmarketing.portlets.categories.model;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a HierarchyCategory but just include the parentList and the inode
 */
public class HierarchyShortCategory implements Serializable {
    private final String inode;
    private final String name;
    private final String key;
    private final List<ShortCategory> parentList;

    public HierarchyShortCategory(final Builder builder) {
        this.inode = builder.inode;
        this.parentList = builder.parentList;
        this.name = builder.name;
        this.key = builder.key;
    }

    public String getInode() {
        return inode;
    }

    public List<ShortCategory> getParentList() {
        return parentList;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public static class Builder {
        private String inode;
        private String name;

        private String key;
        private List<ShortCategory> parentList;

        public Builder setInode(String inode) {
            this.inode = inode;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setParentList(List<ShortCategory> parentList) {
            this.parentList = parentList;
            return this;
        }

        public HierarchyShortCategory build(){
            return new HierarchyShortCategory(this);
        }
    }
}
