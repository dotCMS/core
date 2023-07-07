package com.dotcms.api.traversal;

import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A node in a hierarchical tree representation of a file system directory. Each node represents a
 * folder and contains references to its child folders and files.
 * <p>
 * The class is annotated with {@code @JsonSerialize(using = TreeNodeSerializer.class)} to specify a
 * custom serializer for JSON serialization of TreeNode instances.
 */
public class TreeNode {

    private final FolderView folder;
    private List<TreeNode> children;
    private List<AssetView> assets;

    /**
     * Constructs a new TreeNode instance with the specified folder as its root node.
     *
     * @param folder The folder to use as the root node for the tree.
     */
    public TreeNode(final FolderView folder) {
        this(folder, false);
    }

    /**
     * Constructs a new {@code TreeNode} instance with the specified folder as its root node, and
     * optionally excluding the assets from the cloned node.
     *
     * @param folder       the folder to use as the root node for the tree
     * @param ignoreAssets whether to exclude the assets from the cloned node ({@code true}) or not
     *                     ({@code false})
     */
    public TreeNode(final FolderView folder, final boolean ignoreAssets) {

        this.folder = folder;
        this.children = new ArrayList<>();
        this.assets = new ArrayList<>();

        if (!ignoreAssets) {
            if (folder.assets() != null) {
                this.assets.addAll(folder.assets().versions());
            }
        }
    }

    /**
     * Returns the folder represented by this TreeNode.
     */
    public FolderView folder() {
        return this.folder;
    }

    /**
     * Returns a list of child nodes of this TreeNode.
     */
    public List<TreeNode> children() {
        return this.children;
    }

    /**
     * Returns a list of assets contained within the folder represented by this {@code TreeNode}.
     *
     * @return the list of assets
     */
    public List<AssetView> assets() {
        return this.assets;
    }

    /**
     * Adds a child node to this {@code TreeNode}.
     *
     * @param child the child node to add
     */
    public void addChild(final TreeNode child) {
        this.children.add(child);
    }

    /**
     * Sets the list of assets contained within the folder represented by this {@code TreeNode}.
     *
     * @param assets the list of files to set
     */
    public void assets(final List<AssetView> assets) {
        this.assets = assets;
    }

    /**
     * Clones the current TreeNode and filters its assets based on the provided status and language.
     * It can also optionally filter out empty folders based on the showEmptyFolders parameter.
     *
     * @param live             A boolean indicating whether the assets should be live (true) or
     *                         working (false).
     * @param language         A string representing the language of the assets to be included in
     *                         the cloned node.
     * @param showEmptyFolders A boolean indicating whether to include empty folders in the cloned
     *                         node. If set to true, all folders will be included. If set to false,
     *                         only folders containing assets or having children with assets will be
     *                         included.
     * @param filterForPushChanges A boolean indicating whether the filter should be applied for a push process or
     *                             a regular tree handling.
     * @return A new TreeNode that is a clone of the current node, with its assets filtered based on
     * the provided status and language, and its folders filtered based on the showEmptyFolders
     * parameter.
     */
    public TreeNode cloneAndFilterAssets(final boolean live, final String language,
                                         final boolean showEmptyFolders,
                                         final boolean filterForPushChanges) {

        TreeNode newNode = new TreeNode(this.folder, true);

        // Clone and filter assets based on the status and language
        boolean includeAssets = includeAssets();
        if (includeAssets && this.assets != null) {
            List<AssetView> filteredAssets = this.assets.stream()
                    .filter((asset) -> {

                        if (live) {
                            return asset.live() && asset.lang().equalsIgnoreCase(language);
                        }

                        return asset.working() && asset.lang().equalsIgnoreCase(language);
                    })
                    .collect(Collectors.toList());
            newNode.assets(filteredAssets);
        }

        // Clone children without assets
        for (TreeNode child : this.children) {

            // If we have an explicit rule to exclude this folder, we skip it
            if (child.folder().explicitGlobExclude()) {
                continue;
            }

            TreeNode clonedChild = child.cloneAndFilterAssets(live, language, showEmptyFolders, filterForPushChanges);

            if (filterForPushChanges) {

                if (showEmptyFolders
                        || !clonedChild.assets.isEmpty()
                        || (child.folder().markForPush().isPresent() || child.folder().markForDelete().isPresent())
                        || hasAssetsWithChangesInSubtree(clonedChild)
                        || hasAFolderWithChangesInSubtree(clonedChild)) {

                    if (clonedChild.folder.implicitGlobInclude() || hasIncludeInSubtree(clonedChild)) {
                        newNode.addChild(clonedChild);
                    }
                }
            } else {

                if (showEmptyFolders
                        || !clonedChild.assets.isEmpty()
                        || hasAssetsInSubtree(clonedChild)) {

                    if (clonedChild.folder.implicitGlobInclude() || hasIncludeInSubtree(clonedChild)) {
                        newNode.addChild(clonedChild);
                    }
                }
            }
        }

        return newNode;
    }

    /**
     * Collects unique statuses and languages from the current node and its children.
     *
     * @param showEmptyFolders A boolean indicating whether to include empty folders
     * @return a TreeNodeInfo object containing the collected statuses and languages
     */
    public TreeNodeInfo collectUniqueStatusesAndLanguages(final boolean showEmptyFolders) {

        TreeNodeInfo nodeInfo = new TreeNodeInfo();
        collectUniqueStatusesAndLanguagesHelper(nodeInfo, showEmptyFolders);
        return nodeInfo;
    }

    /**
     * Collects unique statuses and languages from the current tree node and its children.
     *
     * @param collectEmptyFoldersInfo A boolean indicating whether to include empty folders
     * @param nodeInfo                A TreeNodeInfo object containing the collected statuses and languages
     */
    private void collectUniqueStatusesAndLanguagesHelper(TreeNodeInfo nodeInfo, final boolean collectEmptyFoldersInfo) {

        boolean includeAssets = includeAssets();
        if (includeAssets && assets() != null) {
            for (AssetView asset : assets()) {

                if (asset.live()) {
                    nodeInfo.addLiveLanguage(asset.lang());
                    nodeInfo.incrementAssetsCount();
                }
                if (asset.working()) {
                    nodeInfo.addWorkingLanguage(asset.lang());
                    nodeInfo.incrementAssetsCount();
                }

                nodeInfo.addLanguage(asset.lang());
            }
        }

        for (TreeNode child : children()) {

            // If we have an explicit rule to exclude this folder, we skip it
            if (child.folder().explicitGlobExclude()) {
                continue;
            }

            if (collectEmptyFoldersInfo
                    || !child.assets().isEmpty()
                    || hasAssetsInSubtree(child)) {

                if (child.folder().implicitGlobInclude() || hasIncludeInSubtree(child)) {

                    child.collectUniqueStatusesAndLanguagesHelper(nodeInfo, collectEmptyFoldersInfo);

                    nodeInfo.incrementFoldersCount();
                }
            }
        }
    }

    /**
     * Collects push information from the current node and its children.
     *
     * @return A TreeNodePushInfo object containing the collected push information.
     */
    public TreeNodePushInfo collectTreeNodePushInfo() {

        var nodeInfo = new TreeNodePushInfo();
        collectTreeNodePushInfoHelper(nodeInfo);
        return nodeInfo;
    }

    /**
     * Collects push information from the current tree node and its children.
     *
     * @param nodeInfo A TreeNodePushInfo object containing the collected push information.
     */
    private void collectTreeNodePushInfoHelper(TreeNodePushInfo nodeInfo) {

        boolean includeAssets = includeAssets();
        if (includeAssets && assets() != null) {
            for (AssetView asset : assets()) {

                if (asset.markForPush().isPresent()) {
                    if (asset.markForPush().get()) {

                        nodeInfo.incrementAssetsToPushCount();

                        if (asset.pushTypeNew().isPresent()) {
                            if (asset.pushTypeNew().get()) {
                                nodeInfo.incrementAssetsNewCount();
                            }
                        }
                        if (asset.pushTypeModified().isPresent()) {
                            if (asset.pushTypeModified().get()) {
                                nodeInfo.incrementAssetsModifiedCount();
                            }
                        }
                    }
                }

                if (asset.markForDelete().isPresent()) {
                    if (asset.markForDelete().get()) {
                        nodeInfo.incrementAssetsToDeleteCount();
                    }
                }
            }
        }

        for (TreeNode child : children()) {

            // If we have an explicit rule to exclude this folder, we skip it
            if (child.folder().explicitGlobExclude()) {
                continue;
            }

            if (child.folder().implicitGlobInclude() || hasIncludeInSubtree(child)) {

                child.collectTreeNodePushInfoHelper(nodeInfo);

                if (child.folder().markForPush().isPresent()) {
                    if (child.folder().markForPush().get()) {
                        nodeInfo.incrementFoldersToPushCount();
                    }
                }

                if (child.folder().markForDelete().isPresent()) {
                    if (child.folder().markForDelete().get()) {
                        nodeInfo.incrementFoldersToDeleteCount();
                    }
                }
            }
        }
    }

    /**
     * Determines whether the assets should be included for the current TreeNode based on its folder's
     * include and exclude rules.
     *
     * @return {@code true} if the assets should be included, {@code false} otherwise.
     */
    private boolean includeAssets() {

        // Handling a special case for the root folder
        var rootFolder = this.folder().path().equals("/");
        var includeAssets = true;

        if (rootFolder) {
            if (this.folder().explicitGlobExclude()) {
                return false;
            }

            return this.folder().explicitGlobInclude() || this.folder().implicitGlobInclude();
        }

        return includeAssets;
    }

    /**
     * Recursively checks if the given node or any of its children contains assets.
     *
     * @param node The TreeNode to check for assets.
     * @return A boolean value, true if the node or any of its children contains assets, false
     * otherwise.
     */
    private boolean hasAssetsInSubtree(final TreeNode node) {

        if (!node.assets().isEmpty()) {
            return true;
        }

        return node.children().
                stream().
                anyMatch(this::hasAssetsInSubtree);
    }

    /**
     * Recursively checks if the given node or any of its children contains assets with changes, i.e., assets marked
     * for push or delete.
     *
     * @param node The TreeNode to check for assets with changes.
     * @return {@code true} if the node or any of its children contains assets with changes, {@code false} otherwise.
     */
    private boolean hasAssetsWithChangesInSubtree(final TreeNode node) {

        if (!node.assets().isEmpty()) {
            for (var asset : node.assets()) {
                if (asset.markForPush().isPresent() || asset.markForDelete().isPresent()) {
                    return true;
                }
            }
        }

        return node.children().
                stream().
                anyMatch(this::hasAssetsWithChangesInSubtree);
    }

    /**
     * Recursively checks if the given node or any of its children contains a folder marked for push or delete.
     *
     * @param node The TreeNode to check for folders with changes.
     * @return {@code true} if the node or any of its children contains a folder marked for push or
     * delete, {@code false} otherwise.
     */
    private boolean hasAFolderWithChangesInSubtree(final TreeNode node) {

        if (!node.children().isEmpty()) {
            for (var child : node.children()) {
                if (child.folder().markForPush().isPresent() || child.folder().markForDelete().isPresent()) {
                    return true;
                }
            }
        }

        return node.children().
                stream().
                anyMatch(this::hasAFolderWithChangesInSubtree);
    }

    /**
     * Recursively checks if the given node or any of its children contains a folder marked as
     * include.
     *
     * @param node The TreeNode to check for folders.
     * @return A boolean value, true if the node or any of its children have folders marked as
     * include, false otherwise.
     */
    private boolean hasIncludeInSubtree(final TreeNode node) {

        if (node.folder().implicitGlobInclude()) {
            return true;
        }

        return node.children().
                stream().
                anyMatch(this::hasIncludeInSubtree);
    }

}