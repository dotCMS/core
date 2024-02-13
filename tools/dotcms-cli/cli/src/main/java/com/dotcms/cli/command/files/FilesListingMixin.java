package com.dotcms.cli.command.files;

import picocli.CommandLine;
import picocli.CommandLine.Parameters;

/**
 * Mixin class that provides options for listing the contents of a remote dotCMS directory
 */
public class FilesListingMixin {

    @Parameters(index = "0", arity = "1", paramLabel = "path",
            description = "dotCMS path to the directory to list the contents of "
                    + "- Format: //{site}/{folder}")
    String folderPath;

    @CommandLine.Option(names = {"-ee", "--excludeEmptyFolders"}, defaultValue = "false",
            description =
                    "When this option is enabled, the tree display will exclude folders that do "
                            + "not contain any assets, as well as folders that have no children with assets. "
                            + "This can be useful for users who want to focus on the folder structure that "
                            + "contains assets, making the output more concise and easier to navigate. By "
                            + "default, this option is disabled, and all folders, including empty ones, "
                            + "will be displayed in the tree.")
    boolean excludeEmptyFolders;

    @CommandLine.Option(names = {"-ef", "--excludeFolder"},
            paramLabel = "patterns",
            description = "Exclude directories matching the given glob patterns. Multiple "
                    + "patterns can be specified, separated by commas.")
    String excludeFolderPatternsOption;

    @CommandLine.Option(names = {"-ea", "--excludeAsset"},
            paramLabel = "patterns",
            description = "Exclude assets matching the given glob patterns. Multiple "
                    + "patterns can be specified, separated by commas.")
    String excludeAssetPatternsOption;

    @CommandLine.Option(names = {"-if", "--includeFolder"},
            paramLabel = "patterns",
            description = "Include directories matching the given glob patterns. Multiple "
                    + "patterns can be specified, separated by commas.")
    String includeFolderPatternsOption;

    @CommandLine.Option(names = {"-ia", "--includeAsset"},
            paramLabel = "patterns",
            description = "Include assets matching the given glob patterns. Multiple "
                    + "patterns can be specified, separated by commas.")
    String includeAssetPatternsOption;

}
