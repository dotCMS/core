package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * Creates {@code login_lockout}, which holds login attempt counts and locks for
 * {@link com.dotcms.auth.LoginLockout}: one row per account and source (client address, or WebDAV from
 * that address), plus one account-wide row (source {@code *}). A separate table keeps the migration
 * additive: nothing on {@code user_} changes, and an older release rolled back onto this schema never
 * reads the new table.
 */
public class Task261015CreateLoginLockoutTable extends AbstractJDBCStartupTask {

    private static final String TABLE_NAME = "login_lockout";

    /**
     * Runs when the table is missing, or when it has the earlier per-account layout (no {@code source}
     * column) from a pre-release build. Lockout state is short-lived, so that table is simply rebuilt.
     */
    @Override
    public boolean forceRun() {
        try {
            final DotDatabaseMetaData metaData = new DotDatabaseMetaData();
            return !metaData.tableExists(DbConnectionFactory.getConnection(), TABLE_NAME)
                    || !metaData.hasColumn(TABLE_NAME, "source");
        } catch (final SQLException e) {
            Logger.error(this, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "DROP TABLE IF EXISTS login_lockout;" // nosemgrep: gitlab.find_sec_bugs.CUSTOM_INJECTION-2 -- fully hardcoded DDL, no user input
                + "CREATE TABLE login_lockout ("
                + " user_id varchar(100) not null,"
                + " source varchar(100) not null,"
                + " failures int not null,"
                + " window_start bigint not null,"
                + " locked_until bigint not null,"
                + " mod_date timestamptz not null,"
                + " constraint pk_login_lockout primary key (user_id, source)"
                + ");"
                + "CREATE INDEX idx_login_lockout_window ON login_lockout (window_start);";
    }
}
