package com.dotcms.api.client.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodePushInfo;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.List;

public interface PushService {

    /**
     * Traverses the local folders and retrieves the hierarchical tree representation of their contents with the push
     * related information for each file and folder.
     * Each folder is represented as a pair of its local path structure and the corresponding tree node.
     *
     * @param output             the output option mixin
     * @param source             the source to traverse
     * @param workspace          the project workspace
     * @param removeAssets       true to allow remove assets, false otherwise
     * @param removeFolders      true to allow remove folders, false otherwise
     * @param ignoreEmptyFolders true to ignore empty folders, false otherwise
     * @return a list of pairs, where each pair represents a folder's local path structure and its corresponding tree node
     * @throws IllegalArgumentException if the source path or workspace path does not exist, or if the source path is
     *                                  outside the workspace
     */
    List<Pair<AssetsUtils.LocalPathStructure, TreeNode>> traverseLocalFolders(
            OutputOptionMixin output, File workspace, File source, boolean removeAssets, boolean removeFolders,
            boolean ignoreEmptyFolders);

    /**
     * Processes the tree nodes by pushing their contents to the remote server. It initiates the push operation
     * asynchronously, displays a progress bar, and waits for the completion of the push process.
     *
     * @param output             the output option mixin
     * @param workspace          the workspace path
     * @param localPathStructure the local path structure of the folder being pushed
     * @param treeNode           the tree node representing the folder and its contents with all the push information
     *                           for each file and folder
     * @param treeNodePushInfo   the push information summary associated with the tree node
     * @throws RuntimeException if an error occurs during the push process
     */
    void processTreeNodes(OutputOptionMixin output, String workspace, AssetsUtils.LocalPathStructure localPathStructure,
                          TreeNode treeNode, TreeNodePushInfo treeNodePushInfo);

}
