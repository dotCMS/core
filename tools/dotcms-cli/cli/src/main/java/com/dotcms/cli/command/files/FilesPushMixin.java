package com.dotcms.cli.command.files;

import picocli.CommandLine;

public class FilesPushMixin {

    @CommandLine.Option(names = {"-rf", "--removeFolders"}, defaultValue = "false",
            description =
                    "When this option is enabled, the push process allows the deletion of folders in the remote server. "
                            + "By default, this option is disabled, and folders will not be removed on the remote server.")
    public boolean removeFolders;

    @CommandLine.Option(names = {"-ra", "--removeAssets"}, defaultValue = "false",
            description =
                    "When this option is enabled, the push process allows the deletion of assets in the remote server. "
                            + "By default, this option is disabled, and assets will not be removed on the remote server.")
    public boolean removeAssets;

}
