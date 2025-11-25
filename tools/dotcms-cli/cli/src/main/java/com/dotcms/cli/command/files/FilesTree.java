package com.dotcms.cli.command.files;

import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.OutputOptionMixin;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import picocli.CommandLine;

/**
 * Command to display a hierarchical tree view of the files and subdirectories within the specified
 * directory.
 */
@ActivateRequestContext
@CommandLine.Command(
        name = FilesTree.NAME,
        header = "@|bold,blue dotCMS Files Tree|@",
        description = {
                " This command displays a hierarchical tree view of the files and ",
                " subdirectories within a specified directory.",
                "" // empty string here so we can have a new line
        }
)
public class FilesTree extends AbstractFilesListingCommand implements Callable<Integer>,
        DotCommand {

    static final String NAME = "tree";

    @CommandLine.Option(names = {"-d", "--depth"},
            description = "Limits the depth of the directory tree to <number> levels. "
                    + "The default value is 0, which means that only the files and directories in "
                    + "the root directory are displayed. If the <number> argument is not provided, "
                    + "there is no limit on the depth of the directory tree.")
    Integer depth;

    @Override
    public Integer call() throws Exception {
        return listing(depth);
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
