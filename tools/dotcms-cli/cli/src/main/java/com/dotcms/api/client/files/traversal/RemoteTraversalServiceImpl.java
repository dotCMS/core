package com.dotcms.api.client.files.traversal;

import com.dotcms.api.client.files.traversal.data.Pusher;
import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.files.traversal.task.PushTreeNodeTask;
import com.dotcms.api.client.files.traversal.task.PushTreeNodeTaskParams;
import com.dotcms.api.client.files.traversal.task.RemoteFolderTraversalTask;
import com.dotcms.api.client.files.traversal.task.RemoteFolderTraversalTaskParams;
import com.dotcms.api.client.files.traversal.task.TraverseTaskResult;
import com.dotcms.api.traversal.Filter;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.FolderView;
import io.quarkus.arc.DefaultBean;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * Service for traversing a dotCMS remote location and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
@DefaultBean
@Dependent
public class RemoteTraversalServiceImpl implements RemoteTraversalService {

    @Inject
    Logger logger;

    @Inject
    Retriever retriever;

    @Inject
    Pusher pusher;

    @Inject
    ManagedExecutor executor;

    /**
     * Traverses the dotCMS remote location at the specified remote path and builds a hierarchical tree
     * representation of its contents.
     *
     * @param path                  The remote path to the directory to traverse.
     * @param depth                 The maximum depth to traverse the directory tree. If null, the
     *                              traversal will go all the way down to the bottom of the tree.
     * @param failFast              true to fail fast, false to continue on error
     * @param includeFolderPatterns The glob patterns for folders to include in the traversal.
     * @param includeAssetPatterns  The glob patterns for assets to include in the traversal.
     * @param excludeFolderPatterns The glob patterns for folders to exclude from the traversal.
     * @param excludeAssetPatterns  The glob patterns for assets to exclude from the traversal.
     * @return A Pair object containing a list of exceptions encountered during traversal and the resulting
     * TreeNode representing the directory tree rooted at the specified path.
     */
    @ActivateRequestContext
    @Override
    public TraverseTaskResult traverseRemoteFolder(
            final String path,
            final Integer depth,
            final boolean failFast,
            final Set<String> includeFolderPatterns,
            final Set<String> includeAssetPatterns,
            final Set<String> excludeFolderPatterns,
            final Set<String> excludeAssetPatterns
    ) {

        logger.debug(String.format("Traversing folder: %s - with depth: %d", path, depth));

        // Parsing and validating the given path
        var dotCMSPath = AssetsUtils.parseRemotePath(path);

        // Setting the depth to -1 will make the traversal go all the way down
        // to the bottom of the tree
        int depthToUse = depth == null ? -1 : depth;

        // Building the glob filter
        Filter filter = buildFilter(
                dotCMSPath.folderPath().toString(),
                includeFolderPatterns,
                includeAssetPatterns,
                excludeFolderPatterns,
                excludeAssetPatterns);

        // ---
        var task = new RemoteFolderTraversalTask(
                logger,
                executor,
                retriever
        );

        task.setTaskParams(RemoteFolderTraversalTaskParams.builder()
                .filter(filter)
                .siteName(dotCMSPath.site())
                .folder(FolderView.builder()
                        .host(dotCMSPath.site())
                        .path(dotCMSPath.folderPath().toString())
                        .name(dotCMSPath.folderName())
                        .level(0)
                        .build())
                .isRoot(true)
                .depth(depthToUse)
                .failFast(failFast)
                .build()
        );

        try {
            return task.compute().join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TraversalTaskException) {
                throw (TraversalTaskException) cause;
            } else {
                throw new TraversalTaskException(cause.getMessage(), cause);
            }
        }
    }

    /**
     * Pushes the contents of the specified tree node to the remote server. The push operation is
     * performed asynchronously using a ForkJoinPool, and the progress is tracked and displayed
     * using the provided console progress bar.
     *
     * @param traverseParams@return A list of exceptions encountered during the push process.
     */
    public List<Exception> pushTreeNode(PushTraverseParams traverseParams) {

        // If the language does not exist we need to create it
        if (!traverseParams.localPaths().languageExists()) {
           try {
               pusher.createLanguage(traverseParams.localPaths().language());
           } catch (Exception e) {
               //If we failed to create the language we still continue with the push
               // we simply let the user know and move on
               final String errorMessage = String.format(
                       "Error creating language. Can not process this folder [%s] branch.",
                       traverseParams.localPaths().language());
               logger.error(errorMessage, e);
               return List.of(e);
           }
        }

        // ---
        var task = new PushTreeNodeTask(
                logger,
                executor,
                traverseParams.pushContext(),
                pusher
        );
        
        task.setTaskParams(PushTreeNodeTaskParams.builder()
                .workspacePath(traverseParams.workspacePath())
                .localPaths(traverseParams.localPaths())
                .rootNode(traverseParams.rootNode())
                .failFast(traverseParams.failFast())
                .isRetry(traverseParams.isRetry())
                .maxRetryAttempts(traverseParams.maxRetryAttempts())
                .progressBar(traverseParams.progressBar())
                .build()
        );

        try {
            return task.compute().join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TraversalTaskException) {
                throw (TraversalTaskException) cause;
            } else {
                throw new TraversalTaskException(cause.getMessage(), cause);
            }
        }
    }

    /**
     * Builds a filter object based on the provided glob patterns for including and excluding folders and assets.
     *
     * @param path                  The root path.
     * @param includeFolderPatterns The glob patterns for folders to include in the traversal.
     * @param includeAssetPatterns  The glob patterns for assets to include in the traversal.
     * @param excludeFolderPatterns The glob patterns for folders to exclude from the traversal.
     * @param excludeAssetPatterns  The glob patterns for assets to exclude from the traversal.
     * @return The built filter object that can be used to filter the traversal results.
     */
    private static Filter buildFilter(final String path, Set<String> includeFolderPatterns,
                                      Set<String> includeAssetPatterns, Set<String> excludeFolderPatterns,
                                      Set<String> excludeAssetPatterns) {

        var filterRootPath = path;
        if (!filterRootPath.endsWith("/")) {
            filterRootPath += "/";
        }
        var filterBuilder = Filter.builder().rootPath(filterRootPath);

        Optional.ofNullable(includeFolderPatterns).ifPresent(
                includes -> includes.forEach(filterBuilder::includeFolder)
        );
        Optional.ofNullable(includeAssetPatterns).ifPresent(
                includes -> includes.forEach(filterBuilder::includeAsset)
        );
        Optional.ofNullable(excludeFolderPatterns).ifPresent(
                excludes -> excludes.forEach(filterBuilder::excludeFolder)
        );
        Optional.ofNullable(excludeAssetPatterns).ifPresent(
                excludes -> excludes.forEach(filterBuilder::excludeAsset)
        );

        return filterBuilder.build();
    }

}
