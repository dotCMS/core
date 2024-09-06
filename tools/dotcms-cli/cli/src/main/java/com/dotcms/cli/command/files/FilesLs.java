package com.dotcms.cli.command.files;

import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.OutputOptionMixin;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import picocli.CommandLine;

/**
 * Command to lists the files and directories in the specified directory.
 */
@ActivateRequestContext
@CommandLine.Command(
        name = FilesLs.NAME,
        header = "@|bold,blue dotCMS Files ls|@",
        description = {
                " This command lists the files and directories in the specified directory.",
                "" // empty string here so we can have a new line
        }
)
public class FilesLs extends AbstractFilesListingCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "ls";

    @Override
    public Integer call() throws Exception {
        return listing(0);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

}
