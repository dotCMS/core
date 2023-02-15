package com.dotcms.cli.command;

import com.dotcms.cli.command.contenttype.ContentTypeFind;
import com.dotcms.cli.command.contenttype.ContentTypePull;
import com.dotcms.cli.command.contenttype.ContentTypePush;
import com.dotcms.cli.command.contenttype.ContentTypeRemove;
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
           ContentTypeFind.class,
           ContentTypePull.class,
           ContentTypePush.class,
           ContentTypeRemove.class,
          //--- Site related stuff
           SiteFind.class,
           SitePull.class,
           SitePush.class,
           SiteArchive.class,
           SiteStart.class,
           SiteStop.class,
           SiteCurrent.class,
           SiteCopy.class,
           SiteSwitch.class
        }
)
public class EntryCommand {


}
