package com.dotcms.common;

import com.dotcms.model.config.Workspace;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * This interface provides methods for resolving the workspace.
 */
public interface WorkspaceManager {

    /**
     * Finds the project root directory from any given path.
     * @param currentPath
     * @return
     * @throws IOException
     */
    Workspace getOrCreate(Path currentPath) throws IOException;

    /**
     * finds the project root directory from the current working directory.
     * @return
     * @throws IOException
     */
    Workspace getOrCreate() throws IOException;

    /**
     * finds the project root directory from any given path.
     * @param currentPath
     * @return
     */
    Optional<Workspace> findWorkspace(Path currentPath);

    /**
     * finds the project root directory from the current working directory.
     * @return
     */
    Optional<Workspace> findWorkspace();

    /**
     * destroy a new workspace.
     * @param workspace
     * @throws IOException
     */
    void destroy(final Workspace workspace) throws IOException;

}
