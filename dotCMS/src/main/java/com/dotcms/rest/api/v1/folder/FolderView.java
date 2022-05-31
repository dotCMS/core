package com.dotcms.rest.api.v1.folder;

import com.dotmarketing.portlets.folders.model.Folder;
import java.util.Date;
import java.util.List;

/**
 * This class holds a view with all attributes of com.dotmarketing.portlets.folders.model.Folder
 * plus a list to hold the children of a folder
 *
 *
 */
public class FolderView {

    private final String path;
    private final String defaultFileType;
    private final String filesMasks;
    private final Date iDate;
    private final String hostId;
    private final String identifier;
    private final String inode;
    private final Date modDate;
    private final String name;
    private final Boolean showOnMenu;
    private final Integer sortOrder;
    private final String title;
    private final String type;
    private final List<FolderView> subFolders;

    public FolderView(final Folder folder, final List<FolderView> subFolders){
        this.name = folder.getName();
        this.defaultFileType = folder.getDefaultFileType();
        this.iDate = folder.getIDate();
        this.hostId = folder.getHostId();
        this.identifier = folder.getIdentifier();
        this.inode = folder.getInode();
        this.modDate = folder.getModDate();
        this.showOnMenu = folder.isShowOnMenu();
        this.sortOrder = folder.getSortOrder();
        this.title = folder.getTitle();
        this.type = folder.getType();
        this.filesMasks = folder.getFilesMasks();
        this.path = folder.getPath();
        this.subFolders = subFolders;
    }

    public String getDefaultFileType() {
        return defaultFileType;
    }

    public String getFilesMasks() {
        return filesMasks;
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

    public Boolean getShowOnMenu() {
        return showOnMenu;
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

    public List<FolderView> getSubFolders() {
        return subFolders;
    }
}
