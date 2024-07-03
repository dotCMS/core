package com.dotcms.cli.command.contenttype;

import com.dotcms.api.client.pull.PullService;
import com.dotcms.api.client.pull.contenttype.ContentTypeFetcher;
import com.dotcms.api.client.pull.contenttype.ContentTypePullHandler;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.command.DotPull;
import com.dotcms.cli.common.ApplyCommandOrder;
import com.dotcms.cli.common.FullPullOptionsMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PullMixin;
import com.dotcms.cli.common.WorkspaceParams;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.pull.PullOptions;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = ContentTypePull.NAME,
        header = "@|bold,blue Retrieves Content-types descriptors|@",
        description = {
                "  This command fetches and saves the descriptor information",
                "  for Content-types within the dotCMS instance. By default, it",
                "  retrieves descriptors for all Content-types, unless a specific",
                "  Content-type's name or ID is provided as an argument.",
                "  The descriptors are saved into files named after each Content-type's",
                "  variable name.",
                "",
                "  When a Content-type is pulled more than once, the existing descriptor file",
                "  is overwritten. All descriptor files are saved within the 'content-types'",
                "  folder located in the dotCMS workspace, which is created in the",
                "  current directory by default, unless an alternative workspace is specified.",
                "",
                "  The output format for the descriptor files is JSON by default. However,",
                "  you can specify the YAML format using the @|yellow --format|@ option",
                "  if YAML is preferred.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class ContentTypePull extends AbstractContentTypeCommand implements Callable<Integer>, DotPull {

    static final String NAME = "pull";

    static final String CONTENT_TYPE_PULL_MIXIN = "contentTypePullMixin";

    @CommandLine.Mixin
    FullPullOptionsMixin pullMixin;

    @CommandLine.Mixin(name = CONTENT_TYPE_PULL_MIXIN)
    ContentTypePullMixin contentTypePullMixin;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    PullService pullService;

    @Inject
    ContentTypeFetcher contentTypeProvider;

    @Inject
    ContentTypePullHandler contentTypePullHandler;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws IOException {

        // When calling from the global pull we should avoid the validation of the unmatched
        // arguments as we may send arguments meant for other pull subcommands
        if (!pullMixin.noValidateUnmatchedArguments) {
            // Checking for unmatched arguments
            output.throwIfUnmatchedArguments(spec.commandLine());
        }

        // Make sure the path is within a workspace
        final WorkspaceParams params = this.getPullMixin().workspace();
        final Workspace workspace = workspaceManager.getOrCreate(params.workspacePath(), !params.userProvided());

        File contentTypesFolder = workspace.contentTypes().toFile();
        if (!contentTypesFolder.exists() || !contentTypesFolder.canRead()) {
            throw new IOException(String.format(
                    "Unable to access the path [%s] check that it does exist and that you have "
                            + "read permissions on it.", contentTypesFolder)
            );
        }

        // Execute the pull
        pullService.pull(
                PullOptions.builder().
                        destination(contentTypesFolder).
                        contentKey(Optional.ofNullable(contentTypePullMixin.idOrVar)).
                        outputFormat(pullMixin.inputOutputFormat().toString()).
                        isShortOutput(pullMixin.shortOutputOption().isShortOutput()).
                        failFast(pullMixin.failFast).
                        maxRetryAttempts(pullMixin.retryAttempts).
                        build(),
                output,
                contentTypeProvider,
                contentTypePullHandler
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
    public Optional<String> getCustomMixinName() {
        return Optional.empty();
    }

    @Override
    public int getOrder() {
        return ApplyCommandOrder.CONTENT_TYPE.getOrder();
    }

}
