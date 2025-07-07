package com.dotcms.shutdown.tasks;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(30)
public class ServerClusterCleanupShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "Server cluster cleanup";
    }

    @Override
    public void run() {
        try {
            String serverId = APILocator.getServerAPI().readServerId();
            Logger.debug(this, "Removing server " + serverId + " from cluster tables");
            APILocator.getServerAPI().removeServerFromClusterTable(serverId);
            Logger.debug(this, "Server cluster cleanup completed for server " + serverId);
        } catch (Exception e) {
            // Check if this is a database connectivity issue (expected in some scenarios)
            String message = e.getMessage();
            if (message != null && (message.contains("connection") || message.contains("database") ||
                message.contains("SQLException") || message.contains("HikariPool"))) {
                Logger.info(this, "Server cluster cleanup skipped due to database connectivity issue (expected during some shutdowns)");
            } else {
                Logger.warn(this, "Server cluster cleanup failed: " + e.getMessage());
            }
        }
    }
}
