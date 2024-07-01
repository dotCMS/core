package com.dotcms.cli.command.language;

import com.dotcms.api.client.pull.PullService;
import com.dotcms.api.client.pull.language.LanguageFetcher;
import com.dotcms.api.client.pull.language.LanguagePullHandler;
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
        name = LanguagePull.NAME,
        header = "@|bold,blue Retrieves languages descriptors|@",
        description = {
                "  This command fetches and saves the descriptor information",
                "  for languages within the dotCMS instance. By default, it",
                "  retrieves descriptors for all languages, unless a specific",
                "  language's ISO code or ID is provided as an argument.",
                "  The descriptors are saved into files named after each language's",
                "  ISO code.",
                "",
                "  When a languages is pulled more than once, the existing descriptor file",
                "  is overwritten. All descriptor files are saved within the 'languages'",
                "  folder located in the dotCMS workspace, which is created in the",
                "  current directory by default, unless an alternative workspace is specified.",
                "",
                "  The output format for the descriptor files is JSON by default. However,",
                "  you can specify the YAML format using the @|yellow --format|@ option",
                "  if YAML is preferred.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class LanguagePull extends AbstractLanguageCommand implements Callable<Integer>, DotPull {

    public static final String NAME = "pull";

    static final String LANGUAGE_PULL_MIXIN = "languagePullMixin";

    @CommandLine.Mixin
    FullPullOptionsMixin pullMixin;

    @CommandLine.Mixin(name = LANGUAGE_PULL_MIXIN)
    LanguagePullMixin languagePullMixin;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    PullService pullService;

    @Inject
    LanguageFetcher languageProvider;

    @Inject
    LanguagePullHandler languagePullHandler;

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

        File languagesFolder = workspace.languages().toFile();
        if (!languagesFolder.exists() || !languagesFolder.canRead()) {
            throw new IOException(String.format(
                    "Unable to access the path [%s] check that it does exist and that you have "
                            + "read permissions on it.", languagesFolder)
            );
        }

        // Execute the pull
        pullService.pull(
                PullOptions.builder().
                        destination(languagesFolder).
                        contentKey(Optional.ofNullable(languagePullMixin.languageIdOrIso)).
                        outputFormat(pullMixin.inputOutputFormat().toString()).
                        isShortOutput(pullMixin.shortOutputOption().isShortOutput()).
                        failFast(pullMixin.failFast).
                        maxRetryAttempts(pullMixin.retryAttempts).
                        build(),
                output,
                languageProvider,
                languagePullHandler
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
        return ApplyCommandOrder.LANGUAGE.getOrder();
    }
    
}
