package com.dotcms.api.client.files.traversal;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
public interface LocalTraversalService {

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents. The folders and contents are compared to the remote server in order to determine
     * if there are any differences between the local and remote file system.
     *
     * @param output             the output option mixin
     * @param workspacePath      the workspace path
     * @param source             local the source file or directory
     * @param removeAssets       true to allow remove assets, false otherwise
     * @param removeFolders      true to allow remove folders, false otherwise
     * @param ignoreEmptyFolders true to ignore empty folders, false otherwise
     * @return a pair representing a folder's local path structure and its corresponding root node of the hierarchical tree
     */
    Pair<AssetsUtils.LocalPathStructure, TreeNode> traverseLocalFolder(
            OutputOptionMixin output, final String workspacePath, final String source,
            boolean removeAssets, boolean removeFolders, boolean ignoreEmptyFolders);

    /**
     * Builds the file system tree from the specified root node. The tree is built using a ForkJoinPool, which allows
     * for parallel execution of the traversal tasks.
     *
     * @param rootNode             the root node of the file tree
     * @param destination          the destination path to save the pulled files
     * @param isLive               true if processing live tree, false for working tree
     * @param language             the language to process
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param progressBar          the progress bar for tracking the pull progress
     */
    void buildFileSystemTree(TreeNode rootNode, String destination, boolean isLive, String language, boolean overwrite,
                             boolean generateEmptyFolders, ConsoleProgressBar progressBar);

}
