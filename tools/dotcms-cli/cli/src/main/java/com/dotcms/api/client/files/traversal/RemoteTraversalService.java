package com.dotcms.api.client.files.traversal;

import com.dotcms.api.client.files.traversal.task.TraverseTaskResult;
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
    TraverseTaskResult traverseRemoteFolder(
            final String path,
            final Integer depth,
            final boolean failFast,
            final Set<String> includeFolderPatterns,
            final Set<String> includeAssetPatterns,
            final Set<String> excludeFolderPatterns,
            final Set<String> excludeAssetPatterns
    );

    /**
     * Pushes the contents of the specified tree node to the remote server. The push operation is
     * performed asynchronously using a ForkJoinPool, and the progress is tracked and displayed
     * using the provided console progress bar.
     *
     * @param traverseParams All the parameters needed to traverse the tree
     * @return A list of exceptions encountered during the push process.
     */
    List<Exception> pushTreeNode(PushTraverseParams traverseParams);

}
