package com.dotcms.rest.api.v1.folder;

/**
 * View of the Folder for the move actionlet.
 */
public class FolderSearchResultView {

    private final String path;
    private final String hostname;

    public FolderSearchResultView(String path, String hostname) {
        this.path = path;
        this.hostname = hostname;
    }


    public String getPath() {
        return path;
    }

    public String getHostname() {
        return hostname;
    }
}
