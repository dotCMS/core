package com.dotcms.cli.common;

import picocli.CommandLine;

/**
 * This class is an extension of {@link PullMixin} providing additional command-line options
 * specifically for pull operations in dotCMS CLI.
 * <p>
 * This mixin structure allows for sharing common options across multiple pull commands, while also
 * offering command-specific options where necessary.
 */
public class FullPullOptionsMixin extends PullMixin {

    @CommandLine.Option(names = {"-fmt",
            "--format"}, description = {"Format for the pulled descriptor files. ",
            "Supported values: ${COMPLETION-CANDIDATES}"})
    InputOutputFormat inputOutputFormat = InputOutputFormat.defaultFormat();

    public InputOutputFormat inputOutputFormat() {
        return inputOutputFormat;
    }

}
