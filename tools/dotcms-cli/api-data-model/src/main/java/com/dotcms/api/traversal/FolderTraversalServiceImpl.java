package com.dotcms.api.traversal;

import com.dotcms.model.asset.FolderView;
import io.quarkus.arc.DefaultBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
@DefaultBean
@Dependent
public class FolderTraversalServiceImpl implements FolderTraversalService {

    @Inject
    protected Executor executor;

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

        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path cannot be null or empty");
        }

        final URI uri;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        final String site = uri.getHost();
        if (null == site) {
            throw new IllegalArgumentException(String.format(
                    "Unable to determine site from path: [%s]. Site must start with a valid protocol or simply // ",
                    path));
        }

        String folderPath = uri.getPath();
        if (null == folderPath) {
            throw new IllegalArgumentException(
                    String.format("Unable to determine path: [%s].", path));
        }
        if (folderPath.isEmpty()) {
            folderPath = "/";
        }

        Path dotCMSPath;
        try {
            dotCMSPath = Paths.get(folderPath);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid folder path [%s] provided", path), e);
        }

        int nameCount = dotCMSPath.getNameCount();

        String parentFolderName = "/";
        String folderName = "/";

        if (nameCount > 1) {
            parentFolderName = dotCMSPath.subpath(0, nameCount - 1).toString();
            folderName = dotCMSPath.subpath(nameCount - 1, nameCount).toString();
        } else if (nameCount == 1) {
            folderName = dotCMSPath.subpath(0, nameCount).toString();
        }

        // Setting the depth to -1 will make the traversal go all the way down
        // to the bottom of the tree
        int depthToUse = depth == null ? -1 : depth;

        // Building the glob filter
        var filterRootPath = dotCMSPath.toString();
        if (!filterRootPath.endsWith("/")) {
            filterRootPath += "/";
        }
        var filterBuilder = Filter.builder().rootPath(filterRootPath);
        for (var includePattern : includeFolderPatterns) {
            filterBuilder.includeFolder(includePattern);
        }
        for (var includePattern : includeAssetPatterns) {
            filterBuilder.includeAsset(includePattern);
        }
        for (var excludePattern : excludeFolderPatterns) {
            filterBuilder.excludeFolder(excludePattern);
        }
        for (var excludePattern : excludeAssetPatterns) {
            filterBuilder.excludeAsset(excludePattern);
        }
        var filter = filterBuilder.build();

        // ---
        var forkJoinPool = ForkJoinPool.commonPool();

        var task = new FolderTraversalTask(
                executor,
                filter,
                site,
                FolderView.builder()
                        .site(site)
                        .path(parentFolderName)
                        .name(folderName)
                        .level(0)
                        .build(),
                true,
                depthToUse
        );

        return forkJoinPool.invoke(task);
    }

}
