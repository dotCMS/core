package com.dotcms.util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.startup.runonce.Task211101AddContentletAsJsonColumn;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
public class ContentletAsJsonBackup implements AutoCloseable {

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
     * Writes the copy made by {@link #save()} back into the column, removes the backup table and any
     * {@code noDefault-<millis>} site that was created while the column was empty, and empties the
     * site and contentlet caches. Does nothing when {@link #save()} found no column to copy.
     * <p>
     * A default site lookup that finds no default site creates that unpublished site and saves it as
     * the default, so it would otherwise stay next to the real one and be served to the tests that run
     * next. The caches go for the same reason.
     * <p>
     * If a test failed between dropping the column and adding it back, the column is added again
     * first, with the type the upgrade task gives it.
     *
     * @throws DotDataException     if the JSON cannot be restored
     * @throws DotSecurityException if a leftover site cannot be removed
     * @throws SQLException         if the column cannot be looked up
     */
    public void restore() throws DotDataException, DotSecurityException, SQLException {
        if (!saved) {
            return;
        }
        enableAutoCommit();
        if (!new DotDatabaseMetaData().hasColumn("contentlet", "contentlet_as_json")) {
            Logger.warn(this, "contentlet_as_json was dropped and not re-added, adding it again");
            new Task211101AddContentletAsJsonColumn().executeUpgrade();
            enableAutoCommit();
        }
        new DotConnect().setSQL("update contentlet c set contentlet_as_json = b.contentlet_as_json"
                + " from " + backupTable + " b where b.inode = c.inode").loadResult();
        new DotConnect().setSQL("drop table " + backupTable).loadResult();
        DbConnectionFactory.closeSilently();
        removeFakeDefaultSites();
        CacheLocator.getHostCache().clearCache();
        CacheLocator.getContentletCache().clearCache();
        saved = false;
    }

    /**
     * Calls {@link #restore()}, so a try-with-resources block keeps the test's own failure and adds
     * a failed restore to it as a suppressed exception.
     *
     * @throws DotDataException     if the JSON cannot be restored
     * @throws DotSecurityException if a leftover site cannot be removed
     * @throws SQLException         if the column cannot be looked up
     */
    @Override
    public void close() throws DotDataException, DotSecurityException, SQLException {
        restore();
    }

    /**
     * Archives and deletes every site named {@code noDefault-<millis>}.
     *
     * @throws DotDataException     if the sites cannot be listed
     * @throws DotSecurityException if a site cannot be removed
     */
    private void removeFakeDefaultSites() throws DotDataException, DotSecurityException {
        final List<Map<String, Object>> fakeSites = new DotConnect().setSQL(
                "select distinct c.identifier from contentlet c join structure s on s.inode = c.structure_inode"
                        + " where s.name = 'Host' and (c.text1 like 'noDefault-%'"
                        + " or c.contentlet_as_json -> 'fields' -> 'hostName' ->> 'value' like 'noDefault-%')")
                .loadObjectResults();
        for (final Map<String, Object> row : fakeSites) {
            final Host fakeSite = APILocator.getHostAPI().find(
                    String.valueOf(row.get("identifier")), APILocator.systemUser(), false);
            if (null != fakeSite) {
                APILocator.getHostAPI().archive(fakeSite, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(fakeSite, APILocator.systemUser(), false);
            }
        }
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
