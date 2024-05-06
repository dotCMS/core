package com.dotcms.cli.command.site;

import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = SiteCommand.NAME,
        aliases = { SiteCommand.ALIAS },
        header = "@|bold,blue Site operations.|@",
        description = {
                "Use the list of available sub-commands to manage sites.",
                "Use @|yellow --help|@ to see the available subcommands.",
                "For help on a specific subcommand do @|yellow site [SUBCOMMAND] --help|@ to see all available options and params."
        },
        subcommands = {
                SiteFind.class,
                SitePull.class,
                SitePush.class,
                SiteCreate.class,
                SiteRemove.class,
                SiteCopy.class,
                SiteStart.class,
                SiteStop.class,
                SiteArchive.class,
                SiteUnarchive.class
        }
)
public class SiteCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "site";
    static final String ALIAS = "host";

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
        //Upon not proving a sub command exec the default
        output.info("Listing sites (default action, see --help).");
        CommandLine.ParseResult result = spec.commandLine().getParseResult();
        CommandLine listCommand = spec.subcommands().get(SiteFind.NAME);
        return listCommand.execute(
                result.originalArgs().stream().filter(x -> !NAME.equals(x) && !ALIAS.equals(x))
                        .toArray(String[]::new));

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
