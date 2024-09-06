package com.dotcms.api.client.files.traversal;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import java.util.List;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
public interface LocalTraversalService {

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents. The folders and contents are compared to the remote server in
     * order to determine if there are any differences between the local and remote file system.
     *
     * @param params traversal params
     * @return a TraverseResult containing a list of exceptions, the folder's local path structure
     * and its corresponding root node of the hierarchical tree
     */
    TraverseResult traverseLocalFolder(LocalTraverseParams params);

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
     * @param failFast             true to fail fast, false to continue on error
     * @param progressBar          the progress bar for tracking the pull progress
     * @return a list of exceptions that occurred during the pull
     */
    List<Exception> pullTreeNode(TreeNode rootNode, String destination, boolean isLive, String language,
                                        boolean overwrite, boolean generateEmptyFolders, final boolean failFast,
                                        ConsoleProgressBar progressBar);

}
