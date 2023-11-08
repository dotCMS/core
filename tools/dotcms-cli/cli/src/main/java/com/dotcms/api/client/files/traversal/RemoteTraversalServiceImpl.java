package com.dotcms.api.client.files.traversal;

import com.dotcms.api.client.files.traversal.data.Pusher;
import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.api.client.files.traversal.task.PushTreeNodeTask;
import com.dotcms.api.client.files.traversal.task.RemoteFolderTraversalTask;
import com.dotcms.api.traversal.Filter;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.FolderView;
import io.quarkus.arc.DefaultBean;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

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
    protected Retriever retriever;

    @Inject
    protected Pusher pusher;

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
    public Pair<List<Exception>, TreeNode> traverseRemoteFolder(
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
        var forkJoinPool = ForkJoinPool.commonPool();

        var task = new RemoteFolderTraversalTask(
                logger,
                retriever,
                filter,
                dotCMSPath.site(),
                FolderView.builder()
                        .host(dotCMSPath.site())
                        .path(dotCMSPath.folderPath().toString())
                        .name(dotCMSPath.folderName())
                        .level(0)
                        .build(),
                true,
                depthToUse,
                failFast
        );

        return forkJoinPool.invoke(task);
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
        var forkJoinPool = ForkJoinPool.commonPool();
        var task = new PushTreeNodeTask(PushTraverseParams.builder()
                .from(traverseParams)
                .pusher(pusher)
                .build());
        return forkJoinPool.invoke(task);
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
