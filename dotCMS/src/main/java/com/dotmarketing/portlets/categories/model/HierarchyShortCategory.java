package com.dotmarketing.portlets.categories.model;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a HierarchyCategory but just include the parentList and the inode
 */
public class HierarchyShortCategory implements Serializable {
    private final String inode;
    private final String categoryName;
    private final List<ShortCategory> parentList;

    public HierarchyShortCategory(final String inode, final String categoryName, final List<ShortCategory> parentList) {
        this.inode = inode;
        this.parentList = parentList;
        this.categoryName = categoryName;
    }

    public String getInode() {
        return inode;
    }

    public List<ShortCategory> getParentList() {
        return parentList;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
