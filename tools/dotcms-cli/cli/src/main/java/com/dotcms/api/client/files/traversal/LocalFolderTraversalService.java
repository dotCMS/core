package com.dotcms.api.client.files.traversal;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.OutputOptionMixin;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
public interface LocalFolderTraversalService {

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents. The folders and contents are compared to the remote server in order to determine
     * if there are any differences between the local and remote file system.
     *
     * @param output        the output option mixin
     * @param workspacePath the workspace path
     * @param source        local the source file or directory
     * @return the root node of the hierarchical tree
     */
    TreeNode traverse(OutputOptionMixin output, final String workspacePath, final String source);
}
