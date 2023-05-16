package com.dotcms.api.traversal;

import java.util.Set;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
public interface FolderTraversalService {

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents.
     *
     * @param path            The path to the directory to traverse.
     * @param depth           The maximum depth to traverse the directory tree. If null, the
     *                        traversal will go all the way down to the bottom of the tree.
     * @param includePatterns The glob patterns to include in the traversal.
     * @param excludePatterns The glob patterns to exclude from the traversal.
     * @return A TreeNode representing the directory tree rooted at the specified path.
     */
    TreeNode traverse(
            final String path,
            final Integer depth,
            final Set<String> includePatterns,
            final Set<String> excludePatterns);

}
