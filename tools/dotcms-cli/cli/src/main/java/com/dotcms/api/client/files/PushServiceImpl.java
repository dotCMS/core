package com.dotcms.api.client.files;

import static java.util.stream.Collectors.groupingBy;

import com.dotcms.api.client.files.traversal.LocalTraversalService;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.client.files.traversal.AbstractTraverseResult;
import com.dotcms.api.client.files.traversal.TraverseResult;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.files.traversal.TraverseParams;
import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodePushInfo;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import com.dotcms.common.AssetsUtils.LocalPathStructure;
import io.quarkus.arc.DefaultBean;
import java.util.HashMap;
import java.util.Map;
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
    public List<TraverseResult> traverseLocalFolders(
            OutputOptionMixin output, final File workspace, final File source, final boolean removeAssets,
            final boolean removeFolders, final boolean ignoreEmptyFolders, final boolean failFast) {

        var traversalResult = new ArrayList<TraverseResult>();

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
                    .output(output)
                    .workspace(workspace)
                    .sourcePath(root)
                    .removeAssets(removeAssets)
                    .removeFolders(removeFolders)
                    .ignoreEmptyFolders(ignoreEmptyFolders)
                    .failFast(failFast)
                    .build();

            // Traversing the local folder
            var result = localTraversalService.traverseLocalFolder(params);
            traversalResult.add(result);
        }

        normalize(traversalResult);
        return traversalResult;
    }

    /**
     * Any ambiguity should be held here
     * @param traversalResult
     */
    private void normalize(ArrayList<TraverseResult> traversalResult) {
        // Once we have all roots data here we need to eliminate ambiguity
        final Map<String, Map<String, TreeNode>> indexedByStatusLangAndSite = indexByStatusLangAndSite(
                traversalResult);
        indexedByStatusLangAndSite.forEach((lang, folders) -> {
            logger.info("Lang: " + lang);
            folders.forEach((path, folder) -> normalizeCandidatesForDelete(indexedByStatusLangAndSite, path));
        });
    }

    /**
     If we have a structure like this, and we plan to remove one folder :

     ├── files
     │       ├── live
     │       │       └── en-us
     │       │           └── site-1697486558495
     │       │               ├── folder1
     │       │               │       ├── subFolder1-1
     │       ├── working
     │       │       └── en-us
     │       │           └── site-1697486558495
     │       │               ├── folder1
     │       │               │       ├── subFolder1-1

     the folder must be gone in both branches
     The Folder must be removed from working branch but aldo from live one, so it becomes clear that we intend to remove the folder
     If we leave folders hanging all around we're not being explicitly clear about our intention to remove the folder
     This method resolves these discrepancies.
     It is only when the folder exists remotely and all occurrences of that folder has been removed locally that we consider the folder properly marked for delete
     * @param indexedByStatusLangAndSite The mapped TreeNodes
     * @param path the folder path
     */
    private void normalizeCandidatesForDelete(Map<String, Map<String, TreeNode>> indexedByStatusLangAndSite,
            String path) {
        // here I need to get a hold of the other folders under different languages and or status but with the same path
        // and check if they're all marked for delete as well
        // if they all are marked for delete then it is safe to keep it marked for delete
        // otherwise the delete op isn't valid
        final List<TreeNode> nodes = findAllNodesWithTheSamePath(indexedByStatusLangAndSite, path);
        if (!isAllFoldersMarkedForDelete(nodes)) {
            nodes.forEach(node -> node.markForDelete(false));
        }
    }

    /**
     * Build a map first separated by site then by status and language
     * @param traverseResult the result of the local traversal process
     * @return an indexed representation of the local folders  first indexed by  status language and site
     * The most outer map is organized by composite key like status:lang:site the inner map is the folder path
     */
    private Map<String,Map<String, TreeNode>> indexByStatusLangAndSite(List<TraverseResult> traverseResult) {
        final Map<String, List<TraverseResult>> groupBySite = traverseResult.stream()
                .collect(groupingBy(ctx -> ctx.localPaths().site()));

        Map<String,Map<String, TreeNode>> indexedFolders = new HashMap<>();
        groupBySite.forEach((site, list) -> list.forEach(ctx -> {
            final String key = ctx.localPaths().status() + ":" + ctx.localPaths().language() + ":" + site;
            ctx.treeNode().flattened().forEach(node -> indexedFolders.computeIfAbsent(key, k -> new HashMap<>()).put(node.folder().path(), node));
        }));
        return indexedFolders;
    }

    /**
     * Finds all the folders with the same path
     * @param indexByStatusAndLanguage indexed Nodes by path and grouped by site
     * @param path the path I am looking for
     * @return All nodes matching the path
     */
    List<TreeNode> findAllNodesWithTheSamePath(Map<String, Map<String, TreeNode>> indexByStatusAndLanguage, String path){
        return indexByStatusAndLanguage.values().stream()
                .filter(map -> map.containsKey(path)).map(map -> map.get(path))
                .collect(Collectors.toList());
    }

    /**
     * Checks if all the folders in the list are marked for delete
     * @param nodes
     * @return
     */
    boolean isAllFoldersMarkedForDelete(List<TreeNode> nodes) {
        return nodes.stream().map(TreeNode::folder).allMatch(folderView -> folderView.markForDelete().isPresent() && folderView.markForDelete().get());
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
                Thread.currentThread().interrupt();
                throw new PushException(errorMessage, e);
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

}
