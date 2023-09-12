package com.dotcms.cli.common;

import java.io.File;
import java.nio.file.Path;
import picocli.CommandLine;

/**
 * This class is used to inject the workspace option into the commands.
 */
public class WorkspaceMixin {

    /**
     * Command line option for specifying the workspace directory.
     * If not specified, the current directory will be used as the workspace.
     * <p>
     * Usage:
     * --workspace <directory>
     * <p>
     * Example:
     * --workspace /path/to/workspace
     */
    @CommandLine.Option(names = {"--workspace"},
            description = {"The workspace directory.",
                    "Current directory is used if not specified"})
    File file;

    /**
     * if no workspace is provided, it will return current working directory.
     * @return the workspace path.
     */
     public Path workspace(){
        if(null == file){
            return Path.of("");
        }
        return file.toPath();
    }


}
