package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * Creates the operational table that stores vanity aliases published on S3.
 */
public class Task260507CreateS3VanityAliasTable extends AbstractJDBCStartupTask {

    private static final String TABLE_NAME = "static_s3_vanity_mapping";

    /**
     * Checks whether the table already exists in the database.
     *
     * @return true when the task must run
     */
    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().tableExists(DbConnectionFactory.getConnection(), TABLE_NAME);
        } catch (final SQLException e) {
            Logger.error(this, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Returns the PostgreSQL script that creates the mapping table.
     *
     * @return table DDL
     */
    @Override
    public String getPostgresScript() {
        return getScript();
    }

    /**
     * Returns the PostgreSQL DDL for the mapping table.
     *
     * @return table DDL
     */
    private String getScript() {
        return "CREATE TABLE IF NOT EXISTS static_s3_vanity_mapping (" // nosemgrep: gitlab.find_sec_bugs.CUSTOM_INJECTION-2 -- fully hardcoded DDL, no user input
                + " endpoint_id varchar(36) not null,"
                + " host_id varchar(36) not null,"
                + " language_id bigint not null,"
                + " canonical_path varchar not null,"
                + " canonical_path_hash varchar(64) not null,"
                + " vanity_path varchar not null,"
                + " vanity_path_hash varchar(64) not null,"
                + " vanity_url_id varchar(36),"
                + " bucket_name varchar not null,"
                + " bucket_region varchar,"
                + " bucket_prefix varchar,"
                + " mod_date timestamptz not null,"
                + " primary key (endpoint_id, host_id, language_id, canonical_path_hash, vanity_path_hash)"
                + ");"
                + "CREATE INDEX IF NOT EXISTS idx_static_s3_vanity_mapping_vurl "
                + "ON static_s3_vanity_mapping (endpoint_id, vanity_url_id)";
    }
}
