package com.dotcms.cli.common;

import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    private final String NAME = ConfigProvider.getConfig().getValue("dotcms.cli.name", String.class);
    private final String VERSION = ConfigProvider.getConfig().getValue("dotcms.cli.version", String.class);
    @Override
    public String[] getVersion() {

        return new String[]{
            "@|bold,magenta " + NAME + "|@ @|bold,cyan " + VERSION + "|@"
        };
    }
}
