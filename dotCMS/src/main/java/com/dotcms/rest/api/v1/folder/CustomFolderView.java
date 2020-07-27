package com.dotcms.rest.api.v1.folder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.Date;
import java.util.List;

/**
 * This class holds a view with all attributes of com.dotmarketing.portlets.folders.model.Folder
 * plus a list to hold the children of a folder
 *
 * @author Luis Bacca
 *
 */
@JsonPropertyOrder({ "path", "customFolders" })
@JsonRootName("folder")
public class CustomFolderView {

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
    private List<CustomFolderView> customFolders;

    public CustomFolderView(String path, String defaultFileType, String filesMasks, Date iDate,
                            String hostId, String identifier, String inode, Date modDate, String name,
                            Boolean showOnMenu, Integer sortOrder, String title, String type){
        this.path = path;
        this.defaultFileType = defaultFileType;
        this.filesMasks = filesMasks;
        this.iDate = iDate;
        this.hostId = hostId;
        this.identifier = identifier;
        this.inode = inode;
        this.modDate = modDate;
        this.name = name;
        this.showOnMenu = showOnMenu;
        this.sortOrder = sortOrder;
        this.title = title;
        this.type = type;
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

    @JsonProperty("children")
    public List<CustomFolderView> getCustomFolders() {
        return customFolders;
    }

    public void setCustomFolders(List<CustomFolderView> customFolders) {
        this.customFolders = customFolders;
    }
}
