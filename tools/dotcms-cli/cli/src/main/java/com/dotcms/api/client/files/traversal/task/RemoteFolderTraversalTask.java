package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.api.traversal.Filter;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.model.asset.FolderView;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

/**
 * Recursive task for traversing a dotCMS remote location and building a hierarchical tree
 * representation of its contents. This task is used to split the traversal into smaller sub-tasks
 * that can be executed in parallel, allowing for faster traversal of large directory structures.
 */
public class RemoteFolderTraversalTask extends RecursiveTask<Pair<List<Exception>, TreeNode>> {

    private final Logger logger;
    private final Retriever retriever;
    private final Filter filter;
    private final String siteName;
    private final FolderView folder;
    private final boolean root;
    private final int depth;
    private final boolean failFast;

    /**
     * Constructs a new RemoteFolderTraversalTask instance with the specified site name, folder, root
     * flag, and depth.
     *
     * @param logger    the logger for logging debug information
     * @param retriever The retriever used for REST calls and other operations.
     * @param filter    The filter used to include or exclude folders and assets.
     * @param siteName  The name of the site containing the folder to traverse.
     * @param folder    The folder to traverse.
     * @param root      Whether this task is for the root folder.
     * @param depth     The maximum depth to traverse the directory tree.
     * @param failFast  true to fail fast, false to continue on error
     */
    public RemoteFolderTraversalTask(
            final Logger logger,
            Retriever retriever,
            Filter filter,
            final String siteName,
            final FolderView folder,
            final Boolean root,
            final int depth,
            final boolean failFast) {

        this.logger = logger;
        this.retriever = retriever;
        this.filter = filter;
        this.siteName = siteName;
        this.folder = folder;
        this.root = root;
        this.depth = depth;
        this.failFast = failFast;
    }

    /**
     * Executes the folder traversal task and returns a TreeNode representing the directory tree
     * rooted at the folder specified in the constructor.
     *
     * @return A TreeNode representing the directory tree rooted at the folder specified in the
     * constructor.
     */
    @Override
    protected Pair<List<Exception>, TreeNode> compute() {

        var errors = new ArrayList<Exception>();

        var currentNode = new TreeNode(folder);
        var currentFolder = folder;

        List<RemoteFolderTraversalTask> forks = new LinkedList<>();

        // Processing the very first level
        if (root) {

            try {
                // Make a REST call to fetch the root folder
                currentFolder = this.retrieveFolderInformation(
                        this.siteName,
                        folder.path(),
                        folder.level(),
                        folder.implicitGlobInclude(),
                        folder.explicitGlobInclude(),
                        folder.explicitGlobExclude()
                );

                // Using the values set by the filter in the root folder
                var detailedFolder = folder.withImplicitGlobInclude(currentFolder.implicitGlobInclude());
                detailedFolder = detailedFolder.withExplicitGlobInclude(currentFolder.explicitGlobInclude());
                detailedFolder = detailedFolder.withExplicitGlobExclude(currentFolder.explicitGlobExclude());
                currentNode = new TreeNode(detailedFolder);

                // Add the fetched files to the root folder
                if (currentFolder.assets() != null) {
                    currentNode.assets(currentFolder.assets().versions());
                }
            } catch (Exception e) {
                if (failFast) {
                    throw e;
                } else {
                    errors.add(e);
                }
            }
        }

        // Traverse all sub-folders of the current folder
        if (currentFolder.subFolders() != null) {
            for (FolderView subFolder : currentFolder.subFolders()) {

                if (this.depth == -1 || subFolder.level() <= this.depth) {

                    try {
                        // Create a new task to traverse the sub-folder and add it to the list of sub-tasks
                        var task = searchForFolder(
                                this.siteName,
                                subFolder.path(),
                                subFolder.level(),
                                subFolder.implicitGlobInclude(),
                                subFolder.explicitGlobInclude(),
                                subFolder.explicitGlobExclude()
                        );
                        forks.add(task);
                        task.fork();
                    } catch (Exception e) {
                        if (failFast) {
                            throw e;
                        } else {
                            errors.add(e);
                        }
                    }
                } else {

                    // Add the sub-folder to the current node if we've reached the maximum traversal depth
                    currentNode.addChild(new TreeNode(subFolder));
                }
            }
        }

        // Join all sub-tasks and add their results to the current node
        for (RemoteFolderTraversalTask task : forks) {
            var taskResult = task.join();
            errors.addAll(taskResult.getLeft());
            currentNode.addChild(taskResult.getRight());
        }

        return Pair.of(errors, currentNode);
    }

    /**
     * Creates a new FolderTraversalTask instance to search for a folder with the specified
     * parameters.
     *
     * @param siteName            The name of the site containing the folder to search for.
     * @param folderPath          The path of the folder to search for.
     * @param level               The level of the folder to search for.
     * @param implicitGlobInclude This property represents whether a folder should be implicitly included based on the
     *                            absence of any include patterns. When implicitGlobInclude is set to true, it means
     *                            that there are no include patterns specified, so all folders should be included by
     *                            default. In other words, if there are no specific include patterns defined, the
     *                            filter assumes that all folders should be included unless explicitly excluded.
     * @param explicitGlobInclude This property represents whether a folder should be explicitly included based on the
     *                            configured includes patterns for folders. When explicitGlobInclude is set to true,
     *                            it means that the folder has matched at least one of the include patterns and should
     *                            be included in the filtered result. The explicit inclusion takes precedence over other
     *                            rules. If a folder is explicitly included, it will be included regardless of any other
     *                            rules or patterns.
     * @param explicitGlobExclude This property represents whether a folder should be explicitly excluded based on the
     *                            configured excludes patterns for folders. When explicitGlobExclude is set to true, it
     *                            means that the folder has matched at least one of the exclude patterns and should be
     *                            excluded from the filtered result. The explicit exclusion takes precedence over other
     *                            rules. If a folder is explicitly excluded, it will be excluded regardless of any other
     *                            rules or patterns.
     * @return A new FolderTraversalTask instance to search for the specified folder.
     */
    private RemoteFolderTraversalTask searchForFolder(
            final String siteName,
            final String folderPath,
            final int level,
            final boolean implicitGlobInclude,
            final Boolean explicitGlobInclude,
            final Boolean explicitGlobExclude
    ) {

        final var folder = this.retrieveFolderInformation(siteName, folderPath, level,
                implicitGlobInclude, explicitGlobInclude, explicitGlobExclude);

        return new RemoteFolderTraversalTask(
                this.logger,
                this.retriever,
                this.filter,
                siteName,
                folder,
                false,
                this.depth,
                this.failFast);
    }

    /**
     * Retrieves the contents of a folder
     *
     * @param siteName            The name of the site containing the folder
     * @param folderPath          The path of the folder to search for.
     * @param level               The hierarchical level of the folder
     * @param implicitGlobInclude This property represents whether a folder should be implicitly included based on the
     *                            absence of any include patterns. When implicitGlobInclude is set to true, it means
     *                            that there are no include patterns specified, so all folders should be included by
     *                            default. In other words, if there are no specific include patterns defined, the
     *                            filter assumes that all folders should be included unless explicitly excluded.
     * @param explicitGlobInclude This property represents whether a folder should be explicitly included based on the
     *                            configured includes patterns for folders. When explicitGlobInclude is set to true,
     *                            it means that the folder has matched at least one of the include patterns and should
     *                            be included in the filtered result. The explicit inclusion takes precedence over other
     *                            rules. If a folder is explicitly included, it will be included regardless of any other
     *                            rules or patterns.
     * @param explicitGlobExclude This property represents whether a folder should be explicitly excluded based on the
     *                            configured excludes patterns for folders. When explicitGlobExclude is set to true, it
     *                            means that the folder has matched at least one of the exclude patterns and should be
     *                            excluded from the filtered result. The explicit exclusion takes precedence over other
     *                            rules. If a folder is explicitly excluded, it will be excluded regardless of any other
     *                            rules or patterns.
     * @return an {@code FolderView} object containing the metadata for the requested folder
     */
    private FolderView retrieveFolderInformation(final String siteName, final String folderPath, final int level,
                                final boolean implicitGlobInclude,
                                final Boolean explicitGlobInclude,
                                final Boolean explicitGlobExclude) {

        try {
            var foundFolder = this.retriever.retrieveFolderInformation(
                    siteName,
                    folderPath,
                    level,
                    implicitGlobInclude,
                    explicitGlobInclude,
                    explicitGlobExclude
            );

            return this.filter.apply(foundFolder);
        } catch (Exception e) {
            var message = String.format("Error retrieving folder information [%s]", folderPath);
            logger.debug(message, e);
            throw new RuntimeException(message, e);
        }
    }

}