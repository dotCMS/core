package com.dotcms.cli.command.files;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import java.util.concurrent.CompletionException;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import picocli.CommandLine;

public abstract class AbstractFilesCommand {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOptionMixin helpOption;

    @Inject
    protected RestClientFactory clientFactory;

    /**
     * Handles exceptions thrown during the execution of the "tree" and "ls" commands.
     *
     * @param folderPath the path of the folder that was being pulled
     * @param throwable  the exception that was thrown
     * @return the exit code to be used for the command line interface
     */
    public int handleFolderTraversalExceptions(String folderPath, Throwable throwable) {

        if (throwable instanceof CompletionException) {

            Throwable cause = throwable.getCause();
            if (cause instanceof IllegalArgumentException) {

                output.error(String.format(
                        "Error occurred while pulling folder contents: [%s] with message: [%s].",
                        folderPath, cause.getMessage()));
                return CommandLine.ExitCode.USAGE;
            } else {

                output.error(String.format(
                        "Error occurred while pulling folder contents: [%s] with message: [%s].",
                        folderPath, throwable.getMessage()));
                return CommandLine.ExitCode.SOFTWARE;
            }
        } else if (throwable instanceof IllegalArgumentException) {

            output.error(String.format(
                    "Error occurred while pulling folder contents: [%s] with message: [%s].",
                    folderPath, throwable.getMessage()));
            return CommandLine.ExitCode.USAGE;
        } else if (throwable instanceof NotFoundException) {

            output.error(String.format(
                    "Error occurred while pulling folder contents: [%s] with message: [%s].",
                    folderPath, throwable.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        } else {
            output.error(String.format(
                    "Error occurred while pulling folder contents: [%s] with message: [%s].",
                    folderPath, throwable.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

}
