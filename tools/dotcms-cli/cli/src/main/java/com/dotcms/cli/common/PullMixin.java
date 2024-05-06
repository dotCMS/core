package com.dotcms.cli.common;

import picocli.CommandLine;

/**
 * The {@code PullMixin} class defines common options applicable to several pull operations.
 * Adopting this class as a mixin allows different pull commands to share this common set of
 * options.
 */
public class PullMixin {

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

    @CommandLine.Option(names = {"--noValidateUnmatchedArguments"},
            description = "Allows to skip the the validation of the unmatched arguments. "
                    + "Useful for internal use when a pull sub-command is called from the global pull.",
            hidden = true,
            defaultValue = "false")
    public boolean noValidateUnmatchedArguments;

    public WorkspaceParams workspace() {
        return workspaceMixin.workspaceParams();
    }

    public ShortOutputOptionMixin shortOutputOption() {
        return shortOutputOption;
    }

}
