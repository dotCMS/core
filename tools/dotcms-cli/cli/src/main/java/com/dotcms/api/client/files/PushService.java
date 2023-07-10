package com.dotcms.api.client.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface PushService {

    /**
     * Traverses the local folders and retrieves the hierarchical tree representation of their contents with the push
     * related information for each file and folder.
     * Each folder is represented as a pair of its local path structure and the corresponding tree node.
     *
     * @param output the output option mixin
     * @param source the source path to traverse
     * @return a list of pairs, where each pair represents a folder's local path structure and its corresponding tree node
     * @throws IllegalArgumentException if the source path or workspace path does not exist, or if the source path is
     *                                  outside the workspace
     */
    List<Pair<AssetsUtils.LocalPathStructure, TreeNode>> traverseLocalFolders(OutputOptionMixin output, String source);

    void processTreeNodes(OutputOptionMixin output, List<Pair<AssetsUtils.LocalPathStructure, TreeNode>> treeNodes);

}
