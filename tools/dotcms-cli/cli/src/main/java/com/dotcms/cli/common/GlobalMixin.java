package com.dotcms.cli.common;

import picocli.CommandLine;

/**
 * The {@code GlobalMixin} class provides common command-line options that can be shared
 * across multiple global commands in the CLI application.
 * <p>
 * This class is designed to be extended by other mixin classes (like {@code PullMixin} and
 * {@code PushMixin}) to inherit common options while allowing specialized mixins to define
 * additional command-specific options.
 * <p>
 * Currently, it defines an option to disable validation of unmatched arguments, which is
 * particularly useful for internal command chaining where a command might be invoked from
 * a parent command with additional arguments.
 */

public class GlobalMixin {

    public final static String OPTION_NO_VALIDATE_UNMATCHED_ARGUMENTS =
            "--noValidateUnmatchedArguments";

    @CommandLine.Option(names = {OPTION_NO_VALIDATE_UNMATCHED_ARGUMENTS},
            description = "Allows to skip the the validation of the unmatched arguments. "
                    + "Useful for internal use when a pull/push sub-command is called from the "
                    + "global pull/push.",
            hidden = true,
            defaultValue = "false")
    public boolean noValidateUnmatchedArguments;

}
