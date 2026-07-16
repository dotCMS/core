package com.dotcms.shutdown.tasks;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(75)
public class HealthServiceShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "Health service shutdown";
    }

    @Override
    public void run() {
        try {
            Logger.info(this, "Starting health service shutdown");
            
            // Health checks should naturally stop during system shutdown
            // No specific API to shutdown health checks - they rely on system state
            Logger.info(this, "Health checks will stop naturally during system shutdown");
            
            Logger.info(this, "Health service shutdown completed");
        } catch (Exception e) {
            Logger.warn(this, "Health service shutdown failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getTimeoutSeconds() {
        return 5; // Health service shutdown should be quick
    }
}