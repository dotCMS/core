package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
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
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        boolean defaultAutoCommit = false;
        Connection connection = null;
        try {
            connection = DbConnectionFactory.getDataSource().getConnection();
            defaultAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            Logger.info(this, "(1/6) Adding GIN Index for the supporting_values->'contentletIds' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_contentlet_ids_gin ON unique_fields USING GIN ((supporting_values->'contentletIds'))")
                    .loadResult(connection);

            Logger.info(this, "(2/6) Adding Functional Index for the supporting_values->'languageId' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_language_id ON unique_fields (((supporting_values->>'languageId')::BIGINT))")
                    .loadResult(connection);

            Logger.info(this, "(3/6) Adding Functional Index for the supporting_values->'contentTypeId' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_content_type_id ON unique_fields (((supporting_values->>'contentTypeId')))")
                    .loadResult(connection);

            Logger.info(this, "(4/6) Adding Functional Index for the supporting_values->'fieldVariableName' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_field_variable_name ON unique_fields (((supporting_values->>'fieldVariableName')))")
                    .loadResult(connection);

            Logger.info(this, "(5/6) Adding Functional Index for the supporting_values->'variant' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_variant ON unique_fields (((supporting_values->>'variant')))")
                    .loadResult(connection);

            Logger.info(this, "(6/6) Adding Functional Index for the supporting_values->'live' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_live ON unique_fields (((supporting_values->>'live')::BOOLEAN))")
                    .loadResult(connection);
        } catch (final SQLException e) {
            throw new DotRuntimeException("", e);
        } finally {
            if (null != connection) {
                try {
                    connection.setAutoCommit(defaultAutoCommit);
                } catch (final SQLException e) {
                    Logger.error(this, "", e);
                }
            }
        }
    }

}
