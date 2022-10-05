package com.dotmarketing.startup.runonce;


import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

/**
 * Upgrade Task to add the loopback_window column to {@link com.dotcms.experiments.model.Experiment}s table
 */
public class Task220928AddLookbackWindowColumnToExperiment extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().hasColumn("experiment", "lookback_window");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "ALTER TABLE experiment ADD lookback_window integer not null";
    }

    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE experiment ADD lookback_window numeric(19,0) not null";
    }
}
