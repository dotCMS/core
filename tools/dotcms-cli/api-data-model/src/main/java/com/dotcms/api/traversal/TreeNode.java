package com.dotcms.api.traversal;

import com.dotcms.model.asset.Asset;
import com.dotcms.model.asset.AssetsFolder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
@JsonSerialize(using = TreeNodeSerializer.class)
public class TreeNode {

    private final AssetsFolder folder;
    private List<TreeNode> children;
    private List<Asset> assets;

    /**
     * Constructs a new TreeNode instance with the specified folder as its root node.
     *
     * @param folder The folder to use as the root node for the tree.
     */
    public TreeNode(AssetsFolder folder) {
        this.folder = folder;
        this.children = new ArrayList<>();
        this.assets = folder.assets();
    }

    /**
     * Constructs a new {@code TreeNode} instance with the specified folder as its root node, and
     * optionally excluding the assets from the cloned node.
     *
     * @param folder       the folder to use as the root node for the tree
     * @param ignoreAssets whether to exclude the assets from the cloned node ({@code true}) or not
     *                     ({@code false})
     */
    public TreeNode(AssetsFolder folder, Boolean ignoreAssets) {
        this.folder = folder;
        this.children = new ArrayList<>();
        if (!ignoreAssets) {
            this.assets = folder.assets();
        } else {
            this.assets = new ArrayList<>();
        }
    }

    /**
     * Returns the folder represented by this TreeNode.
     */
    public AssetsFolder folder() {
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
    public List<Asset> assets() {
        return this.assets;
    }

    /**
     * Adds a child node to this {@code TreeNode}.
     *
     * @param child the child node to add
     */
    public void addChild(TreeNode child) {
        this.children.add(child);
    }

    /**
     * Sets the list of assets contained within the folder represented by this {@code TreeNode}.
     *
     * @param assets the list of files to set
     */
    public void assets(List<Asset> assets) {
        this.assets = assets;
    }

    /**
     * Clones the current {@code TreeNode}, filtering assets based on the given status and language.
     * The cloned node contains only assets with a status that matches the given status parameter
     * and with a language that matches the given language parameter.
     *
     * @param status   the status to filter assets with
     * @param language the language to filter assets with
     * @return the cloned and filtered {@code TreeNode}
     */
    public TreeNode cloneAndFilterAssets(boolean status, String language) {

        TreeNode newNode = new TreeNode(this.folder, true);

        // Clone and filter assets based on the status and language
        List<Asset> filteredAssets = this.assets.stream()
                .filter(asset -> asset.live() == status && asset.lang().equals(language))
                .collect(Collectors.toList());
        newNode.assets(filteredAssets);

        // Clone children without assets
        for (TreeNode child : this.children) {
            newNode.children.add(child.cloneAndFilterAssets(status, language));
        }

        return newNode;
    }

}
