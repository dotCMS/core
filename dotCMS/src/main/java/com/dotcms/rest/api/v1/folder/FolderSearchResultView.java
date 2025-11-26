package com.dotcms.rest.api.v1.folder;

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

    public FolderSearchResultView(final String id, final String inode, final String path,
                                  final String hostname, final boolean addChildrenAllowed) {
        this.id = id;
        this.inode = inode;
        this.path = path;
        this.hostName = hostname;
        this.addChildrenAllowed = addChildrenAllowed;
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

}
