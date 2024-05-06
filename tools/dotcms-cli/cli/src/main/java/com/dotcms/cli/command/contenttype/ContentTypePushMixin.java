package com.dotcms.cli.command.contenttype;

import picocli.CommandLine;

public class ContentTypePushMixin {

    @CommandLine.Option(names = {"-rct", "--removeContentTypes"}, defaultValue = "false",
            description =
                    "When this option is enabled, the push process allows the deletion of content types in the remote server. "
                            + "By default, this option is disabled, and content types will not be removed on the remote server.")
    public boolean removeContentTypes;

}
