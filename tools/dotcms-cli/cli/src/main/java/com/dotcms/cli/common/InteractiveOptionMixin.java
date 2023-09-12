package com.dotcms.cli.common;

import picocli.CommandLine;

public class InteractiveOptionMixin {

    @CommandLine.Option(names = {"--non-interactive"},
            paramLabel = "Non interactive",
            description = {
                    "When specified, user confirmation is bypassed, allowing the command to run "
                            + "without asking for confirmation.",
                    "By default, this is false."

            }, defaultValue = "false")
    boolean nonInteractive;

    public boolean isInteractive() {
        return !nonInteractive;
    }

}
