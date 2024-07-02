package com.dotcms.cli.command.files;

import static com.dotcms.cli.command.files.TreePrinter.COLOR_DELETED;
import static com.dotcms.cli.command.files.TreePrinter.COLOR_MODIFIED;
import static com.dotcms.cli.command.files.TreePrinter.COLOR_NEW;

import com.dotcms.api.client.files.PushService;
import com.dotcms.api.client.files.traversal.PushTraverseParams;
import com.dotcms.api.client.files.traversal.TraverseResult;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodePushInfo;
import com.dotcms.cli.command.DotPush;
import com.dotcms.cli.command.PushContext;
import com.dotcms.cli.common.ApplyCommandOrder;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PushMixin;
import com.dotcms.common.AssetsUtils;
import com.dotcms.common.LocalPathStructure;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.microprofile.context.ManagedExecutor;
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
public class FilesPush extends AbstractFilesCommand implements Callable<Integer>, DotPush {

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

    @Inject
    PushContext pushContext;

    @Inject
    ManagedExecutor executor;

    @Override
    public Integer call() throws Exception {

        // When calling from the global push we should avoid the validation of the unmatched
        // arguments as we may send arguments meant for other push subcommands
        if (!pushMixin.noValidateUnmatchedArguments) {
            // Checking for unmatched arguments
            output.throwIfUnmatchedArguments(spec.commandLine());
        }

        // Validating and resolving the workspace and path
        var resolvedWorkspaceAndPath = resolveWorkspaceAndPath();

        File finalInputFile = resolvedWorkspaceAndPath.getRight();
        CompletableFuture<List<TraverseResult>>
                folderTraversalFuture = executor.supplyAsync(
                () ->
                        // Service to handle the traversal of the folder
                        pushService.traverseLocalFolders(
                                output, resolvedWorkspaceAndPath.getLeft().root().toFile(),
                                finalInputFile, filesPushMixin.removeAssets,
                                filesPushMixin.removeFolders, false, pushMixin.failFast)
        );

        // ConsoleLoadingAnimation instance to handle the waiting "animation"
        ConsoleLoadingAnimation consoleLoadingAnimation = new ConsoleLoadingAnimation(
                output,
                folderTraversalFuture
        );

        CompletableFuture<Void> animationFuture = executor.runAsync(
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

        if (result.isEmpty()) {
            output.info(String.format("\r%n"
                    + " ──────%n"
                    + " No changes in %s to push%n%n", "Files"));
        } else {
            pushChangesIfAny(resolvedWorkspaceAndPath.getLeft().root(), result);
        }


        return CommandLine.ExitCode.OK;
    }

    /**
     * Processes the results of the traversal and pushes the changes to the server.
     * @param workspacePath The workspace path
     * @param result The traversal result
     */
    private void pushChangesIfAny(final Path workspacePath, final List<TraverseResult> result) {
        final String absolutePath = workspacePath.toFile().getAbsolutePath();
        var count = 0;
        for (var treeNodeData : result) {

            var localPaths = treeNodeData.localPaths();
            var optional = treeNodeData.treeNode();

            if (optional.isEmpty()) {
                continue;
            }

            final TreeNode treeNode = optional.get();

            var outputBuilder = new StringBuilder();

            header(count++, localPaths, outputBuilder);

            var treeNodePushInfo = treeNode.collectPushInfo();

            if (treeNodePushInfo.hasChanges()) {

                changesSummary(treeNodePushInfo, outputBuilder);

                if (pushMixin.dryRun) {
                    dryRunSummary(localPaths, treeNode, outputBuilder);
                }

                output.info(outputBuilder.toString());

                // ---
                // Pushing the tree
                if (!pushMixin.dryRun) {

                    pushService.processTreeNodes(output, treeNodePushInfo,
                            PushTraverseParams.builder()
                                    .workspacePath(absolutePath)
                                    .rootNode(treeNode)
                                    .localPaths(localPaths)
                                    .failFast(pushMixin.failFast)
                                    .maxRetryAttempts(pushMixin.retryAttempts)
                                    .pushContext(pushContext)
                                    .build()
                    );
                }

            } else {
                outputBuilder.
                        append("\r\n").
                        append(" ──────\n").
                        append(String.format(" No changes in %s to push%n%n", "Files"));
                output.info(outputBuilder.toString());
            }
        }
    }

    /**
     * Resolves the workspace and path for the current operation.
     *
     * @return A Pair object containing the Workspace and File objects representing the resolved
     * workspace and path, respectively.
     * @throws IOException If there is an error accessing the path or if no valid workspace is
     *                     found.
     */
    private Pair<Workspace, File> resolveWorkspaceAndPath() throws IOException {

        // Make sure the path is within a workspace
        final Optional<Workspace> workspace = workspace();
        if (workspace.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("No valid workspace found at path: [%s]",
                            this.getPushMixin().path()));
        }

        File inputFile = this.getPushMixin().path().toFile();
        if (!inputFile.isAbsolute()) {
            // If the path is not absolute, we assume it is relative to the files folder
            inputFile = Path.of(
                    workspace.get().files().toString(), inputFile.getPath()
            ).toFile();
        }
        if (!inputFile.exists() || !inputFile.canRead()) {
            throw new IOException(String.format(
                    "Unable to access the path [%s] check that it does exist and that you have "
                            + "read permissions on it.", inputFile)
            );
        }

        return Pair.of(workspace.get(), inputFile);
    }

    private void header(int count, LocalPathStructure localPaths,
            StringBuilder outputBuilder) {
        outputBuilder.append(count == 0 ? "\r\n" : "\n\n").
                append(" ──────\n").
                append(String.format(
                        " @|bold Folder [%s]|@ --- Site: [%s] - Status [%s] - Language [%s] %n",
                        localPaths.filePath(),
                        localPaths.site(),
                        localPaths.status(),
                        localPaths.language()));
    }

    private void dryRunSummary(LocalPathStructure localPaths, TreeNode treeNode,
            StringBuilder outputBuilder) {
        TreePrinter.getInstance().formatByStatus(
                outputBuilder,
                AssetsUtils.statusToBoolean(localPaths.status()),
                List.of(localPaths.language()),
                treeNode,
                false,
                true,
                localPaths.languageExists());
    }

    private void changesSummary(TreeNodePushInfo pushInfo, StringBuilder outputBuilder) {
        var assetsToPushCount = pushInfo.assetsToPushCount();
        if (assetsToPushCount > 0) {
            outputBuilder.append(String.format(" Push Data: " +
                            "@|bold [%s]|@ Assets to push: " +
                            "(@|bold,%s %s|@ New " +
                            "- @|bold,%s %s|@ Modified) " +
                            "- @|bold,%s [%s]|@ Assets to delete " +
                            "- @|bold,%s [%s]|@ Folders to push " +
                            "- @|bold,%s [%s]|@ Folders to delete\n\n",
                    pushInfo.assetsToPushCount(),
                    COLOR_MODIFIED, pushInfo.assetsNewCount(),
                    COLOR_DELETED, pushInfo.assetsModifiedCount(),
                    COLOR_NEW, pushInfo.assetsToDeleteCount(),
                    COLOR_NEW, pushInfo.foldersToPushCount(),
                    COLOR_DELETED, pushInfo.foldersToDeleteCount())
            );
        } else {
            outputBuilder.append(String.format(" Push Data: " +
                            "@|bold,%s [%s]|@ Assets to push " +
                            "- @|bold,%s [%s]|@ Assets to delete " +
                            "- @|bold,%s [%s]|@ Folders to push " +
                            "- @|bold,%s [%s]|@ Folders to delete\n\n",
                    COLOR_NEW, pushInfo.assetsToPushCount(),
                    COLOR_DELETED, pushInfo.assetsToDeleteCount(),
                    COLOR_NEW, pushInfo.foldersToPushCount(),
                    COLOR_DELETED, pushInfo.foldersToDeleteCount()));
        }
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

    @Override
    public int getOrder() {
        return ApplyCommandOrder.FILES.getOrder();
    }

    @Override
    public WorkspaceManager workspaceManager() {
        return workspaceManager;
    }

    @Override
    public Path workingRootDir() {
        final Optional<Workspace> workspace = workspace();
        if (workspace.isPresent()) {
            return workspace.get().files();
        }
        throw new IllegalArgumentException("No valid workspace found.");
    }
}
