package com.dotmarketing.portlets.categories.model;

import java.util.List;

/**
 * Represents a HierarchyCategory but just include the parentList and the inode
 */
public class HierarchyShortCategory {
    private String inode;
    private List<ShortCategory> parentList;

    public HierarchyShortCategory(final String inode, final List<ShortCategory> parentList) {
        this.inode = inode;
        this.parentList = parentList;
    }

    public String getInode() {
        return inode;
    }

    public List<ShortCategory> getParentList() {
        return parentList;
    }
}
