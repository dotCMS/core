package com.dotcms.api.client.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import io.quarkus.arc.DefaultBean;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@DefaultBean
@Dependent
public class PullFolder extends PullBase {

    /**
     * Pulls files from the server and saves them to the specified destination.
     *
     * @param output               the output option mixin for printing progress
     * @param tree                 the tree node representing the file structure
     * @param destination          the destination to save the pulled files
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param failFast             true to fail fast, false to continue on error
     */
    @ActivateRequestContext
    public void pull(OutputOptionMixin output, final TreeNode tree, final File destination,
                     final boolean overwrite, final boolean generateEmptyFolders, final boolean failFast) {

        // Collect important information about the tree
        final var treeNodeInfo = tree.collectUniqueStatusesAndLanguages(generateEmptyFolders);

        output.info(String.format("\rStarting pull process for: " +
                        "@|bold,green [%s]|@ Assets in " +
                        "@|bold,green [%s]|@ Folders and " +
                        "@|bold,green [%s]|@ Languages\n\n",
                treeNodeInfo.assetsCount(),
                treeNodeInfo.foldersCount() == 0 ? 1 : treeNodeInfo.foldersCount(),
                treeNodeInfo.languages().size()));

        // ConsoleProgressBar instance to handle the download progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);

        CompletableFuture<List<Exception>> treeBuilderFuture = CompletableFuture.supplyAsync(
                () -> processTree(
                        tree, treeNodeInfo, destination, overwrite, generateEmptyFolders, failFast, progressBar
                ));

        progressBar.setFuture(treeBuilderFuture);

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                progressBar
        );

        // Waits for the completion of both the file system tree builder and console progress bar animation tasks.
        // This line blocks the current thread until both CompletableFuture instances
        // (treeBuilderFuture and animationFuture) have completed.
        CompletableFuture.allOf(treeBuilderFuture, animationFuture).join();
        try {

            var errors = treeBuilderFuture.get();
            if (!errors.isEmpty()) {
                output.info("Errors found during the pull process:");
                for (var error : errors) {
                    output.error(error.getMessage());
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format("Error occurred while pulling assets: [%s].", e.getMessage());
            logger.debug(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

}
