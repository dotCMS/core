package com.dotcms.cli.command;


import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@TopCommand
@Command(
        name = "dotCMS", mixinStandardHelpOptions = true, showAtFileInUsageHelp = true,
        version = {"dotCMS-cli 2.0", "picocli " + CommandLine.VERSION},
        description = {},
        header = "dotCMS cli tool",
        subcommands = {
           LoginCommand.class, StatusCommand.class, InstanceCommand.class, SiteCommand.class, ContentTypeCommand.class
        }
)
public class EntryCommand {


}
