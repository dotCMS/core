package com.dotcms.util;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;

/**
 * Keeps a copy of the {@code contentlet_as_json} column of every contentlet while a test drops and
 * re-adds that column, and puts it back afterwards.
 * <p>
 * Dropping the column and adding it again empties it for the whole database. Without a copy, every
 * test that runs later in the same JVM finds the starter content, including the default site, without
 * any fields. The next call to {@code HostAPI#findDefaultHost} then creates a second, unpublished
 * default site named {@code noDefault-<millis>}, and tests that resolve the current site, such as the
 * Shorty servlet tests, fail on their first attempt.
 */
public class ContentletAsJsonBackup {

    private final String backupTable;
    private boolean saved;

    /**
     * Creates a backup that uses the given table name, which must not clash with another test's.
     *
     * @param backupTable name of the temporary table that holds the copy
     */
    public ContentletAsJsonBackup(final String backupTable) {
        this.backupTable = backupTable;
    }

    /**
     * Copies the {@code contentlet_as_json} of every contentlet that has one into the backup table.
     * Does nothing when the column does not exist.
     *
     * @throws DotDataException if the copy cannot be written
     * @throws SQLException     if the column cannot be looked up
     */
    public void save() throws DotDataException, SQLException {
        enableAutoCommit();
        saved = new DotDatabaseMetaData().hasColumn("contentlet", "contentlet_as_json");
        if (!saved) {
            return;
        }
        new DotConnect().setSQL("drop table if exists " + backupTable).loadResult();
        new DotConnect().setSQL("create table " + backupTable + " as select inode, contentlet_as_json"
                + " from contentlet where contentlet_as_json is not null").loadResult();
        DbConnectionFactory.closeSilently();
    }

    /**
     * Writes the copy made by {@link #save()} back into the column and removes the backup table. Does
     * nothing when {@link #save()} found no column to copy.
     *
     * @throws DotDataException if the JSON cannot be restored
     */
    public void restore() throws DotDataException {
        if (!saved) {
            return;
        }
        enableAutoCommit();
        new DotConnect().setSQL("update contentlet set contentlet_as_json = (select b.contentlet_as_json"
                + " from " + backupTable + " b where b.inode = contentlet.inode)"
                + " where inode in (select inode from " + backupTable + ")").loadResult();
        new DotConnect().setSQL("drop table " + backupTable).loadResult();
        DbConnectionFactory.closeSilently();
        saved = false;
    }

    /**
     * Switches the current connection to auto-commit, which the schema changes made by these tests
     * need.
     *
     * @throws DotDataException if the connection cannot be switched
     */
    private void enableAutoCommit() throws DotDataException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }
}
