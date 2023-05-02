package com.dotcms.cli.command.files;

import com.dotcms.api.traversal.FolderTraversalService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

/**
 * Command to lists the files and directories in the specified directory.
 */
@ActivateRequestContext
@CommandLine.Command(
        name = FilesLs.NAME,
        header = "@|bold,blue dotCMS Files ls|@",
        description = {
                " This command lists the files and directories in the specified directory.",
                "" // empty string here so we can have a new line
        }
)
public class FilesLs extends AbstractFilesCommand implements Callable<Integer> {

    static final String NAME = "ls";

    @Parameters(index = "0", arity = "1", paramLabel = "path",
            description = "dotCMS path to the directory to list the contents of "
                    + "- Format: {domain}/{folder}")
    String folderPath;

    @Inject
    FolderTraversalService folderTraversalService;

    @Override
    public Integer call() throws Exception {
        try {

            CompletableFuture<TreeNode> folderTraversalFuture = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            // Service to handle the traversal of the folder
                            return folderTraversalService.traverse(folderPath, 0);
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    String.format("Folder traversal task failed for folder [%s]",
                                            folderPath), e);
                        }
                    });

            // ConsoleLoadingAnimation instance to handle the waiting "animation"
            ConsoleLoadingAnimation consoleLoadingAnimation = new ConsoleLoadingAnimation(
                    folderTraversalFuture,
                    ConsoleLoadingAnimation.ANIMATION_CHARS_SIMPLE,
                    250
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
                        "Error occurred while pulling folder info: [%s].", folderPath));
                return CommandLine.ExitCode.SOFTWARE;
            }

            // Display the result
            StringBuilder sb = new StringBuilder();
            shortFormat(sb, "", result, true);

            output.info(sb.toString());

        } catch (NotFoundException e) {
            output.error(String.format(
                    "Error occurred while pulling folder contents: [%s] with message: [%s].",
                    folderPath, e.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }

        return CommandLine.ExitCode.OK;
    }

}
