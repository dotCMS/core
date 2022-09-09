package com.dotmarketing.startup.runonce;


import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

/**
 * Upgrade Task to create the model to represent {@link com.dotcms.experiments.model.Experiment}s
 *
 * Does not attempt to create the table if already existing.
 */
public class Task220829CreateExperimentsTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
            return !databaseMetaData.tableExists(DbConnectionFactory.getConnection(), "experiment");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "CREATE TABLE IF NOT EXISTS experiment ("
                + "     id  varchar(255) primary key,\n"
                + "     page_id varchar(255) not null,\n"
                + "     name varchar(255) not null,\n"
                + "     description varchar(255) not null,\n"
                + "     status varchar(255) not null,\n"
                + "     traffic_proportion jsonb not null,\n"
                + "     traffic_allocation float4 not null,\n"
                + "     mod_date timestamptz not null,\n"
                + "     scheduling jsonb,\n"
                + "     creation_date timestamptz not null,\n"
                + "     created_by varchar(255) not null,\n"
                + "     last_modified_by varchar(255) not null,\n "
                + "     goals jsonb"
                + ")";
    }

    @Override
    public String getMSSQLScript() {
        return "create table experiment (\n"
                + "    id  NVARCHAR(255) primary key,\n"
                + "    page_id NVARCHAR(255) not null,\n"
                + "    name NVARCHAR(255) not null,\n"
                + "    description NVARCHAR(255) not null,\n"
                + "    status NVARCHAR(255) not null,\n"
                + "    traffic_proportion NVARCHAR(MAX) not null,\n"
                + "    traffic_allocation float not null,\n"
                + "    mod_date datetimeoffset(3) not null,\n"
                + "    scheduling NVARCHAR(MAX),\n"
                + "    creation_date datetimeoffset(3) not null,\n"
                + "    created_by NVARCHAR(255) not null,\n"
                + "    last_modified_by NVARCHAR(255) not null\n, "
                + "    goals NVARCHAR(MAX)"
                + "\n"
                + ");";
    }
}
