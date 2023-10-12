package com.dotcms.api.client.files;

import static java.util.stream.Collectors.groupingBy;

import com.dotcms.api.client.files.traversal.LocalTraversalService;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.files.traversal.task.TraverseParams;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodePushInfo;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import com.dotcms.common.AssetsUtils.LocalPathStructure;
import com.dotcms.model.asset.FolderSyncMeta;
import com.dotcms.model.asset.FolderView;
import io.quarkus.arc.DefaultBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
     * Traverses the local folders and retrieves the hierarchical tree representation of their
     * contents with the push related information for each file and folder. Each folder is
     * represented as a pair of its local path structure and the corresponding tree node.
     *
     * @param output             the output option mixin
     * @param workspace          the project workspace
     * @param source             the source to traverse
     * @param removeAssets       true to allow remove assets, false otherwise
     * @param removeFolders      true to allow remove folders, false otherwise
     * @param ignoreEmptyFolders true to ignore empty folders, false otherwise
     * @param failFast           true to fail fast, false to continue on error
     * @return a list of Triple, where each Triple contains a list of exceptions, the folder's local
     * path structure and its corresponding root node of the hierarchical tree
     * @throws IllegalArgumentException if the source path or workspace path does not exist, or if
     *                                  the source path is outside the workspace
     */
    @ActivateRequestContext
    @Override
    public List<TraverseContext> traverseLocalFolders(
            OutputOptionMixin output, final File workspace, final File source, final boolean removeAssets,
            final boolean removeFolders, final boolean ignoreEmptyFolders, final boolean failFast) {

        var traversalResult = new ArrayList<TraverseContext>();

        // Parsing the source in order to get the root or roots for the traversal
        //By root we mean the folder that holds the asset info starting from the site
        //For example:
        // /home/user/workspace/files/live/es/demo.dotcms.com
        // /home/user/workspace/files/working/es/demo.dotcms.com
        // /home/user/workspace/files/live/en-us/demo.dotcms.com
        // /home/user/workspace/files/working/en-us/demo.dotcms.com

        var roots = AssetsUtils.parseRootPaths(workspace, source);
        for (var root : roots) {

            final TraverseParams params = TraverseParams.builder()
                    .withOutput(output)
                    .withWorkspace(workspace)
                    .withSourcePath(root)
                    .withRemoveAssets(removeAssets)
                    .withRemoveFolders(removeFolders)
                    .withIgnoreEmptyFolders(ignoreEmptyFolders)
                    .withFailFast(failFast)
                    .build();

            // Traversing the local folder
            var result = localTraversalService.traverseLocalFolder(params);

            traversalResult.add(new TraverseContext(result.getLeft(), result.getMiddle(), result.getRight()));
        }

        // Once we have all roots data here we need to eliminate ambiguity
        final Map<String, Map<String, FolderView>> indexedByStatusAndLanguage = indexByStatusAndLanguage(traversalResult);
        indexedByStatusAndLanguage.forEach((lang, folders) -> {
            logger.info("Lang: " + lang);
            folders.forEach((path, folder) -> {
                // here I need to get a hold of the other folders under  different languages and statuses but with the same path
                // and check if they all are marked for delete as well
                // if they all are marked for delete then it is safe to keep it otherwise the delete op isn't valid
                final List<FolderView> folderViews = findAllFoldersWithTheSamePath(indexedByStatusAndLanguage, path);
                final boolean allMarked = isAllMarkedForDelete(folderViews);
                if (!allMarked) {
                    logger.info(String.format("Not all folders with path [%s] are marked for delete. Skipping delete operation", path));
                    folderViews.forEach(folderView -> {
                        final Optional<FolderSyncMeta> syncMeta = folderView.syncMeta();
                        syncMeta.ifPresent(meta -> {
                            //folderView.withSyncMeta(FolderSyncMeta.builder().from(meta).markedForDelete(false).build());
                            //??? how to update the folderView with the new syncMeta
                        });
                    });
                }
            });
        });
        return traversalResult;
    }

    /**
     * Traverses the remote folders and retrieves the hierarchical tree representation of their
     * @param traverseResult the result of the local traversal process
     * @return an indexed representation of the local folders by status and language
     */
    private Map<String,Map<String, FolderView>> indexByStatusAndLanguage(List<TraverseContext> traverseResult) {
        final Map<String, List<TraverseContext>> groupBySite = traverseResult.stream()
                .collect(groupingBy(ctx -> ctx.localPaths.site()));

        Map<String,Map<String, FolderView>> indexedFolders = new HashMap<>();
        groupBySite.forEach((site, list) -> list.forEach(ctx -> {
            final String key = site + ":" + ctx.localPaths.status() + ":" + ctx.localPaths.language();
            ctx.treeNode.flattened().forEach(node -> indexedFolders.computeIfAbsent(key, k -> new HashMap<>()).put(node.folder().path(), node.folder()));
        }));
        return indexedFolders;
    }

    /**
     * Finds all the folders with the same path
     * @param indexByStatusAndLanguage
     * @param path
     * @return
     */
    List<FolderView> findAllFoldersWithTheSamePath(Map<String, Map<String, FolderView>> indexByStatusAndLanguage, String path){
        return indexByStatusAndLanguage.values().stream()
                .filter(map -> map.containsKey(path)).map(map -> map.get(path))
                .collect(Collectors.toList());
    }

    /**
     * Checks if all the folders in the list are marked for delete
     * @param folderViews
     * @return
     */
    boolean isAllMarkedForDelete(List<FolderView> folderViews) {
        return folderViews.stream().allMatch(folderView -> folderView.syncMeta().isPresent() && folderView.syncMeta().get().markedForDelete());
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
     * @param maxRetryAttempts   the maximum number of retry attempts in case of error
     * @throws RuntimeException if an error occurs during the push process
     */
    @ActivateRequestContext
    @Override
    public void processTreeNodes(OutputOptionMixin output, final String workspace,
                                 final LocalPathStructure localPathStructure, final TreeNode treeNode,
                                 final TreeNodePushInfo treeNodePushInfo, final boolean failFast,
                                 final int maxRetryAttempts) {

        var retryAttempts = 0;
        var failed = false;

        do {

            if (retryAttempts > 0) {
                output.info(String.format("%n↺ Retrying push process [%d of %d]...", retryAttempts, maxRetryAttempts));
            }

            // ConsoleProgressBar instance to handle the push progress bar
            ConsoleProgressBar progressBar = new ConsoleProgressBar(output);
            // Calculating the total number of steps
            progressBar.setTotalSteps(
                    treeNodePushInfo.assetsToPushCount() +
                            treeNodePushInfo.assetsToDeleteCount() +
                            treeNodePushInfo.foldersToPushCount() +
                            treeNodePushInfo.foldersToDeleteCount()
            );

            var isRetry = retryAttempts > 0;
            CompletableFuture<List<Exception>> pushTreeFuture = CompletableFuture.supplyAsync(
                    () -> remoteTraversalService.pushTreeNode(
                            workspace, localPathStructure, treeNode, failFast, isRetry, progressBar
                    ));
            progressBar.setFuture(pushTreeFuture);

            CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                    progressBar
            );


            try {

                // Waits for the completion of both the file push tree process and console progress bar animation tasks.
                // This line blocks the current thread until both CompletableFuture instances
                // (pushTreeFuture and animationFuture) have completed.
                CompletableFuture.allOf(pushTreeFuture, animationFuture).join();

                var errors = pushTreeFuture.get();
                if (!errors.isEmpty()) {
                    failed = true;
                    output.info(String.format("%n%nFound [@|bold,red %s|@] errors during the push process:", errors.size()));
                    long count = errors.stream().filter(TraversalTaskException.class::isInstance).count();
                    int c = 0;
                    for (var error : errors) {
                        if (error instanceof TraversalTaskException) {
                            c++;
                            output.handleCommandException(error, String. format("%s %n", error.getMessage()), c == count);

                        } else {
                            output.error(error.getMessage());
                        }
                    }
                }

            } catch (InterruptedException | ExecutionException e) {

                var errorMessage = String.format("Error occurred while pushing contents: [%s].", e.getMessage());
                logger.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            } catch (Exception e) {// Fail fast

                failed = true;
                if (retryAttempts + 1 <= maxRetryAttempts) {
                    output.info("\n\nFound errors during the push process:");
                    output.error(e.getMessage());
                } else {
                    throw e;
                }
            }
        } while (failed && retryAttempts++ < maxRetryAttempts);

    }

    public static class TraverseContext {

        final List<Exception> exceptions;
        final LocalPathStructure localPaths;
        final TreeNode treeNode;

        public TraverseContext(List<Exception> exceptions, LocalPathStructure localPaths,
                TreeNode treeNode) {
            this.exceptions = exceptions;
            this.localPaths = localPaths;
            this.treeNode = treeNode;
        }

        public List<Exception> getExceptions() {
            return exceptions;
        }

        public LocalPathStructure getLocalPaths() {
            return localPaths;
        }

        public TreeNode getTreeNode() {
            return treeNode;
        }

        @Override
        public String toString() {
            return "TraverseContext{" +
                    "exceptions=" + exceptions +
                    ", localPaths=" + localPaths +
                    ", treeNode=" + treeNode +
                    '}';
        }
    }

}
