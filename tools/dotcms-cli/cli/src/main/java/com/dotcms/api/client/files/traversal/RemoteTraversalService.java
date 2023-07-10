package com.dotcms.api.client.files.traversal;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.common.AssetsUtils;

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
     * @param includeFolderPatterns The glob patterns for folders to include in the traversal.
     * @param includeAssetPatterns  The glob patterns for assets to include in the traversal.
     * @param excludeFolderPatterns The glob patterns for folders to exclude from the traversal.
     * @param excludeAssetPatterns  The glob patterns for assets to exclude from the traversal.
     * @return A TreeNode representing the directory tree rooted at the specified path.
     */
    TreeNode traverseRemoteFolder(
            final String path,
            final Integer depth,
            final Set<String> includeFolderPatterns,
            final Set<String> includeAssetPatterns,
            final Set<String> excludeFolderPatterns,
            final Set<String> excludeAssetPatterns
    );

    void pushTreeNode(String workspace, AssetsUtils.LocalPathStructure localPathStructure, TreeNode treeNode,
                      ConsoleProgressBar progressBar);

}
