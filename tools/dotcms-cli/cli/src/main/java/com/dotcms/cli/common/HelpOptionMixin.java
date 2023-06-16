package com.dotcms.cli.common;

import picocli.CommandLine;

public class HelpOptionMixin {
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message.")
    public boolean help;
}
