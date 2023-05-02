package com.dotcms.api.traversal;

import com.dotcms.model.file.File;
import com.dotcms.model.folder.Folder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.List;

/**
 * A node in a hierarchical tree representation of a file system directory. Each node represents a
 * folder and contains references to its child folders and files.
 * <p>
 * The class is annotated with {@code @JsonSerialize(using = TreeNodeSerializer.class)} to specify a
 * custom serializer for JSON serialization of TreeNode instances.
 */
@JsonSerialize(using = TreeNodeSerializer.class)
public class TreeNode {

    private final Folder folder;
    private List<TreeNode> children;
    private List<File> files;

    /**
     * Constructs a new TreeNode instance with the specified folder as its root node.
     *
     * @param folder The folder to use as the root node for the tree.
     */
    public TreeNode(Folder folder) {
        this.folder = folder;
        this.children = new ArrayList<>();
        this.files = folder.files();
    }

    /**
     * Returns the folder represented by this TreeNode.
     */
    public Folder folder() {
        return this.folder;
    }

    /**
     * Returns a list of child nodes of this TreeNode.
     */
    public List<TreeNode> children() {
        return this.children;
    }

    /**
     * Returns a list of files contained within the folder represented by this TreeNode.
     */
    public List<File> files() {
        return this.files;
    }

    /**
     * Adds a child node to this TreeNode.
     *
     * @param child The child node to add to this TreeNode.
     */
    public void addChild(TreeNode child) {
        this.children.add(child);
    }

    /**
     * Sets the list of files contained within the folder represented by this TreeNode.
     *
     * @param files The list of files to set.
     */
    public void files(List<File> files) {
        this.files = files;
    }

}
