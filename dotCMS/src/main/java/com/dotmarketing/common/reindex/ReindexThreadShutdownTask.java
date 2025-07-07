package com.dotmarketing.common.reindex;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(20)
public class ReindexThreadShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "Reindex thread";
    }

    @Override
    public void run() {
        try {
            Logger.info(this, "Stopping ReindexThread to prevent DB connection errors during shutdown");
            
            // First, set the thread state to STOPPED
            ReindexThread.stopThread();
            
            // Give the thread a brief moment to notice the state change
            Thread.sleep(500);
            
            // Now force shutdown the ReindexThread executor to interrupt any blocked database operations
            try {
                com.dotcms.concurrent.DotConcurrentFactory.getInstance()
                    .getSubmitter("ReindexThreadSubmitter")
                    .shutdown();
                Logger.debug(this, "ReindexThread executor shutdown initiated");
            } catch (Exception e) {
                Logger.debug(this, "Could not shutdown ReindexThread executor: " + e.getMessage());
            }
            
            // Give additional time for any in-flight operations to complete
            Thread.sleep(1500);
            
            Logger.info(this, "ReindexThread shutdown completed");
        } catch (Exception e) {
            Logger.warn(this, "Reindex thread shutdown failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getTimeoutSeconds() {
        return 8; // Allow time for graceful shutdown + executor shutdown
    }
}
