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
public class PushMixin extends GlobalMixin {

    @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "path",
            description = "local directory or file to push")
    public File pushPath;

    // Workspace is hidden because it is not used in the push command.
    // We already have a pushPath parameter to specify the path to push.
    // This is here for testing purposes.
    // We're not using here our WorkspaceMixin because this param is meant to be hidden.
    @CommandLine.Option(names = {"--workspace"},
            hidden = true,
            description = {"The workspace directory.",
                    "Current directory is used if not specified"})
    public File workspace;

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

    /**
     * Returns the path of the file. If no path is provided, it will return current working directory.
     *
     * @return The path of the file.
     */
    public Path path() {
        final Path workingDir = (this.workspace != null ? this.workspace.toPath() : Path.of("")).normalize().toAbsolutePath();
        if (null == pushPath) {
            return workingDir;
        }
        return workingDir.resolve(pushPath.toPath()).normalize().toAbsolutePath();
    }

    /**
     * Returns whether a path is provided.
     * @return true if a path is provided; false otherwise.
     */
    public boolean isUserProvidedPath(){
        return null != pushPath;
    }

    /**
     * Returns whether the watch mode is enabled.
     * @return true if the watch mode is enabled; false otherwise.
     */
    public boolean isWatchMode(){
        return null != interval;
    }

}
