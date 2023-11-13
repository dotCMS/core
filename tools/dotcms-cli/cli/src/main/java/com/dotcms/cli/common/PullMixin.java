package com.dotcms.cli.common;

import java.nio.file.Path;
import picocli.CommandLine;

/**
 * The {@code PullMixin} class defines common options applicable to several pull operations.
 * Adopting this class as a mixin allows different pull commands to share this common set of
 * options.
 */
public class PullMixin {

    @CommandLine.Option(names = {"-fmt",
            "--format"}, description = "Enum values: ${COMPLETION-CANDIDATES}")
    InputOutputFormat inputOutputFormat = InputOutputFormat.defaultFormat();

    @CommandLine.Mixin(name = "workspace")
    WorkspaceMixin workspaceMixin;

    @CommandLine.Mixin(name = "shorten")
    ShortOutputOptionMixin shortOutputOption;

    @CommandLine.Option(names = {"--noValidateUnmatchedArguments"},
            description = "Allows to skip the the validation of the unmatched arguments. "
                    + "Useful for internal use when a pull sub-command is called from the global pull.",
            hidden = true,
            defaultValue = "false")
    public boolean noValidateUnmatchedArguments;

    public InputOutputFormat inputOutputFormat() {
        return inputOutputFormat;
    }

    public Path workspace() {
        return workspaceMixin.workspace();
    }

    public ShortOutputOptionMixin shortOutputOption() {
        return shortOutputOption;
    }

}
