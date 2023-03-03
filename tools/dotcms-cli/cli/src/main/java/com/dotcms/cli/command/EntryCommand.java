package com.dotcms.cli.command;

import com.dotcms.cli.command.contenttype.*;
import com.dotcms.cli.command.site.*;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@TopCommand
@Command(
        name = "dotCMS", mixinStandardHelpOptions = true, showAtFileInUsageHelp = true,
        version = {"dotCMS-cli 1.0", "picocli " + CommandLine.VERSION},
        description = {},
        header = "dotCMS cli",
        subcommands = {
          //-- Miscellaneous stuff
           LoginCommand.class,
           StatusCommand.class,
           InstanceCommand.class,
          //---- ContentType Related stuff
           ContentTypeCommand.class,
          //--- Site related stuff
           SiteCommand.class,
        }
)
public class EntryCommand {


}
