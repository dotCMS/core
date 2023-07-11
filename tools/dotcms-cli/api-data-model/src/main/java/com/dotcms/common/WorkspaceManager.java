package com.dotcms.common;

import com.dotcms.model.config.Workspace;
import java.io.IOException;
import java.nio.file.Path;

public interface WorkspaceManager {

    /**
     *
     * @param currentPath
     * @return
     * @throws IOException
     */
    Workspace resolve(Path currentPath) throws IOException;

    /**
     *
     * @return
     * @throws IOException
     */
    Workspace resolve() throws IOException;

}
