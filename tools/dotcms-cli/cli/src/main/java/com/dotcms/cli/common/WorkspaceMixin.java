package com.dotcms.cli.common;

import java.io.File;
import java.nio.file.Path;
import picocli.CommandLine;

/**
 * This class is used to inject the workspace option into the commands.
 */
public class WorkspaceMixin {

    /**
     * The workspace option. invisible to the user. only meant to be used other commands or tests.
     */
    @CommandLine.Option(names = { "--workspace" }, hidden = true)
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
