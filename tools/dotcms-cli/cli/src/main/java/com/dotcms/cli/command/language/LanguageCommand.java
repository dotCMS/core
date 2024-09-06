package com.dotcms.cli.command.language;

import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
        name = com.dotcms.cli.command.language.LanguageCommand.NAME,
        aliases = { com.dotcms.cli.command.language.LanguageCommand.ALIAS },
        header = "@|bold,blue Language operations.|@",
        description = {
                " Use the list of available sub-commands to manage languages.",
                " Use @|yellow --help|@ to see the available subcommands.",
                " For help on a specific subcommand do @|yellow language [SUBCOMMAND] --help|@",
                " to see all available options and params.",
                "" // empty line to separate from the subcommands
        },
        subcommands = {
                LanguagePull.class,
                LanguageFind.class,
                LanguagePush.class,
                LanguageRemove.class
        }
)
/**
 * Super command for language operations
 * @author nollymar
 */
public class LanguageCommand implements Callable<Integer>, DotCommand {

    public static final String NAME = "language";
    static final String ALIAS = "lang";

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
        output.throwIfUnmatchedArguments(spec.commandLine());
        output.info("Listing languages (default action, see --help)");
        return spec.commandLine().execute(NAME, LanguageFind.NAME);
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
