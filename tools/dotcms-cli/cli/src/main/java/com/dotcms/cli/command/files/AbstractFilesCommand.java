package com.dotcms.cli.command.files;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;

public abstract class AbstractFilesCommand {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOptionMixin helpOption;

    @Inject
    protected RestClientFactory clientFactory;

    @Inject
    Logger logger;

    /**
     * Handles exceptions thrown during the execution of the "tree" and "ls" commands.
     *
     * @param folderPath the path of the folder that was being pulled
     * @param throwable  the exception that was thrown
     * @return the exit code to be used for the command line interface
     */
    protected int handleFolderTraversalExceptions(String folderPath, Throwable throwable) {

        logger.debug(String.format("Error occurred while processing: [%s] with message: [%s].",
                folderPath, throwable.getMessage()), throwable);

        if (throwable instanceof CompletionException) {

            Throwable cause = throwable.getCause();
            if (cause instanceof IllegalArgumentException) {

                output.error(String.format(
                        "Error occurred while processing: [%s] with message: [%s].",
                        folderPath, cause.getMessage()));
                return CommandLine.ExitCode.USAGE;
            } else {

                output.error(String.format(
                        "Error occurred while processing: [%s] with message: [%s].",
                        folderPath, throwable.getMessage()));
                return CommandLine.ExitCode.SOFTWARE;
            }
        } else if (throwable instanceof IllegalArgumentException) {

            output.error(String.format(
                    "Error occurred while processing: [%s] with message: [%s].",
                    folderPath, throwable.getMessage()));
            return CommandLine.ExitCode.USAGE;
        } else if (throwable instanceof NotFoundException) {

            output.error(String.format(
                    "Error occurred while processing: [%s] with message: [%s].",
                    folderPath, throwable.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        } else {
            output.error(String.format(
                    "Error occurred while processing: [%s] with message: [%s].",
                    folderPath, throwable.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

    /**
     * Parses the pattern option string into a set of patterns.
     *
     * @param patterns the pattern option string containing patterns separated by commas
     * @return a set of parsed patterns
     */
    protected Set<String> parsePatternOption(String patterns) {

        var patternsSet = new HashSet<String>();

        if (patterns == null) {
            return patternsSet;
        }

        for (String pattern : patterns.split(",")) {
            patternsSet.add(pattern.trim());
        }

        return patternsSet;
    }

}
