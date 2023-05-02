package com.dotcms.api.traversal;

import com.dotcms.model.folder.Folder;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import javax.enterprise.context.ApplicationScoped;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
@ApplicationScoped
public class FolderTraversalService {

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents.
     *
     * @param folderPath The path to the directory to traverse.
     * @param depth      The maximum depth to traverse the directory tree. If null, the traversal
     *                   will go all the way down to the bottom of the tree.
     * @return A TreeNode representing the directory tree rooted at the specified path.
     */
    public TreeNode traverse(final String folderPath, final Integer depth) {

        if (folderPath == null || folderPath.isEmpty()) {
            throw new IllegalArgumentException("folderPath cannot be null or empty");
        }

        Path path;
        try {
            path = Paths.get(folderPath);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid folder path [%s] provided", folderPath), e);
        }

        String siteName = Optional.of(path.getName(0))
                .map(Path::toString)
                .orElseThrow(
                        () -> new IllegalStateException("Unexpected error: No site name found")
                );

        int nameCount = path.getNameCount();

        String parentFolderName = "/";
        String folderName = "/";

        if (nameCount > 2) {
            parentFolderName = path.subpath(1, nameCount - 1).toString();
            folderName = path.subpath(nameCount - 1, nameCount).toString();
        } else if (nameCount == 2) {
            folderName = path.subpath(1, nameCount).toString();
        }

        // Setting the depth to -1 will make the traversal go all the way down
        // to the bottom of the tree
        int depthToUse = depth == null ? -1 : depth;

        var forkJoinPool = new ForkJoinPool();
        var task = new FolderTraversalTask(
                siteName,
                Folder.builder()
                        .parent(parentFolderName)
                        .name(folderName)
                        .level(0)
                        .build(),
                true,
                depthToUse
        );

        return forkJoinPool.invoke(task);
    }

}
