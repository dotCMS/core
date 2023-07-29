package com.dotcms.api.client.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.FolderView;
import io.quarkus.arc.DefaultBean;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@DefaultBean
@Dependent
public class PullFile extends PullBase {

    /**
     * Pulls files from the server and saves them to the specified destination.
     *
     * @param output      the output option mixin for printing progress
     * @param assetInfo   the remote asset information
     * @param source      the remote source path for the file to pull
     * @param destination the destination to save the pulled files
     * @param overwrite   true to overwrite existing files, false otherwise
     * @param failFast    true to fail fast, false to continue on error
     */
    @ActivateRequestContext
    public void pull(OutputOptionMixin output, final AssetVersionsView assetInfo,
                     final String source, final File destination, final boolean overwrite, final boolean failFast) {

        // Parsing and validating the given path
        var dotCMSPath = AssetsUtils.ParseRemotePath(source);

        // Create a simple tree node for the asset to handle
        var folder = FolderView.builder()
                .host(dotCMSPath.site())
                .path(dotCMSPath.folderPath().toString())
                .name(dotCMSPath.folderName())
                .assets(assetInfo)
                .build();
        TreeNode tree = new TreeNode(folder);

        // Collect important information about the tree
        final var treeNodeInfo = tree.collectUniqueStatusesAndLanguages(false);

        output.info(String.format("\rStarting pull process for: " +
                        "@|bold,green [%s]|@ Assets in " +
                        "@|bold,green [%s]|@ Languages\n\n",
                1, treeNodeInfo.languages().size()));

        // ConsoleProgressBar instance to handle the download progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);

        CompletableFuture<List<Exception>> processFileFuture = CompletableFuture.supplyAsync(
                () -> processTree(
                        tree, treeNodeInfo, destination, overwrite, false, failFast, progressBar
                ));

        progressBar.setFuture(processFileFuture);

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                progressBar
        );

        // Waits for the completion of both the file process and console progress bar animation tasks.
        // This line blocks the current thread until both CompletableFuture instances
        // (processFileFuture and animationFuture) have completed.
        CompletableFuture.allOf(processFileFuture, animationFuture).join();
        try {

            var errors = processFileFuture.get();
            if (!errors.isEmpty()) {
                output.info("Errors found during the pull process:");
                for (var error : errors) {
                    output.error(error.getMessage());
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format("Error occurred while pulling asset: [%s].", e.getMessage());
            logger.debug(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

}
