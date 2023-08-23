package com.dotcms.cli.common;

import picocli.CommandLine;

public class InteractiveOptionMixin {

    @CommandLine.Option(names = { "-i", "--interactive" },
            paramLabel = "Interactive",
            description = {
                "When specified User confirmation is enforced.",
                "By default this is true.",
                "If set to false, the command will not ask for confirmation."
            }, defaultValue = "true")
    boolean interactive = true;

    public boolean isInteractive() {
        return interactive;
    }
}
