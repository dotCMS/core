package com.dotcms.api.client.files;

import com.dotcms.api.client.files.traversal.LocalTraversalService;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodePushInfo;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import io.quarkus.arc.DefaultBean;
import org.apache.commons.lang3.tuple.Triple;
import org.jboss.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@DefaultBean
@Dependent
public class PushServiceImpl implements PushService {

    @Inject
    Logger logger;

    @Inject
    LocalTraversalService localTraversalService;

    @Inject
    RemoteTraversalService remoteTraversalService;

    /**
     * Traverses the local folders and retrieves the hierarchical tree representation of their contents with the push
     * related information for each file and folder.
     * Each folder is represented as a pair of its local path structure and the corresponding tree node.
     *
     * @param output             the output option mixin
     * @param workspace          the project workspace
     * @param source             the source to traverse
     * @param removeAssets       true to allow remove assets, false otherwise
     * @param removeFolders      true to allow remove folders, false otherwise
     * @param ignoreEmptyFolders true to ignore empty folders, false otherwise
     * @param failFast           true to fail fast, false to continue on error
     * @return a list of Triple, where each Triple contains a list of exceptions, the folder's local path structure
     * and its corresponding root node of the hierarchical tree
     * @throws IllegalArgumentException if the source path or workspace path does not exist, or if the source path is
     *                                  outside the workspace
     */
    @ActivateRequestContext
    @Override
    public List<Triple<List<Exception>, AssetsUtils.LocalPathStructure, TreeNode>> traverseLocalFolders(
            OutputOptionMixin output, final File workspace, final File source, final boolean removeAssets,
            final boolean removeFolders, final boolean ignoreEmptyFolders, final boolean failFast) {

        var traversalResult = new ArrayList<Triple<List<Exception>, AssetsUtils.LocalPathStructure, TreeNode>>();

        // Parsing the source in order to get the root or roots for the traversal
        var roots = AssetsUtils.ParseRootPaths(workspace, source);
        for (var root : roots) {

            // Traversing the local folder
            var result = localTraversalService.traverseLocalFolder(output, workspace, root,
                    removeAssets, removeFolders, ignoreEmptyFolders, failFast);

            traversalResult.add(
                    result
            );
        }

        return traversalResult;
    }

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
     * @param failFast           true to fail fast, false to continue on error
     * @throws RuntimeException if an error occurs during the push process
     */
    @ActivateRequestContext
    @Override
    public void processTreeNodes(OutputOptionMixin output, final String workspace,
                                            final AssetsUtils.LocalPathStructure localPathStructure, final TreeNode treeNode,
                                            final TreeNodePushInfo treeNodePushInfo, final boolean failFast) {

        // ConsoleProgressBar instance to handle the push progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);
        // Calculating the total number of steps
        progressBar.setTotalSteps(
                treeNodePushInfo.assetsToPushCount() +
                        treeNodePushInfo.assetsToDeleteCount() +
                        treeNodePushInfo.foldersToPushCount() +
                        treeNodePushInfo.foldersToDeleteCount()
        );

        CompletableFuture<List<Exception>> pushTreeFuture = CompletableFuture.supplyAsync(
                () -> remoteTraversalService.pushTreeNode(
                        workspace, localPathStructure, treeNode, failFast, progressBar
                ));
        progressBar.setFuture(pushTreeFuture);

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                progressBar
        );

        // Waits for the completion of both the file push tree process and console progress bar animation tasks.
        // This line blocks the current thread until both CompletableFuture instances
        // (pushTreeFuture and animationFuture) have completed.
        CompletableFuture.allOf(pushTreeFuture, animationFuture).join();
        try {

            var errors = pushTreeFuture.get();
            if (!errors.isEmpty()) {
                output.info("\n\nErrors found during the push process:");
                for (var error : errors) {
                    output.error(error.getMessage());
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format("Error occurred while pushing contents: [%s].", e.getMessage());
            logger.debug(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }

    }

}
