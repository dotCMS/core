package com.dotcms.cli.command;

import com.dotcms.cli.command.contenttype.*;
import com.dotcms.cli.command.language.LanguageCommand;
import com.dotcms.cli.command.site.*;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@TopCommand
@Command(
        name = "dotCMS",
        mixinStandardHelpOptions = true,
        version = {"dotCMS-cli 1.0", "picocli " + CommandLine.VERSION},
        description = {
                "@|bold,underline,blue dotCMS|@ cli is a command line interface to interact with your @|bold,underline,blue dotCMS|@ instance.",
        },
        header = "dotCMS cli",
        subcommands = {
          //-- Miscellaneous stuff
           StatusCommand.class,
           InstanceCommand.class,
           LoginCommand.class,

          //---- ContentType Related stuff
           ContentTypeCommand.class,
          //--- Site related stuff
           SiteCommand.class,
          //--- Language related stuff
           LanguageCommand.class
        }
)
public class EntryCommand {


}
