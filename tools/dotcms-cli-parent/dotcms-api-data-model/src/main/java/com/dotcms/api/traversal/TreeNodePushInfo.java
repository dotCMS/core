package com.dotcms.api.traversal;

/**
 * Information about a tree node used for traversal and the push process.
 */
public class TreeNodePushInfo {

    private int assetsToPushCount;
    private int assetsNewCount;
    private int assetsModifiedCount;
    private int assetsToDeleteCount;
    private int foldersToPushCount;
    private int foldersToDeleteCount;

    /**
     * Constructs a new TreeNodePushInfo object.
     */
    public TreeNodePushInfo() {
        this.assetsToPushCount = 0;
        this.assetsNewCount = 0;
        this.assetsModifiedCount = 0;
        this.assetsToDeleteCount = 0;
        this.foldersToPushCount = 0;
        this.foldersToDeleteCount = 0;
    }

    public int assetsToPushCount() {
        return this.assetsToPushCount;
    }

    public int assetsNewCount() {
        return this.assetsNewCount;
    }

    public int assetsModifiedCount() {
        return this.assetsModifiedCount;
    }

    public int assetsToDeleteCount() {
        return this.assetsToDeleteCount;
    }

    public int foldersToPushCount() {
        return this.foldersToPushCount;
    }

    public int foldersToDeleteCount() {
        return this.foldersToDeleteCount;
    }

    public void incrementAssetsToPushCount() {
        this.assetsToPushCount++;
    }

    public void incrementAssetsNewCount() {
        this.assetsNewCount++;
    }

    public void incrementAssetsModifiedCount() {
        this.assetsModifiedCount++;
    }

    public void incrementAssetsToDeleteCount() {
        this.assetsToDeleteCount++;
    }

    public void incrementFoldersToPushCount() {
        this.foldersToPushCount++;
    }

    public void incrementFoldersToDeleteCount() {
        this.foldersToDeleteCount++;
    }

    public boolean hasChanges() {
        return this.assetsToPushCount > 0 ||
                this.assetsToDeleteCount > 0 ||
                this.foldersToPushCount > 0 ||
                this.foldersToDeleteCount > 0;
    }
}
