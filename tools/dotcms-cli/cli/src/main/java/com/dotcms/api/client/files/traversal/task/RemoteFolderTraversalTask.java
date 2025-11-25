package com.dotcms.api.client.files.traversal.task;

import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.task.TaskProcessor;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.model.asset.FolderView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import jakarta.enterprise.context.Dependent;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * Recursive task for traversing a dotCMS remote location and building a hierarchical tree
 * representation of its contents. This task is used to split the traversal into smaller sub-tasks
 * that can be executed in parallel, allowing for faster traversal of large directory structures.
 */
@Dependent
public class RemoteFolderTraversalTask extends
        TaskProcessor<RemoteFolderTraversalTaskParams, CompletableFuture<TraverseTaskResult>> {

    private final ManagedExecutor executor;
    private final Logger logger;
    private final Retriever retriever;

    private RemoteFolderTraversalTaskParams traversalTaskParams;

    /**
     * Constructs a new RemoteFolderTraversalTask instance.
     *
     * @param executor  the executor for parallel execution of traversal tasks
     * @param logger    the logger for logging debug information
     * @param retriever The retriever used for REST calls and other operations.
     */
    public RemoteFolderTraversalTask(
            final Logger logger,
            final ManagedExecutor executor,
            final Retriever retriever) {

        this.executor = executor;
        this.logger = logger;
        this.retriever = retriever;
    }

    /**
     * Sets the traversal parameters for the RemoteFolderTraversalTask. This method provides a way
     * to inject necessary configuration after the instance of RemoteFolderTraversalTask has been
     * created by the container, which is a common pattern when working with frameworks like Quarkus
     * that manage object creation and dependency injection in a specific manner.
     * <p>
     * This method is used as an alternative to constructor injection, which is not feasible due to
     * the limitations or constraints of the framework's dependency injection mechanism. It allows
     * for the explicit setting of traversal parameters after the object's instantiation, ensuring
     * that the executor is properly configured before use.
     *
     * @param params The traversal parameters
     */
    @Override
    public void setTaskParams(final RemoteFolderTraversalTaskParams params) {
        this.traversalTaskParams = params;
    }

    /**
     * Executes the folder traversal task and returns a TreeNode representing the directory tree
     * rooted at the folder specified in the constructor.
     *
     * @return A Pair object containing a list of exceptions encountered during traversal and the
     * resulting TreeNode representing the directory tree at the specified folder.
     */
    @Override
    public CompletableFuture<TraverseTaskResult> compute() {

        var errors = new ArrayList<Exception>();
        TreeNode currentNode = null;
        FolderView currentFolder = null;

        try {

            // Processing the current folder
            var processResult = processCurrentFolder();

            currentNode = processResult.getLeft();
            currentFolder = processResult.getRight();

        } catch (Exception e) {
            if (traversalTaskParams.failFast()) {
                return CompletableFuture.failedFuture(e);
            } else {
                errors.add(e);
            }
        }

        // And now its sub-folders
        return processSubFolders(
                currentNode,
                currentFolder,
                errors
        );
    }

    /**
     * Processes the current folder and returns a TreeNode representing it.
     *
     * @return A Pair object containing the TreeNode representing the processed folder and the
     * FolderView object representing the current folder.
     */
    private Pair<TreeNode, FolderView> processCurrentFolder() {

        var currentNode = new TreeNode(traversalTaskParams.folder());
        var currentFolder = traversalTaskParams.folder();

        // Processing the very first level
        if (traversalTaskParams.isRoot()) {

            try {
                // Make a REST call to fetch the root folder
                currentFolder = this.retrieveFolderInformation(
                        traversalTaskParams.siteName(),
                        traversalTaskParams.folder().path(),
                        traversalTaskParams.folder().level(),
                        traversalTaskParams.folder().implicitGlobInclude(),
                        traversalTaskParams.folder().explicitGlobInclude(),
                        traversalTaskParams.folder().explicitGlobExclude()
                );

                // Using the values set by the filter in the root folder
                var detailedFolder = traversalTaskParams.folder().withImplicitGlobInclude(
                        currentFolder.implicitGlobInclude());
                detailedFolder = detailedFolder.withExplicitGlobInclude(
                        currentFolder.explicitGlobInclude());
                detailedFolder = detailedFolder.withExplicitGlobExclude(
                        currentFolder.explicitGlobExclude());
                currentNode = new TreeNode(detailedFolder);

                // Add the fetched files to the root folder
                if (currentFolder.assets() != null) {
                    currentNode.assets(currentFolder.assets().versions());
                }
            } catch (TraversalTaskException e) {
                throw e;
            } catch (Exception e) {
                throw new TraversalTaskException(e.getMessage(), e);
            }
        }

        return Pair.of(currentNode, currentFolder);
    }

    /**
     * Processes the sub-folders of the current folder and returns a CompletableFuture containing a
     * pair of a list of exceptions encountered during the traversal and a TreeNode representing the
     * current folder.
     *
     * @param currentNode   The current TreeNode representing the current folder.
     * @param currentFolder The FolderView object representing the current folder to process.
     * @param errors        The list of exceptions to which any error should be added.
     * @return a CompletableFuture containing a list of exceptions encountered during the traversal
     * and a TreeNode representing the processed folder.
     */
    private CompletableFuture<TraverseTaskResult> processSubFolders(
            TreeNode currentNode, FolderView currentFolder, List<Exception> errors) {

        // Traverse all sub-folders of the current folder
        if (currentFolder != null && currentFolder.subFolders() != null) {

            List<CompletableFuture<TraverseTaskResult>> futures = new ArrayList<>();

            for (FolderView subFolder : currentFolder.subFolders()) {

                if (traversalTaskParams.depth() == -1
                        || subFolder.level() <= traversalTaskParams.depth()) {

                    try {
                        // Create a new task to traverse the sub-folder and add it to the list of sub-tasks
                        var task = searchForFolder(
                                traversalTaskParams.siteName(),
                                subFolder.path(),
                                subFolder.level(),
                                subFolder.implicitGlobInclude(),
                                subFolder.explicitGlobInclude(),
                                subFolder.explicitGlobExclude()
                        );

                        CompletableFuture<TraverseTaskResult> future =
                                CompletableFuture.supplyAsync(
                                        task::compute, executor
                                ).thenCompose(Function.identity());
                        futures.add(future);
                    } catch (Exception e) {
                        handleException(errors, e);
                    }
                } else {

                    // Add the sub-folder to the current node if we've reached the maximum traversal depth
                    currentNode.addChild(new TreeNode(subFolder));
                }
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> {
                        for (CompletableFuture<TraverseTaskResult> future : futures) {
                            var taskResult = future.join();
                            errors.addAll(taskResult.exceptions());
                            taskResult.treeNode().ifPresent(currentNode::addChild);
                        }
                        return TraverseTaskResult.builder()
                                .exceptions(errors)
                                .treeNode(Optional.ofNullable(currentNode))
                                .build();
                    });
        }

        return CompletableFuture.completedFuture(TraverseTaskResult.builder()
                .exceptions(errors)
                .treeNode(Optional.ofNullable(currentNode))
                .build());
    }

    /**
     * Creates a new FolderTraversalTask instance to search for a folder with the specified
     * parameters.
     *
     * @param siteName            The name of the site containing the folder to search for.
     * @param folderPath          The path of the folder to search for.
     * @param level               The level of the folder to search for.
     * @param implicitGlobInclude This property represents whether a folder should be implicitly
     *                            included based on the absence of any include patterns. When
     *                            implicitGlobInclude is set to true, it means that there are no
     *                            include patterns specified, so all folders should be included by
     *                            default. In other words, if there are no specific include patterns
     *                            defined, the filter assumes that all folders should be included
     *                            unless explicitly excluded.
     * @param explicitGlobInclude This property represents whether a folder should be explicitly
     *                            included based on the configured includes patterns for folders.
     *                            When explicitGlobInclude is set to true, it means that the folder
     *                            has matched at least one of the include patterns and should be
     *                            included in the filtered result. The explicit inclusion takes
     *                            precedence over other rules. If a folder is explicitly included,
     *                            it will be included regardless of any other rules or patterns.
     * @param explicitGlobExclude This property represents whether a folder should be explicitly
     *                            excluded based on the configured excludes patterns for folders.
     *                            When explicitGlobExclude is set to true, it means that the folder
     *                            has matched at least one of the exclude patterns and should be
     *                            excluded from the filtered result. The explicit exclusion takes
     *                            precedence over other rules. If a folder is explicitly excluded,
     *                            it will be excluded regardless of any other rules or patterns.
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

        var task = new RemoteFolderTraversalTask(
                this.logger,
                this.executor,
                this.retriever
        );

        task.setTaskParams(RemoteFolderTraversalTaskParams.builder()
                .filter(traversalTaskParams.filter())
                .siteName(siteName)
                .folder(folder)
                .isRoot(false)
                .depth(traversalTaskParams.depth())
                .failFast(traversalTaskParams.failFast())
                .build()
        );

        return task;
    }

    /**
     * Retrieves the contents of a folder
     *
     * @param siteName            The name of the site containing the folder
     * @param folderPath          The path of the folder to search for.
     * @param level               The hierarchical level of the folder
     * @param implicitGlobInclude This property represents whether a folder should be implicitly
     *                            included based on the absence of any include patterns. When
     *                            implicitGlobInclude is set to true, it means that there are no
     *                            include patterns specified, so all folders should be included by
     *                            default. In other words, if there are no specific include patterns
     *                            defined, the filter assumes that all folders should be included
     *                            unless explicitly excluded.
     * @param explicitGlobInclude This property represents whether a folder should be explicitly
     *                            included based on the configured includes patterns for folders.
     *                            When explicitGlobInclude is set to true, it means that the folder
     *                            has matched at least one of the include patterns and should be
     *                            included in the filtered result. The explicit inclusion takes
     *                            precedence over other rules. If a folder is explicitly included,
     *                            it will be included regardless of any other rules or patterns.
     * @param explicitGlobExclude This property represents whether a folder should be explicitly
     *                            excluded based on the configured excludes patterns for folders.
     *                            When explicitGlobExclude is set to true, it means that the folder
     *                            has matched at least one of the exclude patterns and should be
     *                            excluded from the filtered result. The explicit exclusion takes
     *                            precedence over other rules. If a folder is explicitly excluded,
     *                            it will be excluded regardless of any other rules or patterns.
     * @return an {@code FolderView} object containing the metadata for the requested folder
     */
    private FolderView retrieveFolderInformation(final String siteName, final String folderPath,
            final int level, final boolean implicitGlobInclude, final Boolean explicitGlobInclude,
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

            return traversalTaskParams.filter().apply(foundFolder);
        } catch (Exception e) {
            var message = String.format("Error retrieving folder information [%s]", folderPath);
            logger.error(message, e);
            throw new TraversalTaskException(message, e);
        }
    }

    /**
     * Handles an exception that occurred during the execution of a traversal task.
     *
     * @param errors The list of exceptions to which the error should be added.
     * @param e      The exception that occurred.
     */
    private void handleException(List<Exception> errors, Exception e) {

        if (traversalTaskParams.failFast()) {

            if (e instanceof TraversalTaskException) {
                throw (TraversalTaskException) e;
            }

            throw new TraversalTaskException(e.getMessage(), e);
        } else {
            errors.add(e);
        }
    }

}
