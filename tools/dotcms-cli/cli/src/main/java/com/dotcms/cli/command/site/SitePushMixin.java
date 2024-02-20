package com.dotcms.cli.command.site;

import picocli.CommandLine;

public class SitePushMixin {

    @CommandLine.Option(names = {"-rs", "--removeSites"}, defaultValue = "false",
            description =
                    "When this option is enabled, the push process allows the deletion of sites in the remote server. "
                            + "By default, this option is disabled, and sites will not be removed on the remote server.")
    public boolean removeSites;

    @CommandLine.Option(names = {"-fse", "--forceSiteExecution"}, defaultValue = "false",
            paramLabel = "force site execution",
            description = "Force must me set to true to update a site name.")
    public boolean forceExecution;

}
