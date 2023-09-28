package com.dotcms.cli.command.files;

import static com.dotcms.cli.command.files.TreePrinter.COLOR_DELETED;
import static com.dotcms.cli.command.files.TreePrinter.COLOR_MODIFIED;
import static com.dotcms.cli.command.files.TreePrinter.COLOR_NEW;

import com.dotcms.api.client.files.PushService;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.command.DotPush;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PushMixin;
import com.dotcms.common.AssetsUtils;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.apache.commons.lang3.tuple.Triple;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = FilesPush.NAME,
        header = "@|bold,blue dotCMS Files push|@",
        description = {
                " This command push files to the server.",
                "" // empty string here so we can have a new line
        }
)
public class FilesPush extends AbstractFilesCommand implements Callable<Integer>,
        DotCommand, DotPush {

    static final String NAME = "push";
    static final String FILES_PUSH_MIXIN = "filesPushMixin";

    @CommandLine.Mixin
    PushMixin pushMixin;

    @CommandLine.Mixin(name = FILES_PUSH_MIXIN)
    FilesPushMixin filesPushMixin;

    @Inject
    PushService pushService;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {

        // When calling from the global push we should avoid the validation of the unmatched
        // arguments as we may send arguments meant for other push subcommands
        if (!pushMixin.noValidateUnmatchedArguments) {
            // Checking for unmatched arguments
            output.throwIfUnmatchedArguments(spec.commandLine());
        }

        // Getting the workspace
        var workspace = getWorkspaceDirectory(pushMixin.path());

        CompletableFuture<List<Triple<List<Exception>, AssetsUtils.LocalPathStructure, TreeNode>>>
                folderTraversalFuture = CompletableFuture.supplyAsync(
                () -> {
                    // Service to handle the traversal of the folder
                    return pushService.traverseLocalFolders(output, workspace,
                            pushMixin.path().toFile(),
                            filesPushMixin.removeAssets, filesPushMixin.removeFolders,
                            true, true);
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
                    "Error occurred while pushing folder info: [%s].",
                    pushMixin.path().toAbsolutePath()));
            return CommandLine.ExitCode.SOFTWARE;
        }

        // Let's try to print these tree with some order
        result.sort((o1, o2) -> {
            var left = o1.getMiddle();
            var right = o2.getMiddle();
            return left.filePath().compareTo(right.filePath());
        });

        var count = 0;

        if (!result.isEmpty()) {
            for (var treeNodeData : result) {

                var localPathStructure = treeNodeData.getMiddle();
                var treeNode = treeNodeData.getRight();

                var outputBuilder = new StringBuilder();

                outputBuilder.append(count++ == 0 ? "\r\n" : "\n\n").
                        append(" ──────\n").
                        append(String.format(
                                " @|bold Folder [%s]|@ --- Site: [%s] - Status [%s] - Language [%s] \n",
                                localPathStructure.filePath(),
                                localPathStructure.site(),
                                localPathStructure.status(),
                                localPathStructure.language()));

                var treeNodePushInfo = treeNode.collectTreeNodePushInfo();

                if (treeNodePushInfo.hasChanges()) {

                    var assetsToPushCount = treeNodePushInfo.assetsToPushCount();
                    if (assetsToPushCount > 0) {
                        outputBuilder.append(String.format(" Push Data: " +
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
                        outputBuilder.append(String.format(" Push Data: " +
                                        "@|bold," + COLOR_NEW + " [%s]|@ Assets to push " +
                                        "- @|bold," + COLOR_DELETED + " [%s]|@ Assets to delete " +
                                        "- @|bold," + COLOR_NEW + " [%s]|@ Folders to push " +
                                        "- @|bold," + COLOR_DELETED + " [%s]|@ Folders to delete\n\n",
                                treeNodePushInfo.assetsToPushCount(),
                                treeNodePushInfo.assetsToDeleteCount(),
                                treeNodePushInfo.foldersToPushCount(),
                                treeNodePushInfo.foldersToDeleteCount()));
                    }

                    if (pushMixin.dryRun) {
                        TreePrinter.getInstance().formatByStatus(
                                outputBuilder,
                                AssetsUtils.statusToBoolean(localPathStructure.status()),
                                List.of(localPathStructure.language()),
                                treeNode,
                                false,
                                true,
                                localPathStructure.languageExists());
                    }

                    output.info(outputBuilder.toString());

                    // ---
                    // Pushing the tree
                    if (!pushMixin.dryRun) {
                        pushService.processTreeNodes(output, workspace.getAbsolutePath(),
                                localPathStructure, treeNode, treeNodePushInfo, pushMixin.failFast,
                                pushMixin.retryAttempts);
                    }

                } else {
                    outputBuilder.
                            append("\r\n").
                            append(" ──────\n").
                            append(
                                    String.format(" No changes in %s to push%n%n", "Files"));
                    output.info(outputBuilder.toString());
                }
            }
        } else {
            output.info(String.format("\r%n"
                    + " ──────%n"
                    + " No changes in %s to push%n%n", "Files"));
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

    @Override
    public PushMixin getPushMixin() {
        return pushMixin;
    }

    @Override
    public Optional<String> getCustomMixinName() {
        return Optional.of(FILES_PUSH_MIXIN);
    }

}
