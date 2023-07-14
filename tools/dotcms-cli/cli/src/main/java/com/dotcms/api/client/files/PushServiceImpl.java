package com.dotcms.api.client.files;

import com.dotcms.api.client.files.traversal.LocalTraversalService;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodePushInfo;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import io.quarkus.arc.DefaultBean;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.dotcms.common.AssetsUtils.ParseLocalPath;

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
     * @param source             the source path to traverse
     * @param removeAssets       true to allow remove assets, false otherwise
     * @param removeFolders      true to allow remove folders, false otherwise
     * @param ignoreEmptyFolders true to ignore empty folders, false otherwise
     * @return a list of pairs, where each pair represents a folder's local path structure and its corresponding tree node
     * @throws IllegalArgumentException if the source path or workspace path does not exist, or if the source path is
     *                                  outside the workspace
     */
    @ActivateRequestContext
    @Override
    public List<Pair<AssetsUtils.LocalPathStructure, TreeNode>> traverseLocalFolders(
            OutputOptionMixin output, final String source, final boolean removeAssets, final boolean removeFolders,
            final boolean ignoreEmptyFolders) {

        // TODO: Remove this hardcoded path
        var workspacePath = "/Users/jonathan/Downloads/CLI";

        var workspaceFile = new File(workspacePath);
        var sourceFile = new File(source);

        // Validating the source is a valid path
        validateSource(workspaceFile, sourceFile);

        var traversalResult = new ArrayList<Pair<AssetsUtils.LocalPathStructure, TreeNode>>();

        // Parsing the source in order to get the root or roots for the traversal
        var roots = AssetsUtils.ParseRootPaths(workspaceFile, sourceFile);
        for (var root : roots) {

            final var localPathStructure = ParseLocalPath(workspaceFile, new File(root));
            var treeNode = localTraversalService.traverseLocalFolder(output, workspacePath, root,
                    removeAssets, removeFolders, ignoreEmptyFolders);

            traversalResult.add(
                    Pair.of(localPathStructure, treeNode)
            );
        }

        return traversalResult;
    }

    /**
     * Processes the tree nodes by pushing their contents to the remote server. It initiates the push operation
     * asynchronously, displays a progress bar, and waits for the completion of the push process.
     *
     * @param output             the output option mixin
     * @param localPathStructure the local path structure of the folder being pushed
     * @param treeNode           the tree node representing the folder and its contents with all the push information
     *                           for each file and folder
     * @param treeNodePushInfo   the push information summary associated with the tree node
     * @throws RuntimeException if an error occurs during the push process
     */
    @ActivateRequestContext
    @Override
    public void processTreeNodes(OutputOptionMixin output, final AssetsUtils.LocalPathStructure localPathStructure,
                                 final TreeNode treeNode, final TreeNodePushInfo treeNodePushInfo) {

        // TODO: Remove this hardcoded path
        var workspacePath = "/Users/jonathan/Downloads/CLI";

        // ConsoleProgressBar instance to handle the push progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);
        // Calculating the total number of steps
        progressBar.setTotalSteps(
                treeNodePushInfo.assetsToPushCount() +
                        treeNodePushInfo.assetsToDeleteCount() +
                        treeNodePushInfo.foldersToPushCount() +
                        treeNodePushInfo.foldersToDeleteCount()
        );

        CompletableFuture<Void> pushTreeFuture = CompletableFuture.supplyAsync(
                () -> {
                    remoteTraversalService.pushTreeNode(
                            workspacePath, localPathStructure, treeNode, progressBar
                    );
                    return null;
                });
        progressBar.setFuture(pushTreeFuture);

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                progressBar
        );

        // Waits for the completion of both the file push tree process and console progress bar animation tasks.
        // This line blocks the current thread until both CompletableFuture instances
        // (pushTreeFuture and animationFuture) have completed.
        CompletableFuture.allOf(pushTreeFuture, animationFuture).join();
        try {
            pushTreeFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format("Error occurred while pushing contents: [%s].", e.getMessage());
            logger.debug(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }

    }

    /**
     * Validates the source path and workspace path.
     *
     * @param workspaceFile the workspace file
     * @param sourceFile    the source file
     * @throws IllegalArgumentException if the source path does not exist, the workspace path does not exist,
     *                                  or the source path is outside the workspace
     */
    private void validateSource(File workspaceFile, File sourceFile) {

        if (!sourceFile.exists()) {
            throw new IllegalArgumentException(String.format("Source path [%s] does not exist", sourceFile.getAbsolutePath()));
        }

        if (!workspaceFile.exists()) {
            throw new IllegalArgumentException(String.format("Workspace path [%s] does not exist", workspaceFile.getAbsolutePath()));
        }

        // Validating the source is within the workspace
        var workspaceFilePath = workspaceFile.toPath();
        var sourceFilePath = sourceFile.toPath();

        var workspaceCount = workspaceFilePath.getNameCount();
        var sourceCount = sourceFilePath.getNameCount();

        if (sourceCount < workspaceCount) {
            throw new IllegalArgumentException("Source path cannot be outside of the workspace");
        }
    }

}
