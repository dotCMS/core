package com.dotcms.storage;

/**
 * Defines the default storages in dotCMS, which defines both singe Storage Provider types and the
 * available storage chains. You can create your own implementation and use this Enum as key.
 *
 * @author jsanca
 */
public enum StorageType {

    FILE_SYSTEM, DB, S3, MEMORY,
    // Besides the default chain, Sys Admins can customize up to 3 different chains
    DEFAULT_CHAIN, CHAIN1, CHAIN2, CHAIN3

}
