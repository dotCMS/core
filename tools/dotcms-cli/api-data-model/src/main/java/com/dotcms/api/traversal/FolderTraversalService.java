package com.dotcms.api.traversal;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.model.asset.AssetsFolder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ForkJoinPool;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
@ApplicationScoped
public class FolderTraversalService {

    @Inject
    RestClientFactory clientFactory;

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents.
     *
     * @param path  The path to the directory to traverse.
     * @param depth The maximum depth to traverse the directory tree. If null, the traversal will go
     *              all the way down to the bottom of the tree.
     * @return A TreeNode representing the directory tree rooted at the specified path.
     */
    public TreeNode traverse(final String path, final Integer depth) {

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

        final String folderPath = uri.getPath();
        if (null == folderPath) {
            throw new IllegalArgumentException(
                    String.format("Unable to determine path: [%s].", path));
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

        final AssetAPI assetAPI = clientFactory.getClient(AssetAPI.class);

        var forkJoinPool = new ForkJoinPool();
        var task = new FolderTraversalTask(
                assetAPI,
                site,
                AssetsFolder.builder()
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
