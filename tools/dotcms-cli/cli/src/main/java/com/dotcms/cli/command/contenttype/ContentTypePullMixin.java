package com.dotcms.cli.command.contenttype;

import picocli.CommandLine.Parameters;

public class ContentTypePullMixin {

    @Parameters(paramLabel = "idOrName", index = "0", arity = "0..1", description = "Identifier or Name.")
    String idOrVar;

}
