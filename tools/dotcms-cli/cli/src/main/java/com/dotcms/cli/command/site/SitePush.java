package com.dotcms.cli.command.site;

import com.dotcms.api.client.push.PushService;
import com.dotcms.api.client.push.site.SiteComparator;
import com.dotcms.api.client.push.site.SiteFetcher;
import com.dotcms.api.client.push.site.SitePushHandler;
import com.dotcms.cli.command.DotPush;
import com.dotcms.cli.common.ApplyCommandOrder;
import com.dotcms.cli.common.FullPushOptionsMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PushMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.push.PushOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SitePush.NAME,
        header = "@|bold,blue Push sites|@",
        description = {
                "This command enables the pushing of sites to the server. It accommodates the "
                        + "specification of either a site file or a folder path.",
                "" // empty string to add a new line
        }
)
public class SitePush extends AbstractSiteCommand implements Callable<Integer>, DotPush {

    static final String NAME = "push";

    public static final String SITE_PUSH_OPTION_FORCE_EXECUTION = "forceExecution";

    static final String SITE_PUSH_MIXIN = "sitePushMixin";

    @CommandLine.Mixin
    FullPushOptionsMixin pushMixin;

    @CommandLine.Mixin(name = SITE_PUSH_MIXIN)
    SitePushMixin sitePushMixin;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    PushService pushService;

    @Inject
    SiteFetcher siteProvider;

    @Inject
    SiteComparator siteComparator;

    @Inject
    SitePushHandler sitePushHandler;

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

        return push();
    }

    private int push() throws Exception {

        // Make sure the path is within a workspace
        final Optional<Workspace> workspace = workspace();
        if (workspace.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("No valid workspace found at path: [%s]",
                            this.getPushMixin().pushPath.toPath()));
        }

        File inputFile = this.getPushMixin().path().toFile();
        if (!inputFile.isAbsolute() && inputFile.isFile()) {
            inputFile = Path.of(workspace.get().sites().toString(), inputFile.getName())
                    .toFile();
        }
        if (!inputFile.exists() || !inputFile.canRead()) {
            throw new IOException(String.format(
                    "Unable to access the path [%s] check that it does exist and that you have "
                            + "read permissions on it.", inputFile)
            );
        }

        // To make sure that if the user is passing a directory we use the sites folder
        if (inputFile.isDirectory()) {
            inputFile = workspace.get().sites().toFile();
        }

        // Execute the push
        pushService.push(
                inputFile,
                PushOptions.builder().
                        failFast(pushMixin.failFast).
                        allowRemove(sitePushMixin.removeSites).
                        disableAutoUpdate(pushMixin.isDisableAutoUpdate()).
                        maxRetryAttempts(pushMixin.retryAttempts).
                        dryRun(pushMixin.dryRun).
                        build(),
                output,
                siteProvider,
                siteComparator,
                sitePushHandler,
                Map.of(SITE_PUSH_OPTION_FORCE_EXECUTION, sitePushMixin.forceExecution)
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
    public PushMixin getPushMixin() {
        return pushMixin;
    }

    @Override
    public Optional<String> getCustomMixinName() {
        return Optional.of(SITE_PUSH_MIXIN);
    }

    @Override
    public int getOrder() {
        return ApplyCommandOrder.SITE.getOrder();
    }

    @Override
    public WorkspaceManager workspaceManager() {
        return workspaceManager;
    }

    @Override
    public Path workingRootDir() {
        final Optional<Workspace> workspace = workspace();
        if (workspace.isPresent()) {
            return workspace.get().sites();
        }
        throw new IllegalArgumentException("No valid workspace found.");
    }

}
