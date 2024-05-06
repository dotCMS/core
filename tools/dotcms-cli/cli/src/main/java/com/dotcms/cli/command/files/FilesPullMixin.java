package com.dotcms.cli.command.files;

import picocli.CommandLine;

public class FilesPullMixin {

    @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "path",
            description = "dotCMS path to a specific site, directory or file to pull. "
                    + "%nFormat: //{site} - //{site}/{folder} - //{site}/{folder}/{file}."
                    + "%nIf no path is provided, all files across all sites will be pulled, "
                    + "which can be a resource-intensive operation.")
    String path;

    @CommandLine.Option(names = {"-nr", "--non-recursive"}, defaultValue = "false",
            description = "Pulls only the specified directory and the contents under it.")
    boolean nonRecursive;

    @CommandLine.Option(names = {"-p", "--preserve"}, defaultValue = "false",
            description = "Preserves existing files and directories, avoiding overwriting if they already exist.")
    boolean preserve;

    @CommandLine.Option(names = {"-ie", "--includeEmptyFolders"}, defaultValue = "false",
            description =
                    "When this option is enabled, the pull process will not create empty folders. "
                            + "By default, this option is disabled, and empty folders will not be created.")
    boolean includeEmptyFolders;

    @CommandLine.Mixin
    FilesGlobMixin globMixin;

}
