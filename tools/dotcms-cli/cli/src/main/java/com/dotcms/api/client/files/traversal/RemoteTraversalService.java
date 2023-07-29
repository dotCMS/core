package com.dotcms.api.client.files.traversal;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.common.AssetsUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;

/**
 * Service for traversing a dotCMS remote location and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
public interface RemoteTraversalService {

    /**
     * Traverses the dotCMS remote location at the specified remote path and builds a hierarchical tree
     * representation of its contents.
     *
     * @param path                  The remote path to the directory to traverse.
     * @param depth                 The maximum depth to traverse the directory tree. If null, the
     *                              traversal will go all the way down to the bottom of the tree.
     * @param failFast              true to fail fast, false to continue on error
     * @param includeFolderPatterns The glob patterns for folders to include in the traversal.
     * @param includeAssetPatterns  The glob patterns for assets to include in the traversal.
     * @param excludeFolderPatterns The glob patterns for folders to exclude from the traversal.
     * @param excludeAssetPatterns  The glob patterns for assets to exclude from the traversal.
     * @return A Pair object containing a list of exceptions encountered during traversal and the resulting
     * TreeNode representing the directory tree rooted at the specified path.
     */
    Pair<List<Exception>, TreeNode> traverseRemoteFolder(
            final String path,
            final Integer depth,
            final boolean failFast,
            final Set<String> includeFolderPatterns,
            final Set<String> includeAssetPatterns,
            final Set<String> excludeFolderPatterns,
            final Set<String> excludeAssetPatterns
    );

    /**
     * Pushes the contents of the specified tree node to the remote server. The push operation is performed
     * asynchronously using a ForkJoinPool, and the progress is tracked and displayed using the provided
     * console progress bar.
     *
     * @param workspace          the local workspace path
     * @param localPathStructure the local path structure of the folder being pushed
     * @param treeNode           the tree node representing the folder and its contents with all the push
     *                           information for each file and folder
     * @param failFast           true to fail fast, false to continue on error
     * @param progressBar        the console progress bar to track and display the push progress
     * @return A list of exceptions encountered during the push process.
     */
    List<Exception> pushTreeNode(String workspace, AssetsUtils.LocalPathStructure localPathStructure, TreeNode treeNode,
                                 final boolean failFast, ConsoleProgressBar progressBar);

}
