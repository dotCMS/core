package com.dotcms.cli.common;

import picocli.CommandLine;

/**
 * This class is an extension of {@link PushMixin} providing additional command-line options
 * specifically for push operations in dotCMS CLI.
 * <p>
 * This mixin structure allows for sharing common options across multiple push commands, while also
 * offering command-specific options where necessary.
 */
public class FullPushOptionsMixin extends PushMixin {

    @CommandLine.Option(names = {"-dau", "--disable-auto-update"}, defaultValue = "false",
            description =
                    "Disable the default behaviour of updating the local file descriptor with the "
                            + "response from the server after a push. When this option is used, the "
                            + "local file will remain in its initial state even after a successful push.")
    public boolean disableAutoUpdate;

}
