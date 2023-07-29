package com.dotcms.api.client.files.traversal;

import com.dotcms.api.client.files.traversal.data.Pusher;
import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.api.client.files.traversal.task.PushTreeNodeTask;
import com.dotcms.api.client.files.traversal.task.RemoteFolderTraversalTask;
import com.dotcms.api.traversal.Filter;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
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
        var dotCMSPath = AssetsUtils.ParseRemotePath(path);

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
     * Pushes the contents of the specified tree node to the remote server. The push operation is performed
     * asynchronously using a ForkJoinPool, and the progress is tracked and displayed using the provided
     * console progress bar.
     *
     * @param workspace          the local workspace path
     * @param localPathStructure the local path structure of the folder being pushed
     * @param treeNode           the tree node representing the folder and its contents with all the push
     *                           information for each file and folder
     * @param failFast           true to fail fast, false to continue on error
     * @param progressBar        the console progress bar to track and display the push progress
     * @return A list of exceptions encountered during the push process.
     */
    public List<Exception> pushTreeNode(final String workspace, final AssetsUtils.LocalPathStructure localPathStructure,
                                        final TreeNode treeNode, final boolean failFast, ConsoleProgressBar progressBar) {

        // If the language does not exist we need to create it
        if (!localPathStructure.languageExists()) {
            pusher.createLanguage(localPathStructure.language());
        }

        // ---
        var forkJoinPool = ForkJoinPool.commonPool();
        var task = new PushTreeNodeTask(
                workspace,
                localPathStructure,
                treeNode,
                failFast,
                logger,
                pusher,
                progressBar);
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
