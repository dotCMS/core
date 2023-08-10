package com.dotcms.cli.command.files;

import com.dotcms.api.client.files.PushService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import org.apache.commons.lang3.tuple.Triple;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static com.dotcms.cli.command.files.TreePrinter.*;

@ActivateRequestContext
@CommandLine.Command(
        name = FilesPush.NAME,
        header = "@|bold,blue dotCMS Files push|@",
        description = {
                " This command push files to the server.",
                "" // empty string here so we can have a new line
        }
)
public class FilesPush extends AbstractFilesCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "push";

    @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "source",
            description = "local directory or file to push")
    File source;

    @CommandLine.Option(names = {"-rf", "--removeFolders"}, defaultValue = "false",
            description =
                    "When this option is enabled, the push process allows the deletion of folders in the remote server. "
                            + "By default, this option is disabled, and folders will not be removed on the remote server.")
    boolean removeFolders;

    @CommandLine.Option(names = {"-ra", "--removeAssets"}, defaultValue = "false",
            description =
                    "When this option is enabled, the push process allows the deletion of assets in the remote server. "
                            + "By default, this option is disabled, and assets will not be removed on the remote server.")
    boolean removeAssets;

    @CommandLine.Option(names = {"--dry-run"}, defaultValue = "false",
            description =
                    "When this option is enabled, the push process displays information about the changes that would be made on " +
                            "the remote server without actually pushing those changes. No modifications will be made to the remote server. "
                            + "By default, this option is disabled, and the changes will be applied to the remote server.")
    boolean dryRun;

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

    @Inject
    PushService pushService;

    @Override
    public Integer call() throws Exception {

            // Getting the workspace
            var workspace = getWorkspaceDirectory(source);

            // If the source is not specified, we use the current directory
            if (source == null) {
                source = Paths.get("").toAbsolutePath().normalize().toFile();
            }

            CompletableFuture<List<Triple<List<Exception>, AssetsUtils.LocalPathStructure, TreeNode>>>
                    folderTraversalFuture = CompletableFuture.supplyAsync(
                    () -> {
                        // Service to handle the traversal of the folder
                        return pushService.traverseLocalFolders(output, workspace, source,
                                removeAssets, removeFolders, true, true);
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

            // Let's try to print these tree with some order
            result.sort((o1, o2) -> {
                var left = o1.getMiddle();
                var right = o2.getMiddle();
                return left.filePath().compareTo(right.filePath());
            });

            var count = 0;

            for (var treeNodeData : result) {

                var localPathStructure = treeNodeData.getMiddle();
                var treeNode = treeNodeData.getRight();

                StringBuilder sb = new StringBuilder();

                sb.append(count++ == 0 ? "\r\n" : "\n\n").
                        append(" ──────\n").
                        append(String.format(" @|bold Folder [%s]|@ --- Site: [%s] - Status [%s] - Language [%s] \n",
                                localPathStructure.filePath(),
                                localPathStructure.site(),
                                localPathStructure.status(),
                                localPathStructure.language()));

                var treeNodePushInfo = treeNode.collectTreeNodePushInfo();

                if (treeNodePushInfo.hasChanges()) {

                    var assetsToPushCount = treeNodePushInfo.assetsToPushCount();
                    if (assetsToPushCount > 0) {
                        sb.append(String.format(" Push Data: " +
                                        "@|bold [%s]|@ Assets to push: " +
                                        "(@|bold," + COLOR_NEW + " %s|@ New " +
                                        "- @|bold," + COLOR_MODIFIED + " %s|@ Modified) " +
                                        "- @|bold," + COLOR_DELETED + " [%s]|@ Assets to delete " +
                                        "- @|bold," + COLOR_NEW + " [%s]|@ Folders to push " +
                                        "- @|bold," + COLOR_DELETED + " [%s]|@ Folders to delete\n\n",
                                treeNodePushInfo.assetsToPushCount(),
                                treeNodePushInfo.assetsNewCount(),
                                treeNodePushInfo.assetsModifiedCount(),
                                treeNodePushInfo.assetsToDeleteCount(),
                                treeNodePushInfo.foldersToPushCount(),
                                treeNodePushInfo.foldersToDeleteCount()));
                    } else {
                        sb.append(String.format(" Push Data: " +
                                        "@|bold," + COLOR_NEW + " [%s]|@ Assets to push " +
                                        "- @|bold," + COLOR_DELETED + " [%s]|@ Assets to delete " +
                                        "- @|bold," + COLOR_NEW + " [%s]|@ Folders to push " +
                                        "- @|bold," + COLOR_DELETED + " [%s]|@ Folders to delete\n\n",
                                treeNodePushInfo.assetsToPushCount(),
                                treeNodePushInfo.assetsToDeleteCount(),
                                treeNodePushInfo.foldersToPushCount(),
                                treeNodePushInfo.foldersToDeleteCount()));
                    }

                    if (dryRun) {
                        TreePrinter.getInstance().formatByStatus(
                                sb,
                                AssetsUtils.statusToBoolean(localPathStructure.status()),
                                List.of(localPathStructure.language()),
                                treeNode,
                                false,
                                true,
                                localPathStructure.languageExists());
                    }

                    output.info(sb.toString());

                    // ---
                    // Pushing the tree
                    if (!dryRun) {
                        pushService.processTreeNodes(output, workspace.getAbsolutePath(),
                                localPathStructure, treeNode, treeNodePushInfo, failFast, retryAttempts);
                    }

                } else {
                    sb.append(" No changes to push\n\n");
                    output.info(sb.toString());
                }
            }

        return CommandLine.ExitCode.OK;
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
