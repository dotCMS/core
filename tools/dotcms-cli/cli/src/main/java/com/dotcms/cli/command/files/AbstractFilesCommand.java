package com.dotcms.cli.command.files;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    protected WorkspaceManager workspaceManager;

    @Inject
    Logger logger;

    /**
     * Handles exceptions thrown during the execution of the files commands.
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

    /**
     * Returns the directory where workspace files are stored. If the directory does not exist,
     * it will be created.
     *
     * @param fromFile the file object representing a directory within the workspace, or null if not specified
     * @return the workspace files directory
     * @throws IOException if an I/O error occurs while creating the directory
     */
    protected File getOrCreateWorkspaceFilesDirectory(final File fromFile) throws IOException {

        String fromPath;
        if (fromFile == null) {
            // If the workspace is not specified, we use the current directory
            fromPath = Paths.get("").toAbsolutePath().normalize().toString();
        } else {
            fromPath = fromFile.getAbsolutePath();
        }

        final Path path = Paths.get(fromPath);
        final Workspace workspace = workspaceManager.getOrCreate(path);
        return workspace.files().toFile();
    }

    /**
     * Returns the directory where the workspace is.
     *
     * @param fromFile the file object representing a directory within the workspace, or null if not specified
     * @return the workspace files directory
     * @throws IllegalArgumentException if a valid workspace is not found from the provided path
     */
    protected File getWorkspaceDirectory(final File fromFile) {

        String fromPath;
        if (fromFile == null) {
            // If the workspace is not specified, we use the current directory
            fromPath = Paths.get("").toAbsolutePath().normalize().toString();
        } else {
            fromPath = fromFile.getAbsolutePath();
        }

        final Path path = Paths.get(fromPath);
        final var workspace = workspaceManager.findWorkspace(path);

        if (workspace.isPresent()) {
            return workspace.get().root().toFile();
        }

        throw new IllegalArgumentException(String.format("Not valid workspace found from path: [%s]", fromPath));
    }

}
