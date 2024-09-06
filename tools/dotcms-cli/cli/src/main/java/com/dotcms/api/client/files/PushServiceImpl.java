package com.dotcms.api.client.files;

import static java.util.stream.Collectors.groupingBy;

import com.dotcms.api.client.files.traversal.LocalTraversalService;
import com.dotcms.api.client.files.traversal.LocalTraverseParams;
import com.dotcms.api.client.files.traversal.PushTraverseParams;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.client.files.traversal.TraverseResult;
import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.api.client.util.ErrorHandlingUtil;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodePushInfo;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.exception.ForceSilentExitException;
import com.dotcms.common.AssetsUtils;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;
import picocli.CommandLine.ExitCode;

@DefaultBean
@Dependent
public class PushServiceImpl implements PushService {

    @Inject
    Logger logger;

    @Inject
    LocalTraversalService localTraversalService;

    @Inject
    RemoteTraversalService remoteTraversalService;

    @Inject
    ErrorHandlingUtil errorHandlerUtil;

    @Inject
    ManagedExecutor executor;

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

            final LocalTraverseParams params = LocalTraverseParams.builder()
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

        if(removeFolders) {
           // If we are removing folders we need to conciliate the paths to ensure we're not removing folders that are not meant to be removed
           //That is we only remove folders that are marked for delete in all branches and languages and status
           conciliatePathsForDelete(traversalResult);
        }
        sort(traversalResult);

        return traversalResult;
    }

    /**
     * Sorts the given list of TraverseResult objects based on site and status.
     *
     * @param traversalResult the list of TraverseResult objects to be sorted
     */
    private void sort(ArrayList<TraverseResult> traversalResult) {

        // Apply some sorting to ensure a proper processing order

        // Sorting by site name
        traversalResult.sort((o1, o2) -> {
            var left = o1.localPaths();
            var right = o2.localPaths();
            return left.site().compareTo(right.site());
        });
        // Sorting by default language: true comes first
        traversalResult.sort((o1, o2) -> {
            var left = o1.localPaths();
            var right = o2.localPaths();
            return Boolean.compare(right.isDefaultLanguage(), left.isDefaultLanguage());
        });
        // Sorting by status: "live" comes first
        traversalResult.sort((o1, o2) -> {
            var left = o1.localPaths();
            var right = o2.localPaths();
            return left.status().compareTo(right.status());
        });
    }

    /**
     * Any ambiguity should be held here
     * @param traversalResult the result of the local traversal process
     */
    private void conciliatePathsForDelete(List<TraverseResult> traversalResult) {
        // Once we have all roots data here we need to eliminate ambiguity
        final Map<String, Map<String, TreeNode>> indexedByStatusLangAndSite =
                indexByStatusLangAndSite(traversalResult);
        indexedByStatusLangAndSite.forEach((lang, folders) -> folders.forEach(
                (path, folder) -> conciliateCandidatesForDelete(indexedByStatusLangAndSite, path))
        );
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
     The Folder must be removed from working branch but also from live one, so it becomes clear that we intend to remove the folder
     If we leave folders hanging all around we're not being explicitly clear about our intention to remove the folder
     This method resolves these discrepancies.
     It is only when the folder exists remotely and all occurrences of that folder has been removed locally that we consider the folder properly marked for delete
     * @param indexedByStatusLangAndSite The mapped TreeNodes
     * @param path the folder path
     */
    private void conciliateCandidatesForDelete(Map<String, Map<String, TreeNode>> indexedByStatusLangAndSite, String path) {
        // here I need to get a hold of the other folders under different languages and or status but with the same path
        // and check if they're all marked for delete as well
        // if they all are marked for delete then it is safe to keep it marked for delete
        // otherwise the delete op isn't valid
        final List<TreeNode> nodes = findAllNodesWithTheSamePath(indexedByStatusLangAndSite, path);
        if (!isAllFoldersMarkedForDelete(nodes)) {
            if(nodes.stream().anyMatch(isMarkedForDelete)) {
               logger.info("The folder " + path + " appears to be marked for delete but NOT in all branches. Therefore it won't be removed. ");
            }
            nodes.forEach(node -> node.markForDelete(false));
        }
    }

    final Predicate<TreeNode> isMarkedForDelete = node -> node.folder().sync().isPresent() && node.folder().sync().get().markedForDelete();

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
            final Optional<TreeNode> optional = ctx.treeNode();
            if(optional.isPresent()) {
                final TreeNode treeNode = optional.get();
                final String key = ctx.localPaths().status() + ":" + ctx.localPaths().language() + ":" + site;
                treeNode.flattened().forEach(
                        node -> indexedFolders.computeIfAbsent(key, k -> new HashMap<>())
                                .put(node.folder().path(), node));
            }
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
     * @param nodes the list of nodes to check
     * @return true if all folders are marked for delete, false otherwise
     */
    boolean isAllFoldersMarkedForDelete(List<TreeNode> nodes) {
        return nodes.stream().map(TreeNode::folder).allMatch(folderView -> folderView.sync().isPresent() && folderView.sync().get().markedForDelete());
    }

    /**
     * Processes the tree nodes by pushing their contents to the remote server. It initiates the
     * push operation asynchronously, displays a progress bar, and waits for the completion of the
     * push process.
     *
     * @param output   the output option mixin
     * @param pushInfo the push info
     * @param traverseParams the push traverse parameters
     * @throws RuntimeException if an error occurs during the push process
     */
    @ActivateRequestContext
    public void processTreeNodes(OutputOptionMixin output,
            TreeNodePushInfo pushInfo, PushTraverseParams traverseParams) {

        var maxRetryAttempts = traverseParams.maxRetryAttempts();
        var failed = false;
        var retryAttempts = 0;
        var errorCode = ExitCode.OK;

        do {

            if (retryAttempts > 0) {
                //In order to retry we need to clear the context
                traverseParams.pushContext().clear();
                output.info(
                        String.format(
                                "%n↺ Retrying push process [%d of %d]...",
                                retryAttempts,
                                traverseParams.maxRetryAttempts()
                        )
                );
            }

            var e = processTreeNodesAttempt(
                    output,
                    pushInfo,
                    traverseParams,
                    retryAttempts
            );
            errorCode = Math.max(errorCode, e);
            if (errorCode > ExitCode.OK) {
                failed = true;
            }

        } while (failed && retryAttempts++ < maxRetryAttempts);
        if (errorCode > ExitCode.OK) {
            //All exceptions are already handled and logged, so we can just throw a generic exception to force exit
            throw new ForceSilentExitException(errorCode);
        }
    }

    /**
     * Process tree nodes by pushing their contents to the remote server. It initiates the push
     * operation asynchronously, displays a progress bar, and waits for the completion of the push
     * process.
     *
     * @param output         the output option mixin
     * @param pushInfo       the push info
     * @param traverseParams the push traverse parameters
     * @param retryAttempts  the number of retry attempts
     * @return the exit code representing the result of the push process
     * @throws PushException if an error occurs during the push process
     */
    private int processTreeNodesAttempt(OutputOptionMixin output,
            TreeNodePushInfo pushInfo, PushTraverseParams traverseParams,
            int retryAttempts) {

        // ConsoleProgressBar instance to handle the push progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);
        // Calculating the total number of steps
        progressBar.setTotalSteps(
                pushInfo.assetsToPushCount() +
                        pushInfo.assetsToDeleteCount() +
                        pushInfo.foldersToPushCount() +
                        pushInfo.foldersToDeleteCount()
        );

        var isRetry = retryAttempts > 0;
        CompletableFuture<List<Exception>> pushTreeFuture = executor.supplyAsync(
                () -> remoteTraversalService.pushTreeNode(
                        PushTraverseParams.builder().from(traverseParams)
                                .progressBar(progressBar)
                                .isRetry(isRetry).build()
                )
        );
        progressBar.setFuture(pushTreeFuture);

        CompletableFuture<Void> animationFuture = executor.runAsync(
                progressBar
        );

        try {

            // Waits for the completion of both the file push tree process and console progress bar animation tasks.
            // This line blocks the current thread until both CompletableFuture instances
            // (pushTreeFuture and animationFuture) have completed.
            CompletableFuture.allOf(pushTreeFuture, animationFuture).join();

            var errors = pushTreeFuture.get();
            return errorHandlerUtil.handlePushExceptions(errors, output);

        } catch (InterruptedException e) {

            var errorMessage = String.format(
                    "Error occurred while pushing contents: [%s].", e.getMessage()
            );
            logger.error(errorMessage, e);
            Thread.currentThread().interrupt();
            throw new PushException(errorMessage, e);
        } catch (ExecutionException | CompletionException e) {// Fail fast

            var cause = e.getCause();
            var toThrow = errorHandlerUtil.handlePushFailFastException(
                    retryAttempts, traverseParams.maxRetryAttempts(), output, cause
            );
            if (toThrow.isPresent()) {
                throw toThrow.get();
            }

            return ExitCode.SOFTWARE;
        }
    }

}
