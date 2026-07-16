package com.dotcms.shutdown.tasks;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(85)
public class DataSourceShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "DataSource shutdown";
    }

    @Override
    public void run() {
        try {
            Logger.info(this, "Starting DataSource shutdown");
            DbConnectionFactory.shutdownDataSource();
            Logger.info(this, "DataSource shutdown completed");
        } catch (Exception e) {
            Logger.warn(this, "DataSource shutdown failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getTimeoutSeconds() {
        return 15; // Give HikariCP time to properly shutdown connection pool
    }
}