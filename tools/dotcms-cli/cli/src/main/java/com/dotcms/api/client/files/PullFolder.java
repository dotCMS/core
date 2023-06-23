package com.dotcms.api.client.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import io.quarkus.arc.DefaultBean;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
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
     * @param destination          the destination path to save the pulled files
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     */
    @ActivateRequestContext
    public void pull(OutputOptionMixin output, final TreeNode tree, final String destination,
                     final boolean overwrite, final boolean generateEmptyFolders) {

        // Collect important information about the tree
        final var treeNodeInfo = tree.collectUniqueStatusesAndLanguages(generateEmptyFolders);

        output.info(String.format("\rStarting pull process for: " +
                        "@|bold,green [%s]|@ Assets in " +
                        "@|bold,green [%s]|@ Folders and " +
                        "@|bold,green [%s]|@ Languages\n\n",
                treeNodeInfo.assetsCount(), treeNodeInfo.foldersCount(), treeNodeInfo.languages().size()));

        // ConsoleProgressBar instance to handle the download progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);

        CompletableFuture<Void> treeBuilderFuture = CompletableFuture.supplyAsync(
                () -> {
                    processTree(tree, treeNodeInfo, destination, overwrite, generateEmptyFolders, progressBar);
                    return null;
                });

        progressBar.setFuture(treeBuilderFuture);

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                progressBar
        );

        // Waits for the completion of both the file system tree builder and console progress bar animation tasks.
        // This line blocks the current thread until both CompletableFuture instances
        // (treeBuilderFuture and animationFuture) have completed.
        CompletableFuture.allOf(treeBuilderFuture, animationFuture).join();
        try {
            treeBuilderFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format("Error occurred while pulling assets: [%s].", e.getMessage());
            logger.debug(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

}
