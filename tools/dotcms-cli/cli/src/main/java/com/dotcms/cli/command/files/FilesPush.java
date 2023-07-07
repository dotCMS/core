package com.dotcms.cli.command.files;

import com.dotcms.api.client.files.PushService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.common.AssetsUtils;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

@ActivateRequestContext
@CommandLine.Command(
        name = FilesPush.NAME,
        header = "@|bold,blue dotCMS Files push|@",
        description = {
                " This command push files to the server.",
                "" // empty string here so we can have a new line
        }
)
public class FilesPush extends AbstractFilesCommand implements Callable<Integer> {

    static final String NAME = "push";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "source",
            description = "local directory or file to push")
    String source;

    @Inject
    PushService pushService;

    @Override
    public Integer call() throws Exception {

        try {

            CompletableFuture<List<Pair<AssetsUtils.LocalPathStructure, TreeNode>>> folderTraversalFuture = CompletableFuture.supplyAsync(
                    () -> {
                        // Service to handle the traversal of the folder
                        return pushService.traverseLocalFolders(output, source);
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
                        "Error occurred while pushing folder info: [%s].", source));
                return CommandLine.ExitCode.SOFTWARE;
            }

            // Display the result
            StringBuilder sb = new StringBuilder();
            TreePrinter.getInstance().formatForPush(sb, result, false);

            output.info(sb.toString());

        } catch (Exception e) {
            return handleFolderTraversalExceptions(source, e);
        }

        return CommandLine.ExitCode.OK;
    }

}
