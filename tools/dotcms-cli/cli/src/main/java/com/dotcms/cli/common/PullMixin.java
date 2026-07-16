package com.dotcms.cli.common;

import picocli.CommandLine;

/**
 * The {@code PullMixin} class defines common options applicable to several pull operations.
 * Adopting this class as a mixin allows different pull commands to share this common set of
 * options.
 */
public class PullMixin extends GlobalMixin {

    @CommandLine.Mixin(name = "workspace")
    WorkspaceMixin workspaceMixin;

    @CommandLine.Mixin(name = "shorten")
    ShortOutputOptionMixin shortOutputOption;

    @CommandLine.Option(names = {"-ff", "--fail-fast"}, defaultValue = "false",
            description =
                    "Stop at first failure and exit the command. By default, this option is disabled, "
                            + "and the command will continue on error.")
    public boolean failFast;

    @CommandLine.Option(names = {"--retry-attempts"}, defaultValue = "0",
            description =
                    "Number of retry attempts on errors. By default, this option is disabled, "
                            + "and the command will not retry on error.")
    public int retryAttempts;

    public WorkspaceParams workspace() {
        return workspaceMixin.workspaceParams();
    }

    public ShortOutputOptionMixin shortOutputOption() {
        return shortOutputOption;
    }

}
