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
    public File pushPath;

    @CommandLine.Option(names = {"--dry-run"}, defaultValue = "false",
            description = {
                "When this option is enabled, the push process displays information about the changes that would be made on ",
                "the remote server without actually pushing those changes. No modifications will be made to the remote server. ",
                "By default, this option is disabled, and the changes will be applied to the remote server."
    })
    public boolean dryRun;

    @CommandLine.Option(names = {"-ff", "--fail-fast"}, defaultValue = "false",
            description = {
                "Stop at first failure and exit the command. By default, this option is disabled, ",
                "and the command will continue on error."
            }
    )
    public boolean failFast;

    @CommandLine.Option(names = {"--retry-attempts"}, defaultValue = "0",
            description = {
                "Number of retry attempts on errors. By default, this option is disabled, ",
                "and the command will not retry on error."
            })
    public int retryAttempts;

    @CommandLine.Option(names = {"-w","--watch"},
            arity = "0..1",
            paramLabel = "watch",
            fallbackValue = "2",
            description = {
               "When this option is enabled the tool observes changes in the file system within the push path",
               "If a change is detected the push command being executed gets triggered. ",
               "The auto-update feature is disabled when watch mode is on",
                "The default watch interval is 2 seconds, but it can be specified passing an integer value with this option.",
                "e.g. --watch 5"
            }
    )
    public Integer interval;


    @CommandLine.Option(names = {"--noValidateUnmatchedArguments"},
            description = {
                  "Allows skipping the the validation of the unmatched arguments. ",
                  "Useful for internal use when a push sub-command is called from the global push."
            },
            hidden = true,
            defaultValue = "false")
    public boolean noValidateUnmatchedArguments;

    /**
     * Returns the path of the file. If no path is provided, it will return current working directory.
     *
     * @return The path of the file.
     */
    public Path path() {
        if (null == pushPath) {
            return Path.of("").toAbsolutePath();
        }
        return pushPath.toPath();
    }

    public boolean isWatchMode(){
        return null != interval;
    }

}
