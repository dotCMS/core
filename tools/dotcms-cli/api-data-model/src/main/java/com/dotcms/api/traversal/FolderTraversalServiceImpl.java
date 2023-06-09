package com.dotcms.api.traversal;

import com.dotcms.model.asset.FolderView;
import io.quarkus.arc.DefaultBean;
import org.jboss.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        InternalFolderPath dotCMSPath = parse(path);

        int nameCount = dotCMSPath.path.getNameCount();

        String parentFolderName = "/";
        String folderName = "/";

        if (nameCount > 1) {
            parentFolderName = dotCMSPath.path.subpath(0, nameCount - 1).toString();
            folderName = dotCMSPath.path.subpath(nameCount - 1, nameCount).toString();
        } else if (nameCount == 1) {
            folderName = dotCMSPath.path.subpath(0, nameCount).toString();
        }

        // Setting the depth to -1 will make the traversal go all the way down
        // to the bottom of the tree
        int depthToUse = depth == null ? -1 : depth;

        // Building the glob filter
        var filterRootPath = dotCMSPath.path.toString();
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
        var filter = filterBuilder.build();

        // ---
        var forkJoinPool = ForkJoinPool.commonPool();

        var task = new FolderTraversalTask(
                retriever,
                filter,
                dotCMSPath.site,
                FolderView.builder()
                        .host(dotCMSPath.site)
                        .path(parentFolderName)
                        .name(folderName)
                        .level(0)
                        .build(),
                true,
                depthToUse
        );

        return forkJoinPool.invoke(task);
    }

    /**
     * Parses the given path and extracts the site and folder path components.
     *
     * @param path the path to parse
     * @return an InternalFolderPath object containing the site and folder path
     */
    private InternalFolderPath parse(String path) {

        if (path == null || path.isEmpty()) {
            var error = "path cannot be null or empty";
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }

        final URI uri;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            logger.debug(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        final String site = uri.getHost();
        if (null == site) {
            var error = String.format(
                    "Unable to determine site from path: [%s]. Site must start with a valid protocol or simply // ",
                    path);
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }

        String folderPath = uri.getPath();
        if (null == folderPath) {
            var error = String.format("Unable to determine path: [%s].", path);
            logger.debug(error);
            throw new IllegalArgumentException(error);
        }
        if (folderPath.isEmpty()) {
            folderPath = "/";
        }

        Path dotCMSPath;
        try {
            dotCMSPath = Paths.get(folderPath);
        } catch (InvalidPathException e) {
            var error = String.format("Invalid folder path [%s] provided", path);
            logger.debug(error, e);
            throw new IllegalArgumentException(error, e);
        }

        // Represents the site and folder path components of the parsed path.
        return new InternalFolderPath(site, dotCMSPath);
    }

    /**
     * Represents the site and folder path components of the parsed path.
     */
    private static class InternalFolderPath {

        /**
         * The site component of the parsed path.
         */
        public final String site;

        /**
         * The folder path component of the parsed path.
         */
        public final Path path;

        /**
         * Constructs an InternalFolderPath object with the given site and folder path.
         *
         * @param site the site component
         * @param path the folder path component
         */
        public InternalFolderPath(String site, Path path) {
            this.site = site;
            this.path = path;
        }
    }

}
