package com.dotcms.api.traversal;

import com.dotcms.model.asset.FolderView;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

/**
 * Recursive task for traversing a file system directory and building a hierarchical tree
 * representation of its contents. This task is used to split the traversal into smaller sub-tasks
 * that can be executed in parallel, allowing for faster traversal of large directory structures.
 */
public class FolderTraversalTask extends RecursiveTask<TreeNode> {

    private final Executor executor;
    private final String siteName;
    private final FolderView folder;
    private final boolean root;
    private final int depth;

    /**
     * Constructs a new FolderTraversalTask instance with the specified site name, folder, root
     * flag, and depth.
     *
     * @param siteName The name of the site containing the folder to traverse.
     * @param folder   The folder to traverse.
     * @param root     Whether this task is for the root folder.
     * @param depth    The maximum depth to traverse the directory tree.
     */
    FolderTraversalTask(
            Executor executor,
            final String siteName,
            final FolderView folder,
            final Boolean root,
            final int depth) {

        this.executor = executor;
        this.siteName = siteName;
        this.folder = folder;
        this.root = root;
        this.depth = depth;
    }

    /**
     * Executes the folder traversal task and returns a TreeNode representing the directory tree
     * rooted at the folder specified in the constructor.
     *
     * @return A TreeNode representing the directory tree rooted at the folder specified in the
     * constructor.
     */
    @Override
    protected TreeNode compute() {

        TreeNode currentNode = new TreeNode(folder);

        List<FolderTraversalTask> forks = new LinkedList<>();

        // Processing the very first level
        if (root) {

            // Make a REST call to fetch the root folder
            var fetchedFolder = this.restCall(
                    this.siteName,
                    folder.path(),
                    folder.name(),
                    folder.level()
            );

            // Process the fetched sub-folders
            if (fetchedFolder.subFolders() != null) {
                for (FolderView subFolder : fetchedFolder.subFolders()) {
                    if (this.depth == 0) {
                        currentNode.addChild(new TreeNode(subFolder));
                    } else {

                        // Create a new task to traverse the sub-folder and add it to the list of sub-tasks
                        var task = searchForFolder(
                                this.siteName,
                                folder.name(),
                                subFolder.name(),
                                subFolder.level()
                        );
                        forks.add(task);
                        task.fork();
                    }
                }
            }

            // Add the fetched files to the root folder
            if (fetchedFolder.assets() != null) {
                currentNode.assets(fetchedFolder.assets().versions());
            }

        } else {

            // Traverse all sub-folders of the current folder
            if (this.folder.subFolders() != null) {
                for (FolderView subFolder : this.folder.subFolders()) {

                    if (this.depth == -1 || subFolder.level() <= this.depth) {

                        // Create a new task to traverse the sub-folder and add it to the list of sub-tasks
                        var task = searchForFolder(
                                this.siteName,
                                folder.path(),
                                subFolder.name(),
                                subFolder.level()
                        );
                        forks.add(task);
                        task.fork();
                    } else {

                        // Add the sub-folder to the current node if we've reached the maximum traversal depth
                        currentNode.addChild(new TreeNode(subFolder));
                    }
                }
            }
        }

        // Join all sub-tasks and add their results to the current node
        for (FolderTraversalTask task : forks) {
            TreeNode childNode = task.join();
            currentNode.addChild(childNode);
        }

        return currentNode;
    }

    /**
     * Creates a new FolderTraversalTask instance to search for a folder with the specified
     * parameters.
     *
     * @param siteName         The name of the site containing the folder to search for.
     * @param parentFolderName The name of the parent folder of the folder to search for.
     * @param folderName       The name of the folder to search for.
     * @param level            The level of the folder to search for.
     * @return A new FolderTraversalTask instance to search for the specified folder.
     */
    private FolderTraversalTask searchForFolder(final String siteName,
            final String parentFolderName, final String folderName, final int level) {

        final var folder = this.restCall(siteName, parentFolderName, folderName, level);
        return new FolderTraversalTask(
                this.executor,
                siteName,
                folder,
                false,
                this.depth);
    }

    /**
     * Retrieves the contents of a folder
     *
     * @param siteName         the name of the site containing the folder
     * @param parentFolderName the name of the parent folder containing the folder
     * @param folderName       the name of the folder to retrieve metadata for
     * @param level            the hierarchical level of the folder
     * @return an {@code FolderView} object containing the metadata for the requested folder
     */
    private FolderView restCall(final String siteName, final String parentFolderName,
            final String folderName, final int level) {
        return this.executor.restCall(siteName, parentFolderName, folderName, level);
    }

}