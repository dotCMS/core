package com.dotcms.api.traversal;

import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.FolderView;
import io.quarkus.arc.DefaultBean;
import org.jboss.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
@DefaultBean
@Dependent
public class FolderTraversalServiceImpl implements FolderTraversalService {

    @Inject
    Logger logger;

    @Inject
    protected Retriever retriever;

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents.
     *
     * @param path                  The path to the directory to traverse.
     * @param depth                 The maximum depth to traverse the directory tree. If null, the
     *                              traversal will go all the way down to the bottom of the tree.
     * @param includeFolderPatterns The glob patterns for folders to include in the traversal.
     * @param includeAssetPatterns  The glob patterns for assets to include in the traversal.
     * @param excludeFolderPatterns The glob patterns for folders to exclude from the traversal.
     * @param excludeAssetPatterns  The glob patterns for assets to exclude from the traversal.
     * @return A TreeNode representing the directory tree rooted at the specified path.
     */
    @ActivateRequestContext
    @Override
    public TreeNode traverse(
            final String path,
            final Integer depth,
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

        var task = new FolderTraversalTask(
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
                depthToUse
        );

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
