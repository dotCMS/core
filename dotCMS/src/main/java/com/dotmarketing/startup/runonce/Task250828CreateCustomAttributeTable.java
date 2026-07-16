package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * This task creates the "analytic_custom_attributes" table in the database.
 * <p>
 * The table stores custom attributes for different analytics event types with the following structure:
 * <ul>
 *     <li>event_type: Primary key that identifies the type of analytics event</li>
 *     <li>custom_attribute: JSON data containing the custom attributes for the event type</li>
 * </ul>
 * <p>
 * This table allows for flexible storage of custom attributes that can vary by event type
 * in the analytics system.
 * <p>
 * The task only runs if the table doesn't already exist in the database.
 */
public class Task250828CreateCustomAttributeTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
            return !databaseMetaData.tableExists(DbConnectionFactory.getConnection(), "analytic_custom_attributes");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "CREATE TABLE IF NOT EXISTS analytic_custom_attributes ("
                + "     event_type  varchar(255) primary key,\n"
                + "     custom_attribute jsonb not null\n"
                + ")";
    }
}
