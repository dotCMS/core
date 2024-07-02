package com.dotcms.cli.command.files;

import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.EXCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_ASSET_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_EMPTY_FOLDERS;
import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_FOLDER_PATTERNS;
import static com.dotcms.api.client.pull.file.OptionConstants.NON_RECURSIVE;
import static com.dotcms.api.client.pull.file.OptionConstants.PRESERVE;

import com.dotcms.api.client.pull.PullService;
import com.dotcms.api.client.pull.file.FileFetcher;
import com.dotcms.api.client.pull.file.FilePullHandler;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.command.DotPull;
import com.dotcms.cli.common.ApplyCommandOrder;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PullMixin;
import com.dotcms.cli.common.WorkspaceParams;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.pull.PullOptions;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = FilesPull.NAME,
        header = "@|bold,blue dotCMS Files pull|@",
        description = {
                "  This command pulls files from the dotCMS instance.",
                "  By default, without a specified @|yellow 'path'|@ parameter, it retrieves files from",
                "  all the sites within the dotCMS instance.",
                "",
                "  Providing a @|yellow 'path'|@ parameter you can pull files from a specific site,",
                "  directory or file.",
                "  The format for the @|yellow 'path'|@ parameter is:",
                "  @|yellow //site|@ - @|yellow //site/folder|@ - @|yellow //site/folder/file|@.",
                "",
                "  The pulled files are saved withing the 'files' folder located in the dotCMS",
                "  workspace, which is created in the current directory by default, unless an",
                "  alternative workspace is specified.",
                "",
                "  @|bold Note:|@ Omitting the @|yellow 'path'|@ parameter triggers the pulling of all",
                "  files from all sites, which can be a very resource-intensive and",
                "  time-consuming process.",
                "" // empty string here so we can have a new line
        }
)
public class FilesPull extends AbstractFilesCommand implements Callable<Integer>, DotPull {

    static final String NAME = "pull";

    static final String FILE_PULL_MIXIN = "filePullMixin";

    @CommandLine.Mixin
    PullMixin pullMixin;

    @CommandLine.Mixin(name = FILE_PULL_MIXIN)
    FilesPullMixin filesPullMixin;

    @Inject
    PullService pullService;

    @Inject
    FileFetcher fileProvider;

    @Inject
    FilePullHandler filePullHandler;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {

        // When calling from the global pull we should avoid the validation of the unmatched
        // arguments as we may send arguments meant for other pull subcommands
        if (!pullMixin.noValidateUnmatchedArguments) {
            // Checking for unmatched arguments
            output.throwIfUnmatchedArguments(spec.commandLine());
        }

        // Make sure the path is within a workspace
        final WorkspaceParams params = this.getPullMixin().workspace();
        final Workspace workspace = workspaceManager.getOrCreate(params.workspacePath(), !params.userProvided());

        File filesFolder = workspace.files().toFile();
        if (!filesFolder.exists() || !filesFolder.canRead()) {
            throw new IOException(String.format(
                    "Unable to access the path [%s] check that it does exist and that you have "
                            + "read permissions on it.", filesFolder)
            );
        }

        var includeFolderPatterns = parsePatternOption(
                filesPullMixin.globMixin.includeFolderPatternsOption
        );
        var includeAssetPatterns = parsePatternOption(
                filesPullMixin.globMixin.includeAssetPatternsOption
        );
        var excludeFolderPatterns = parsePatternOption(
                filesPullMixin.globMixin.excludeFolderPatternsOption
        );
        var excludeAssetPatterns = parsePatternOption(
                filesPullMixin.globMixin.excludeAssetPatternsOption
        );

        var customOptions = Map.of(
                INCLUDE_FOLDER_PATTERNS, includeFolderPatterns,
                INCLUDE_ASSET_PATTERNS, includeAssetPatterns,
                EXCLUDE_FOLDER_PATTERNS, excludeFolderPatterns,
                EXCLUDE_ASSET_PATTERNS, excludeAssetPatterns,
                NON_RECURSIVE, filesPullMixin.nonRecursive,
                PRESERVE, filesPullMixin.preserve,
                INCLUDE_EMPTY_FOLDERS, filesPullMixin.includeEmptyFolders
        );

        // Execute the pull
        pullService.pull(
                PullOptions.builder().
                        destination(filesFolder).
                        contentKey(Optional.ofNullable(filesPullMixin.path)).
                        isShortOutput(pullMixin.shortOutputOption().isShortOutput()).
                        failFast(pullMixin.failFast).
                        maxRetryAttempts(pullMixin.retryAttempts).
                        customOptions(customOptions).
                        build(),
                output,
                fileProvider,
                filePullHandler
        );

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
    public PullMixin getPullMixin() {
        return pullMixin;
    }

    @Override
    public int getOrder() {
        return ApplyCommandOrder.FILES.getOrder();
    }

}
