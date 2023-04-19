package com.dotcms.cli.common;

import picocli.CommandLine;

public class InteractiveOptionMixin {

    @CommandLine.Option(names = { "-i", "--interactive" }, description = {"When specified User confirmation is enforced."}, defaultValue = "true")
    boolean interactive = true;

    public boolean isInteractive() {
        return interactive;
    }
}
