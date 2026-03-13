package com.dotcms.rest.api.v1.folder;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the REST View of a Folder. It's used by several Endpoints and related
 * classes, such as the {@link FolderResource}, the
 * {@link com.dotmarketing.portlets.workflows.actionlet.MoveContentActionlet}, and so on.
 *
 * @author Jonathan Sanchez
 * @since Jul 19th, 2021
 */
public class FolderSearchResultView {

    private final String id;
    private final String inode;
    private final String path;
    private final String hostName;
    private final boolean addChildrenAllowed;

    /**
     * {@code true} when this entry represents a nested host (sub-site) rather than a regular
     * folder. The front-end uses this flag to render a host/globe icon instead of a folder icon.
     */
    private final boolean isHost;

    /**
     * Backward-compatible constructor that creates a regular folder entry ({@code isHost=false}).
     */
    public FolderSearchResultView(final String id, final String inode, final String path,
                                  final String hostname, final boolean addChildrenAllowed) {
        this(id, inode, path, hostname, addChildrenAllowed, false);
    }

    /**
     * Full constructor that allows specifying whether this entry represents a nested host.
     */
    public FolderSearchResultView(final String id, final String inode, final String path,
                                  final String hostname, final boolean addChildrenAllowed,
                                  final boolean isHost) {
        this.id = id;
        this.inode = inode;
        this.path = path;
        this.hostName = hostname;
        this.addChildrenAllowed = addChildrenAllowed;
        this.isHost = isHost;
    }

    public String getId() {
        return id;
    }

    public String getInode() {
        return inode;
    }

    public String getPath() {
        return path;
    }

    public String getHostName() {
        return hostName;
    }

    public boolean isAddChildrenAllowed(){ return addChildrenAllowed; }

    /**
     * Returns {@code true} if this entry represents a nested host (sub-site) rather than a
     * regular folder.
     * <p>
     * The explicit {@code @JsonProperty} prevents Jackson from stripping the {@code is} prefix
     * and serializing this as {@code "host"} — keeping the JSON key as {@code "isHost"} to match
     * the TypeScript {@code DotFolder} interface.
     */
    @JsonProperty("isHost")
    public boolean isHost() {
        return isHost;
    }

}
