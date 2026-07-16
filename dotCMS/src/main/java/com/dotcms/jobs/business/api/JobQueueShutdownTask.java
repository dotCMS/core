package com.dotcms.jobs.business.api;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(40)
public class JobQueueShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "Job queue shutdown";
    }

    @Override
    public void run() {
        try {
            Logger.info(this, "Shutting down job queue manager - preventing new job processing");
            APILocator.getJobQueueManagerAPI().close();
            Logger.info(this, "Job queue manager shutdown completed");
        } catch (Exception e) {
            Logger.warn(this, "Job queue shutdown failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getTimeoutSeconds() {
        return 10; // Allow time for job cleanup but not too long
    }
}
