package com.dotcms.cli.command.files;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import org.jboss.logging.Logger;
import picocli.CommandLine;

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
     * @param workspacePath the file object representing a directory within the workspace
     * @return the workspace files directory
     * @throws IOException if an I/O error occurs while creating the directory
     */
    protected File getOrCreateWorkspaceFilesDirectory(final Path workspacePath) throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate(workspacePath);
        return workspace.files().toFile();
    }

    /**
     *
     * @param path represents a directory within the workspace
     * @return the workspace files directory
     * @throws IllegalArgumentException if a valid workspace is not found from the provided path
     */
    protected File getWorkspaceDirectory(final Path path) {

        final var workspace = workspaceManager.findWorkspace(path);

        if (workspace.isPresent()) {
            return workspace.get().root().toFile();
        }

        throw new IllegalArgumentException(
                String.format("No valid workspace found at path: [%s]", path.toAbsolutePath()));
    }


}
