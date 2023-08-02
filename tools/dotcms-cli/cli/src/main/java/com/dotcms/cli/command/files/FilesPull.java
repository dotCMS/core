package com.dotcms.cli.command.files;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.client.files.PullService;
import com.dotcms.api.traversal.RemoteFolderTraversalService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.SearchByPathRequest;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

@ActivateRequestContext
@CommandLine.Command(
        name = FilesPull.NAME,
        header = "@|bold,blue dotCMS Files pull|@",
        description = {
                " This command pulls files from the server and saves them to the specified destination.",
                "" // empty string here so we can have a new line
        }
)
public class FilesPull extends AbstractFilesCommand implements Callable<Integer> {

    static final String NAME = "pull";
    
    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "source",
            description = "dotCMS path to the directory or file to pull "
                    + "- Format: //{site}/{folder} or //{site}/{folder}/{file}")
    String source;

    @CommandLine.Parameters(index = "1", arity = "1", paramLabel = "destination", defaultValue = "",
            description = "Local root directory of the CLI project.")
    String destination;

    @CommandLine.Option(names = {"-r", "--recursive"}, defaultValue = "true",
            description = "Pulls directories and their contents recursively.")
    boolean recursive;

    @CommandLine.Option(names = {"-o", "--override"}, defaultValue = "true",
            description = "Overrides the local files with the ones from the server.")
    boolean override;

    @CommandLine.Option(names = {"-ie", "--includeEmptyFolders"}, defaultValue = "false",
            description =
                    "When this option is enabled, the pull process will not create empty folders. "
                            + "By default, this option is disabled, and empty folders will not be created.")
    boolean includeEmptyFolders;

    @CommandLine.Option(names = {"-ef", "--excludeFolder"},
            paramLabel = "patterns",
            description = "Exclude directories matching the given glob patterns. Multiple "
                    + "patterns can be specified, separated by commas.")
    String excludeFolderPatternsOption;

    @CommandLine.Option(names = {"-ea", "--excludeAsset"},
            paramLabel = "patterns",
            description = "Exclude assets matching the given glob patterns. Multiple "
                    + "patterns can be specified, separated by commas.")
    String excludeAssetPatternsOption;

    @CommandLine.Option(names = {"-if", "--includeFolder"},
            paramLabel = "patterns",
            description = "Include directories matching the given glob patterns. Multiple "
                    + "patterns can be specified, separated by commas.")
    String includeFolderPatternsOption;

    @CommandLine.Option(names = {"-ia", "--includeAsset"},
            paramLabel = "patterns",
            description = "Include assets matching the given glob patterns. Multiple "
                    + "patterns can be specified, separated by commas.")
    String includeAssetPatternsOption;

    @Inject
    RemoteFolderTraversalService folderTraversalService;

    @Inject
    PullService pullAssetsService;

    @Override
    public Integer call() throws Exception {

        try {

            if (AssetsUtils.URLIsFolder(source)) {

                var includeFolderPatterns = parsePatternOption(includeFolderPatternsOption);
                var includeAssetPatterns = parsePatternOption(includeAssetPatternsOption);
                var excludeFolderPatterns = parsePatternOption(excludeFolderPatternsOption);
                var excludeAssetPatterns = parsePatternOption(excludeAssetPatternsOption);

                CompletableFuture<TreeNode> folderTraversalFuture = CompletableFuture.supplyAsync(
                        () -> {
                            // Service to handle the traversal of the folder
                            return folderTraversalService.traverse(
                                    source,
                                    recursive ? null : 0,
                                    includeFolderPatterns,
                                    includeAssetPatterns,
                                    excludeFolderPatterns,
                                    excludeAssetPatterns
                            );
                        });

                // ConsoleLoadingAnimation instance to handle the waiting "animation"
                ConsoleLoadingAnimation consoleLoadingAnimation = new ConsoleLoadingAnimation(
                        output,
                        folderTraversalFuture
                );

                CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                        consoleLoadingAnimation
                );

                // Waits for the completion of both the folder traversal and console loading animation tasks.
                // This line blocks the current thread until both CompletableFuture instances
                // (folderTraversalFuture and animationFuture) have completed.
                CompletableFuture.allOf(folderTraversalFuture, animationFuture).join();
                final var result = folderTraversalFuture.get();

                if (result == null) {
                    output.error(String.format("Error occurred while pulling folder info: [%s].", source));
                    return CommandLine.ExitCode.SOFTWARE;
                }

                // ---
                // Now we need to pull the contents based on the tree we found
                pullAssetsService.pullTree(output, result, destination, override, includeEmptyFolders);

            } else {

                CompletableFuture<AssetVersionsView> assetInformationFuture = CompletableFuture.supplyAsync(
                        () -> retrieveAssetInformation(source)
                );

                // ConsoleLoadingAnimation instance to handle the waiting "animation"
                ConsoleLoadingAnimation consoleLoadingAnimation = new ConsoleLoadingAnimation(
                        output,
                        assetInformationFuture
                );

                CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                        consoleLoadingAnimation
                );

                // Waits for the completion of both the asset retrieval and console loading animation tasks.
                // This line blocks the current thread until both CompletableFuture instances
                // (assetInformationFuture and animationFuture) have completed.
                CompletableFuture.allOf(assetInformationFuture, animationFuture).join();
                final var result = assetInformationFuture.get();

                if (result == null) {
                    output.error(String.format("Error occurred while pulling asset info: [%s].", source));
                    return CommandLine.ExitCode.SOFTWARE;
                }

                // Handle the pull of a single file
                pullAssetsService.pullFile(output, result, source, destination, override);
            }

            output.info("\n\nPull process finished successfully.");

        } catch (Exception e) {
            return handleFolderTraversalExceptions(source, e);
        }

        return CommandLine.ExitCode.OK;
    }

    /**
     * Retrieves the asset information
     *
     * @param source The asset path
     * @return The asset information
     */
    AssetVersionsView retrieveAssetInformation(final String source) {

        // Requesting the file info
        final AssetAPI assetAPI = this.clientFactory.getClient(AssetAPI.class);

        // Execute the REST call to retrieve asset information
        var response = assetAPI.assetByPath(SearchByPathRequest.builder().assetPath(source).build());
        return response.entity();
    }

}
