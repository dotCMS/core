package com.dotcms.cli.command.files;

import com.dotcms.api.client.files.PullFilesService;
import com.dotcms.api.traversal.FolderTraversalService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

@ActivateRequestContext
@CommandLine.Command(
        name = FilesPull.NAME,
        header = "@|bold,blue dotCMS Files ls|@",
        description = {
                " This command lists the files and directories in the specified directory.",
                "" // empty string here so we can have a new line
        }
)
public class FilesPull extends AbstractFilesCommand implements Callable<Integer> {

    static final String NAME = "pull";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "source",
            description = "dotCMS path to the directory or file to pull "
                    + "- Format: //{site}/{folder} or //{site}/{folder}/{file}")
    String source;

    @CommandLine.Parameters(index = "1", arity = "1", paramLabel = "destination", defaultValue = ".",
            description = "Local root directory of the CLI project.")
    String destination;

    @CommandLine.Option(names = {"-r", "--recursive"}, defaultValue = "true",
            description = "Pulls directories and their contents recursively.")
    boolean recursive;

    @CommandLine.Option(names = {"-o", "--override"}, defaultValue = "true",
            description = "Overrides the local files with the ones from the server.")
    boolean override;

    @CommandLine.Option(names = {"-ee", "--excludeEmptyFolders"}, defaultValue = "true",
            description =
                    "When this option is enabled, the pull process will not create folders that do "
                            + "not contain any assets, as well as folders that have no children with assets. "
                            + "This can be useful for users who want to focus on the folder structure that "
                            + "contains assets, making the folder structure more concise and easier to navigate. By "
                            + "default, this option is enabled, and empty folders will not be created.")
    boolean excludeEmptyFolders;

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
    FolderTraversalService folderTraversalService;

    @Inject
    PullFilesService pullAssetsService;

    @Override
    public Integer call() throws Exception {

        try {

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
                output.error(String.format(
                        "Error occurred while pulling folder info: [%s].", source));
                return CommandLine.ExitCode.SOFTWARE;
            }

            // ---
            // Now we need to pull the contents based on the tree we found
            pullAssetsService.pull(output, result, destination, override, !excludeEmptyFolders);

            output.info("\n\nPull process finished successfully.");

        } catch (Exception e) {
            return handleFolderTraversalExceptions(source, e);
        }

        return CommandLine.ExitCode.OK;
    }

}
