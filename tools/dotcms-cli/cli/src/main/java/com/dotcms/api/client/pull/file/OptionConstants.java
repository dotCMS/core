package com.dotcms.api.client.pull.file;

/**
 * The OptionConstants class provides constant values for various options used in pull commands.
 * These options are used to configure specific behaviors and settings. This class cannot be
 * instantiated and only contains static final fields.
 */
public class OptionConstants {

    public static final String INCLUDE_FOLDER_PATTERNS = "includeFolderPatterns";
    public static final String INCLUDE_ASSET_PATTERNS = "includeAssetPatterns";
    public static final String EXCLUDE_FOLDER_PATTERNS = "excludeFolderPatterns";
    public static final String EXCLUDE_ASSET_PATTERNS = "excludeAssetPatterns";
    public static final String NON_RECURSIVE = "non-recursive";
    public static final String PRESERVE = "preserve";
    public static final String INCLUDE_EMPTY_FOLDERS = "includeEmptyFolders";

    /**
     * Private constructor to prevent instantiation
     */
    private OptionConstants() {
        // This constructor will never be called
    }

}
