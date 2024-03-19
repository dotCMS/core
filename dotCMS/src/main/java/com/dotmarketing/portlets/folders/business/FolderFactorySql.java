package com.dotmarketing.portlets.folders.business;

/**
 * This class provides the SQL queries to be used by the {@link FolderFactoryImpl} class.
 *
 * @author Jose Castro
 * @since Mar 7th, 2024
 */
public final class FolderFactorySql {

    public static final String GET_CONTENT_REPORT = "SELECT asset_subtype, COUNT(DISTINCT(identifier)) AS total " +
            "FROM identifier " +
            "WHERE asset_type = ? AND parent_path LIKE ? AND host_inode = ? GROUP BY asset_subtype ORDER BY %s %s";

    public static final String GET_CONTENT_TYPE_COUNT = "SELECT COUNT(DISTINCT(asset_subtype)) " +
            "FROM identifier " +
            "WHERE asset_type = ? AND parent_path LIKE ? and host_inode = ?";

    private FolderFactorySql() {
        // Default private constructor
    }

}
