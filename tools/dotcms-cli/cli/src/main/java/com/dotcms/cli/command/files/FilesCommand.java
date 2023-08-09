package com.dotcms.cli.command.files;

import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * This class is responsible for handling the "files" command.
 */
@CommandLine.Command(
        name = com.dotcms.cli.command.files.FilesCommand.NAME,
        header = "@|bold,blue Files operations.|@",
        description = {
                " Use the list of available sub-commands to manage files.",
                " Use @|yellow --help|@ to see the available subcommands.",
                " For help on a specific subcommand do @|yellow files [SUBCOMMAND] --help|@",
                " to see all available options and params.",
                "" // empty line to separate from the subcommands
        },
        subcommands = {
                FilesTree.class,
                FilesLs.class,
                FilesPull.class,
                FilesPush.class
        }
)
public class FilesCommand implements Callable<Integer> {

    static final String NAME = "files";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOptionMixin;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        spec.commandLine().usage(output.out());
        return CommandLine.ExitCode.OK;
    }
}
