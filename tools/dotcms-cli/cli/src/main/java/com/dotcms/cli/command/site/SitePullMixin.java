package com.dotcms.cli.command.site;

import picocli.CommandLine;

public class SitePullMixin {

    @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "idOrName",
            description = "Site name or Id.")
    String siteNameOrId;

}
