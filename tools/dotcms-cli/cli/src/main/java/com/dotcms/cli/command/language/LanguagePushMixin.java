package com.dotcms.cli.command.language;

import picocli.CommandLine;

public class LanguagePushMixin {

    @CommandLine.Option(names = {"-rl", "--removeLanguages"}, defaultValue = "false",
            description =
                    "When this option is enabled, the push process allows the deletion of languages in the remote server. "
                            + "By default, this option is disabled, and languages will not be removed on the remote server.")
    public boolean removeLanguages;

}
