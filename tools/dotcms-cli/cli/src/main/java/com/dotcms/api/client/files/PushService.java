package com.dotcms.api.client.files;

import com.dotcms.api.client.files.traversal.PushTraverseParams;
import com.dotcms.api.client.files.traversal.TraverseResult;
import com.dotcms.api.traversal.TreeNodePushInfo;
import com.dotcms.cli.common.OutputOptionMixin;
import java.io.File;
import java.util.List;

public interface PushService {

    /**
     * Traverses the local folders and retrieves the hierarchical tree representation of their
     * contents with the push related information for each file and folder. Each folder is
     * represented as a TraverseResult with the corresponding local path structure and tree node.
     *
     * @param output             the output option mixin
     * @param source             the source to traverse
     * @param workspace          the project workspace
     * @param removeAssets       true to allow remove assets, false otherwise
     * @param removeFolders      true to allow remove folders, false otherwise
     * @param ignoreEmptyFolders true to ignore empty folders, false otherwise
     * @param failFast           true to fail fast, false to continue on error
     * @return a list of Triple, where each Triple contains a list of exceptions, the folder's local
     * path structure and its corresponding root node of the hierarchical tree
     * @throws IllegalArgumentException if the source path or workspace path does not exist, or if
     *                                  the source path is outside the workspace
     */
    List<TraverseResult> traverseLocalFolders(
            OutputOptionMixin output, File workspace, File source, boolean removeAssets, boolean removeFolders,
            boolean ignoreEmptyFolders, final boolean failFast);

    /**
     * Processes the tree nodes by pushing their contents to the remote server. It initiates the
     * push operation asynchronously, displays a progress bar, and waits for the completion of the
     * push process.
     *
     * @param output           the output option mixin*
     * @param pushInfo  the tree node push info
     * @param traverseParams All the parameters needed to traverse the tree
     * @throws RuntimeException if an error occurs during the push process
     */
    void processTreeNodes(OutputOptionMixin output,
            TreeNodePushInfo pushInfo, PushTraverseParams traverseParams);

}
