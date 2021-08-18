package com.dotmarketing.beans;

import java.util.Date;

/**
 * This class is for maintaining backwards compatibility with old Bundles containing objects of classes
 * that used to extend from {@link Inode}. E.g. {@link com.dotmarketing.portlets.structure.model.Relationship}
 */

@Deprecated
public class LegacyInode {
    private Date iDate;
    private String type;
    private String owner;

    public Date getiDate() {
        return iDate;
    }

    public void setiDate(Date iDate) {
        this.iDate = iDate;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }
}
