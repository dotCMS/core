package com.dotcms.cli.command.site;

import com.dotcms.api.client.pull.PullService;
import com.dotcms.api.client.pull.site.SiteFetcher;
import com.dotcms.api.client.pull.site.SitePullHandler;
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
@CommandLine.Command(name = SitePull.NAME,
        header = "@|bold,blue Retrieves sites descriptors|@",
        description = {
                "  This command fetches and saves the descriptor information",
                "  for sites within the dotCMS instance. By default, it ",
                "  retrieves descriptors for all sites, unless a specific site's",
                "  name or ID is provided as an argument. The descriptors are",
                "  saved into files named after each site's hostname.",
                "",
                "  When a site is pulled more than once, the existing descriptor file",
                "  is overwritten. All descriptor files are saved within the 'sites'",
                "  folder located in the dotCMS workspace, which is created in the",
                "  current directory by default, unless an alternative workspace is specified.",
                "",
                "  The output format for the descriptor files is JSON by default. However,",
                "  you can specify the YAML format using the @|yellow --format|@ option",
                "  if YAML is preferred.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class SitePull extends AbstractSiteCommand implements Callable<Integer>, DotPull {

    static final String NAME = "pull";

    static final String SITE_PULL_MIXIN = "sitePullMixin";

    @CommandLine.Mixin
    FullPullOptionsMixin pullMixin;

    @CommandLine.Mixin(name = SITE_PULL_MIXIN)
    SitePullMixin sitePullMixin;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    PullService pullService;

    @Inject
    SiteFetcher siteProvider;

    @Inject
    SitePullHandler sitePullHandler;

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

        return pull();
    }

    private int pull() throws IOException {

        // Make sure the path is within a workspace
        final WorkspaceParams params = this.getPullMixin().workspace();
        final Workspace workspace = workspaceManager.getOrCreate(params.workspacePath(), !params.userProvided());

        File sitesFolder = workspace.sites().toFile();
        if (!sitesFolder.exists() || !sitesFolder.canRead()) {
            throw new IOException(String.format(
                    "Unable to access the path [%s] check that it does exist and that you have "
                            + "read permissions on it.", sitesFolder)
            );
        }

        // Execute the pull
        pullService.pull(
                PullOptions.builder().
                        destination(sitesFolder).
                        contentKey(Optional.ofNullable(sitePullMixin.siteNameOrId)).
                        outputFormat(pullMixin.inputOutputFormat().toString()).
                        isShortOutput(pullMixin.shortOutputOption().isShortOutput()).
                        failFast(pullMixin.failFast).
                        maxRetryAttempts(pullMixin.retryAttempts).
                        build(),
                output,
                siteProvider,
                sitePullHandler
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
        return ApplyCommandOrder.SITE.getOrder();
    }

}
