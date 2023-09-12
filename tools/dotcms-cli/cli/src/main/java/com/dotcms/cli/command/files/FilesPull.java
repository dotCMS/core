package com.dotcms.cli.command.files;

import static com.dotcms.common.LocationUtils.encodePath;

import com.dotcms.api.AssetAPI;
import com.dotcms.api.client.files.PullService;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.WorkspaceMixin;
import com.dotcms.common.LocationUtils;
import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.ByPathRequest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = FilesPull.NAME,
        header = "@|bold,blue dotCMS Files pull|@",
        description = {
                " This command pulls files from the server and saves them to the specified destination.",
                "" // empty string here so we can have a new line
        }
)
public class FilesPull extends AbstractFilesCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "pull";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "path",
            description = "dotCMS path to the directory or file to pull "
                    + "- Format: //{site}/{folder} or //{site}/{folder}/{file}")
    String path;

    @CommandLine.Mixin(name = "workspace")
    WorkspaceMixin workspaceMixin;

    @CommandLine.Option(names = {"-nr", "--non-recursive"}, defaultValue = "false",
            description = "Pulls only the specified directory and the contents under it.")
    boolean nonRecursive;

    @CommandLine.Option(names = {"-p", "--preserve"}, defaultValue = "false",
            description = "Preserves existing files and directories, avoiding overwriting if they already exist.")
    boolean preserve;

    @CommandLine.Option(names = {"-ie", "--includeEmptyFolders"}, defaultValue = "false",
            description =
                    "When this option is enabled, the pull process will not create empty folders. "
                            + "By default, this option is disabled, and empty folders will not be created.")
    boolean includeEmptyFolders;

    @CommandLine.Option(names = {"-ff", "--fail-fast"}, defaultValue = "false",
            description =
                    "Stop at first failure and exit the command. By default, this option is disabled, "
                            + "and the command will continue on error.")
    boolean failFast;

    @CommandLine.Option(names = {"--retry-attempts"}, defaultValue = "0",
            description =
                    "Number of retry attempts on errors. By default, this option is disabled, "
                            + "and the command will not retry on error.")
    int retryAttempts;

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
    RemoteTraversalService remoteTraversalService;

    @Inject
    PullService pullAssetsService;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        // Calculating the workspace path for files
        var workspaceFilesFolder = getOrCreateWorkspaceFilesDirectory(workspaceMixin.workspace());

        if (LocationUtils.isFolderURL(path)) { // Handling folders

                var includeFolderPatterns = parsePatternOption(includeFolderPatternsOption);
                var includeAssetPatterns = parsePatternOption(includeAssetPatternsOption);
                var excludeFolderPatterns = parsePatternOption(excludeFolderPatternsOption);
                var excludeAssetPatterns = parsePatternOption(excludeAssetPatternsOption);

                CompletableFuture<Pair<List<Exception>, TreeNode>> folderTraversalFuture = CompletableFuture.supplyAsync(
                        () -> {
                            // Service to handle the traversal of the folder
                            return remoteTraversalService.traverseRemoteFolder(
                                    path,
                                    nonRecursive ? 0 : null,
                                    true,
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
                    throw new IOException(
                            String.format("Error occurred while pulling folder info: [%s].", path));
                }

                // ---
                // Now we need to pull the contents based on the tree we found
            pullAssetsService.pullTree(output, result.getRight(), workspaceFilesFolder, !preserve,
                        includeEmptyFolders, failFast, retryAttempts);

            } else { // Handling single files

                CompletableFuture<AssetVersionsView> assetInformationFuture = CompletableFuture.supplyAsync(
                        () -> retrieveAssetInformation(path)
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
                    output.error(String.format("Error occurred while pulling asset info: [%s].",
                            path));
                    return CommandLine.ExitCode.SOFTWARE;
                }

                // Handle the pull of a single file
            pullAssetsService.pullFile(output, result, path, workspaceFilesFolder, !preserve,
                        failFast, retryAttempts);
            }

            output.info(String.format("%n%nOutput has been written to [%s]", workspaceFilesFolder.getAbsolutePath()));


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
        String encodedURL = encodePath(source);
        var response = assetAPI.assetByPath(ByPathRequest.builder().assetPath(encodedURL).build());
        return response.entity();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

}
