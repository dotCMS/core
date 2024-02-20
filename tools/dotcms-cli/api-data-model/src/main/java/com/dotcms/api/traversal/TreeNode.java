package com.dotcms.api.traversal;

import static com.dotcms.common.AssetsUtils.isMarkedForDelete;
import static com.dotcms.common.AssetsUtils.isMarkedForPush;

import com.dotcms.model.asset.AbstractAssetSync.PushType;
import com.dotcms.model.asset.AssetSync;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderSync;
import com.dotcms.model.asset.FolderView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A node in a hierarchical tree representation of a file system directory. Each node represents a
 * folder and contains references to its child folders and files.
 * <p>
 * The class is annotated with {@code @JsonSerialize(using = TreeNodeSerializer.class)} to specify a
 * custom serializer for JSON serialization of TreeNode instances.
 */
public class TreeNode {

    private FolderView folder;
    private final List<TreeNode> children;
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
        if (!ignoreAssets && null != folder.assets()) {
            this.assets.addAll(folder.assets().versions());
        }
    }

    /**
     * Mutators are evil, but we really need to update the status of the folder
     * @param mark the delete mark
     */
    public void markForDelete(boolean mark) {
        final Optional<FolderSync> syncData = this.folder.sync();
        final FolderSync sync = syncData.map(
                   folderSync -> FolderSync.builder().from(folderSync)
                        .markedForDelete(mark).build()
                ).orElseGet(
                   () -> FolderSync.builder().markedForDelete(mark).build()
                );
        this.folder = FolderView.builder().from(this.folder)
                .sync(sync)
                .build();
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
     * Sorts the children of the TreeNode based on their folder names in ascending order. If the
     * TreeNode has no children or is null, this method does nothing.
     */
    public void sortChildren() {
        if (this.children != null && !this.children.isEmpty()) {
            this.children.sort(Comparator.comparing(a -> a.folder().name()));
        }
    }

    /**
     * Returns a list of child nodes of this TreeNode
     * Given that this is a recursive structure, this method returns a flattened list of all the
     * @return the list of child nodes
     */
    public Stream<TreeNode> flattened() {
        return Stream.concat(
                Stream.of(this),
                children.stream().flatMap(TreeNode::flattened));
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
     * Sorts the assets within the current TreeNode based on their names. The sorting is done in
     * ascending order. If the TreeNode does not contain any assets or is null, this method does
     * nothing.
     */
    public void sortAssets() {
        if (this.assets != null && !this.assets.isEmpty()) {
            this.assets.sort(Comparator.comparing(AssetView::name));
        }
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

        final TreeNode newNode = new TreeNode(this.folder, true);
        boolean includeAssets = includeAssets();

        // Clone and filter assets based on the status and language
        if (includeAssets && this.assets != null) {
            List<AssetView> filteredAssets = this.assets.stream()
                    .filter(filterAssetsPredicate(live, language))
                    .collect(Collectors.toList());
            newNode.assets(filteredAssets);
        }

        // Clone children without assets and apply filtering conditions
        for (final TreeNode child : this.children) {
            final TreeNode clonedChild = child.cloneAndFilterAssets(
                    live,
                    language,
                    showEmptyFolders,
                    filterForPushChanges
            );
            if (childShouldBeIncluded(clonedChild, showEmptyFolders, filterForPushChanges)) {
                newNode.addChild(clonedChild);
            }
        }

        return newNode;
    }

    /**
     * Clones the current TreeNode and filters its assets based on the provided status and language.
     * @param child
     * @param showEmptyFolders
     * @param filterForPushChanges
     * @return
     */
    private boolean childShouldBeIncluded(TreeNode child, boolean showEmptyFolders, boolean filterForPushChanges) {
        if (child.folder().explicitGlobExclude()) {
            return false;
        }
        if (filterForPushChanges) {
            return (showEmptyFolders
                    || !child.assets.isEmpty()
                    || (isMarkedForPush(child.folder())
                    || isMarkedForDelete(child.folder()))
                    || hasAssetsWithChangesInSubtree(child)
                    || hasFolderWithChangesInSubtree(child))
                    && (child.folder.implicitGlobInclude() || hasIncludeInSubtree(child));
        } else {
            return (showEmptyFolders
                    || !child.assets.isEmpty()
                    || hasAssetsInSubtree(child))
                    && (child.folder.implicitGlobInclude() || hasIncludeInSubtree(child));
        }
    }

    /**
     * Status and language filter predicate.
     * @param live
     * @param language
     * @return
     */
    private static Predicate<AssetView> filterAssetsPredicate(boolean live, String language) {
        return asset -> {
            if (live) {
                return asset.live() && asset.lang().equalsIgnoreCase(language);
            }
            return asset.working() && asset.lang().equalsIgnoreCase(language);
        };
    }

    /**
     * Collects unique statuses and languages from the current node and its children.
     *
     * @param showEmptyFolders A boolean indicating whether to include empty folders
     * @return a TreeNodeInfo object containing the collected statuses and languages
     */
    public TreeNodeInfo collectUniqueStatusAndLanguage(final boolean showEmptyFolders) {

        TreeNodeInfo nodeInfo = new TreeNodeInfo(this.folder().host());
        internalCollectUniqueStatusAndLanguage(nodeInfo, showEmptyFolders);
        return nodeInfo;
    }

    /**
     * Collects unique statuses and languages from the current tree node and its children.
     *
     * @param collectEmptyFoldersInfo A boolean indicating whether to include empty folders
     * @param nodeInfo                A TreeNodeInfo object containing the collected statuses and languages
     */
    private void internalCollectUniqueStatusAndLanguage(TreeNodeInfo nodeInfo, final boolean collectEmptyFoldersInfo) {
        boolean includeAssets = includeAssets();
        if (includeAssets && assets() != null) {
            assetsLangAndStatusInfo(nodeInfo, assets());
        }
        if(null != children()) {
            childrenLangAndStatusInfo(nodeInfo, children(), collectEmptyFoldersInfo);
        }
    }

    /**
     * Collects unique statuses and languages from the current node's assets.
     * @param nodeInfo
     * @param children
     * @param collectEmptyFoldersInfo
     */
    private void childrenLangAndStatusInfo(final TreeNodeInfo nodeInfo, final List<TreeNode> children, final boolean collectEmptyFoldersInfo) {
        for (TreeNode child : children) {
            if (shouldIncludeChild(child, collectEmptyFoldersInfo)) {
                child.internalCollectUniqueStatusAndLanguage(nodeInfo, collectEmptyFoldersInfo);
                nodeInfo.incrementFoldersCount();
            }
        }
    }

    // Helper method to determine if a child should be included
    private boolean shouldIncludeChild(TreeNode child, boolean collectEmptyFoldersInfo) {
        if (child.folder().explicitGlobExclude()) {
            return false;
        }

        if (collectEmptyFoldersInfo || !child.assets().isEmpty() || hasAssetsInSubtree(child)) {
            return child.folder().implicitGlobInclude() || hasIncludeInSubtree(child);
        }

        return false;
    }

    private void assetsLangAndStatusInfo(final TreeNodeInfo nodeInfo, final List<AssetView> assets) {
        for (AssetView asset : assets) {
            if (asset.live()) {
                nodeInfo.addLiveLanguage(asset.lang());
                nodeInfo.incrementAssetsCount();
                nodeInfo.incrementLiveAssetsCountByLanguage(asset.lang());
            }
            if (asset.working()) {
                nodeInfo.addWorkingLanguage(asset.lang());
                nodeInfo.incrementAssetsCount();
                nodeInfo.incrementWorkingAssetsCountByLanguage(asset.lang());
            }
            nodeInfo.addLanguage(asset.lang());
        }
    }

    /**
     * Collects push information from the current node and its children.
     *
     * @return A TreeNodePushInfo object containing the collected push information.
     */
    public TreeNodePushInfo collectPushInfo() {

        var nodeInfo = new TreeNodePushInfo();
        internalCollectPushInfo(nodeInfo);
        return nodeInfo;
    }

    /**
     * Collects push information from the current tree node and its children.
     *
     * @param nodeInfo A TreeNodePushInfo object containing the collected push information.
     */
    private void internalCollectPushInfo(TreeNodePushInfo nodeInfo) {

        if (includeAssets() && assets() != null) {
            assetsPushInfo(nodeInfo, assets());
        }
        if(null != children()){
            childrenPushInfo(nodeInfo, children());
        }
    }

    private void childrenPushInfo(TreeNodePushInfo nodeInfo, List<TreeNode> children) {
        for (TreeNode child : children) {
            // If we have an explicit rule to exclude this folder, we skip it
            if (child.folder().explicitGlobExclude()) {
                continue;
            }
            if (child.folder().implicitGlobInclude() || hasIncludeInSubtree(child)) {
                child.internalCollectPushInfo(nodeInfo);
                if(isMarkedForPush(child.folder())){
                    nodeInfo.incrementFoldersToPushCount();
                } else if(isMarkedForDelete(child.folder())){
                    nodeInfo.incrementFoldersToDeleteCount();
                }
            }
        }
    }

    private void assetsPushInfo(final TreeNodePushInfo nodeInfo, List<AssetView> assets) {
        for (AssetView asset : assets) {
            final Optional<AssetSync> optional = asset.sync();
            if(optional.isEmpty()) {
                continue;
            }
            final AssetSync meta = optional.get();
            if(meta.markedForPush()){
                nodeInfo.incrementAssetsToPushCount();
                final PushType pushType = meta.pushType();
                if(pushType == PushType.NEW) {
                    nodeInfo.incrementAssetsNewCount();
                }
                if(pushType == PushType.MODIFIED) {
                    nodeInfo.incrementAssetsModifiedCount();
                }
            } else if(meta.markedForDelete()){
                nodeInfo.incrementAssetsToDeleteCount();
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

        for (var asset : node.assets()) {
            if (isMarkedForPush(asset) || isMarkedForDelete(asset)) {
                return true;
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
    private boolean hasFolderWithChangesInSubtree(final TreeNode node) {

        if (!node.children().isEmpty()) {
            for (var child : node.children()) {
                if (isMarkedForPush(child.folder()) || isMarkedForDelete(child.folder())) {
                    return true;
                }
            }
        }

        return node.children().
                stream().
                anyMatch(this::hasFolderWithChangesInSubtree);
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