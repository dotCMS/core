package com.dotcms.rest.api.v1.folder;

/**
 * View of the Folder for the move actionlet.
 */
public class FolderSearchResultView {

    private final String path;
    private final String hostName;
    private final boolean addChildrenAllowed;

    public FolderSearchResultView(final String path, final String hostname, final boolean addChildrenAllowed) {
        this.path = path;
        this.hostName = hostname;
        this.addChildrenAllowed = addChildrenAllowed;
    }


    public String getPath() {
        return path;
    }


    public String getHostName() {
        return hostName;
    }

    public boolean isAddChildrenAllowed(){ return addChildrenAllowed; }
}
