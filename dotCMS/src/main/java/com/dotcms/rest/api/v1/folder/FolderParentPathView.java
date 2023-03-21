package com.dotcms.rest.api.v1.folder;

import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;

/**
 * This class holds a view with all attributes of com.dotmarketing.portlets.folders.model.Folder
 * plus a list to hold the children of a folder
 *
 *
 */
public class FolderParentPathView {

    private final String path;
    private final Date iDate;
    private final String hostId;
    private final String identifier;
    private final String inode;
    private final Date modDate;
    private final String name;
    private final Integer sortOrder;
    private final String title;
    private final String type;

    public FolderParentPathView(final Folder folder){
        this.name = folder.getName();
        this.iDate = folder.getIDate();
        this.hostId = folder.getHostId();
        this.identifier = folder.getIdentifier();
        this.inode = folder.getInode();
        this.modDate = folder.getModDate();
        this.sortOrder = folder.getSortOrder();
        this.title = folder.getTitle();
        this.type = folder.getType();
        this.path = folder.getPath();
    }

    public Date getiDate() {
        return iDate;
    }

    public String getHostId() {
        return hostId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getInode() {
        return inode;
    }

    public Date getModDate() {
        return modDate;
    }

    public String getName() {
        return name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }
}
