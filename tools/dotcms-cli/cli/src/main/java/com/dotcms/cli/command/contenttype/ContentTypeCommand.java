package com.dotcms.cli.command.contenttype;

import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = ContentTypeCommand.NAME,
        aliases = { ContentTypeCommand.ALIAS },
        header = "@|bold,blue Content type operations.|@",
        description = {
                "Use the list of available sub-commands to manage content-types.",
                "Use @|yellow --help|@ to see the available subcommands.",
                "For help on a specific subcommand do @|yellow content-type [SUBCOMMAND] --help|@ to see all available options and params."
        },
        subcommands = {
          ContentTypeFind.class,
          ContentTypePull.class,
          ContentTypePush.class,
          ContentTypeRemove.class
     }
 )
public class ContentTypeCommand implements Callable<Integer> {

    static final String NAME = "content-type";
    static final String ALIAS = "ct";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {

        // If no default action is desired do this:
        /*
        spec.commandLine().usage(output.out());
        return CommandLine.ExitCode.OK;
        */
        //Upon not proving a sub command exec the default
        output.info("Listing content-types (default action, see --help).");
        CommandLine.ParseResult result = spec.commandLine().getParseResult();
        CommandLine listCommand = spec.subcommands().get(ContentTypeFind.NAME);
        return listCommand.execute(
                result.originalArgs().stream().filter(x -> !NAME.equals(x) && !ALIAS.equals(x))
                        .toArray(String[]::new));
    }
}
