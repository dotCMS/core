package com.dotcms.cli.common;

import java.io.File;
import java.nio.file.Path;
import picocli.CommandLine;

/**
 * The {@code PushMixin} class defines common options applicable to several push operations. These
 * include but not limited to parameters for managing how push operations handle local directories,
 * failure occurrences, and error retries. Adopting this class as a mixin allows different push
 * commands to share this common set of options.
 */
public class PushMixin {

    @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "path",
            description = "local directory or file to push")
    public File path;

    @CommandLine.Option(names = {"--dry-run"}, defaultValue = "false",
            description =
                    "When this option is enabled, the push process displays information about the changes that would be made on "
                            + "the remote server without actually pushing those changes. No modifications will be made to the remote server. "
                            + "By default, this option is disabled, and the changes will be applied to the remote server.")
    public boolean dryRun;

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
                    + "Useful for internal use when a push sub-command is called from the global push.",
            hidden = true,
            defaultValue = "false")
    public boolean noValidateUnmatchedArguments;

    /**
     * Returns the path of the file. If no path is provided, it will return current working directory.
     *
     * @return The path of the file.
     */
    public Path path() {
        if (null == path) {
            return Path.of("").toAbsolutePath();
        }
        return path.toPath();
    }

}
