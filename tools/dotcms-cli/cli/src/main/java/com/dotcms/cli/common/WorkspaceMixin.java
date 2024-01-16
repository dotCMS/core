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
     * Checks if the workspace was explicitly set. This helps me to tell if the user specified a workspace or not.
     * Or we're simply using the current working directory. This is important information for the WorkspaceManager.
     * @return true if the workspace was explicitly set.
     */
    boolean userProvided(){
        return null != file;
    }

    /**
     * if no workspace is provided, it will return current working directory.
     * @return the workspace path.
     */
     Path workspace(){
        if(null == file){
            // if no workspace is provided, it will return current working directory.
            return Path.of("");
        }
        //a workspace was provided, so we need to return the path to it.
        return file.toPath();
    }

   /**
    * This method is used to create the WorkspaceParams object.
    * @return the WorkspaceParams object.
    */
    WorkspaceParams workspaceParams(){
        return WorkspaceParams.builder()
                .workspacePath(workspace())
                .userProvided(userProvided())
                .build();
    }


}
