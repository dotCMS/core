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

    @CommandLine.Option(names = {"-r", "--recursive"}, defaultValue = "false",
            description = "Pulls directories and their contents recursively.")
    boolean recursive;

    @CommandLine.Option(names = {"-o", "--override"}, defaultValue = "true",
            description = "Overrides the local files with the ones from the server.")
    boolean override;

    @Inject
    FolderTraversalService folderTraversalService;

    @Inject
    PullFilesService pullAssetsService;

    @Override
    public Integer call() throws Exception {

        try {

            CompletableFuture<TreeNode> folderTraversalFuture = CompletableFuture.supplyAsync(
                    () -> {
                        // Service to handle the traversal of the folder
                        return folderTraversalService.traverse(
                                source,
                                recursive ? null : 0,
                                null,
                                null,
                                null,
                                null
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
            pullAssetsService.pull(output, result, destination, override);

            output.info("\n\nPull process finished successfully.");

        } catch (Exception e) {
            return handleFolderTraversalExceptions(source, e);
        }

        return CommandLine.ExitCode.OK;
    }

}
