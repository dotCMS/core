package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldDataBaseUtil;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Adds several Indexes to the {@code unique_fields} table for improving its performance. In
 * summary, the table will have the following indexes:
 * <ul>
 *     <li>The {@code BTREE} unique index on the {@code unique_key_val} column, which is already
 *     present through the Primary Key.</li>
 *     <li>The {@code GIN} index on the {@code supporting_values->'contentletIds'} attribute.</li>
 *     <li>The {@code BTREE} indexes for all other attributes in the {@code supporting_values}
 *     column that are used in {@code WHERE} clauses.</li>
 * </ul>
 *
 * @author Jose Castro
 * @since Aug 26th, 2025
 */
public class Task250826AddIndexesToUniqueFieldsTable implements StartupTask {

    @Override
    public boolean forceRun() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();
        try {
            return databaseMetaData.tableExists(connection, "unique_fields");
        } catch (final SQLException e) {
            Logger.error(this, String.format("An error occurred when checking if the 'unique_fields' table exists: " +
                    "%s", ExceptionUtil.getErrorMessage(e)), e);
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        new UniqueFieldDataBaseUtil().addTableIndexes();
    }

}
