package com.dotcms.cli.command.site;

import com.dotcms.cli.common.HelpOption;
import com.dotcms.cli.common.OutputOptionMixin;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = SiteCommand.NAME,
        aliases = { SiteCommand.ALIAS },
        header = "Site CRUD operations.",
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
                SiteUnarchive.class,
                SiteCurrent.class,
                SiteSwitch.class
        }
)
public class SiteCommand implements Callable<Integer> {

    static final String NAME = "site";
    static final String ALIAS = "host";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @CommandLine.Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {

        //Upon not proving a sub command exec the default
        output.info("Listing sites (default action, see --help).");
        CommandLine.ParseResult result = spec.commandLine().getParseResult();
        List<String> args = result.originalArgs().stream().filter(x -> !NAME.equals(x) && !ALIAS.equals(x)).collect(Collectors.toList());
        CommandLine listCommand = spec.subcommands().get(SiteFind.NAME);
        return listCommand.execute(args.toArray(new String[0]));

    }

}
