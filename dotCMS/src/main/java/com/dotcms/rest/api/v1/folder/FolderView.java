package com.dotcms.rest.api.v1.folder;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

/**
 * This class holds a view with all attributes of com.dotmarketing.portlets.folders.model.Folder
 * plus a list to hold the children of a folder.
 *
 * <p>When a nested host appears as a child node in the folder tree, {@link #isHost()} returns
 * {@code true} and {@link #getHostname()} returns the hostname string. All other fields are
 * populated from the {@link Host} contentlet in the same way they would be for a folder.
 */
public class FolderView {

    @JsonIgnore
    private final Folder folder;
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

    /** {@code true} when this node represents a nested host rather than a plain folder. */
    private final boolean isHost;

    /**
     * The hostname of the nested host, or {@code null} when this node represents a plain folder.
     */
    private final String hostname;

    public FolderView(final Folder folder, final List<FolderView> subFolders){
        this.folder = folder;
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
        this.isHost = false;
        this.hostname = null;
    }

    /**
     * Constructs a {@link FolderView} that represents a <em>nested host</em> node in the folder
     * tree. These nodes have {@link #isHost()} == {@code true} and carry the host's
     * {@link #getHostname()} for display in the tree UI.
     *
     * @param nestedHost  the nested-host contentlet to represent as a tree node
     * @param parentHostId the identifier of the immediate parent host (used as {@link #getHostId()})
     * @param nodePath    the path of this host node within the parent host's folder hierarchy
     *                    (e.g. {@code "/"}), as stored in the {@code Identifier.parent_path} column
     * @param subFolders  child folder and nested-host nodes belonging to this host
     */
    public FolderView(final Host nestedHost,
                      final String parentHostId,
                      final String nodePath,
                      final List<FolderView> subFolders) {
        this.folder = null;
        this.name = nestedHost.getHostname();
        this.hostname = nestedHost.getHostname();
        this.identifier = nestedHost.getIdentifier();
        this.inode = nestedHost.getInode();
        this.hostId = parentHostId;
        this.path = nodePath;
        this.title = nestedHost.getTitle();
        this.iDate = nestedHost.getModDate();
        this.modDate = nestedHost.getModDate();
        this.subFolders = subFolders;
        this.isHost = true;
        // Fields that do not apply to host nodes
        this.defaultFileType = null;
        this.filesMasks = null;
        this.showOnMenu = null;
        this.sortOrder = null;
        this.type = Host.HOST_VELOCITY_VAR_NAME;
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

    /**
     * Returns {@code true} when this node represents a nested host rather than a plain folder.
     * The Angular tree UI uses this flag to render a site-icon instead of a folder-icon.
     * <p>
     * {@code @JsonProperty} keeps the serialized key as {@code "isHost"} — without it Jackson
     * strips the {@code is} prefix and emits {@code "host"}, which the TypeScript interface does
     * not recognise.
     */
    @JsonProperty("isHost")
    public boolean isHost() {
        return isHost;
    }

    /**
     * Returns the hostname of the nested host represented by this node, or {@code null} when this
     * node represents a plain folder.
     */
    public String getHostname() {
        return hostname;
    }

    @JsonIgnore
    public Folder getFolder() {
        return folder;
    }
}
