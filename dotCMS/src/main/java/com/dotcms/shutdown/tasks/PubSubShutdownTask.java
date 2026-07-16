package com.dotcms.shutdown.tasks;

import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(70)
public class PubSubShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "PubSub shutdown";
    }

    @Override
    public void run() {
        try {
            Logger.info(this, "Starting PubSub shutdown - CRITICAL to prevent DB connection errors");
            
            DotPubSubProvider pubSubProvider = APILocator.getDotPubSubProvider();
            if (pubSubProvider != null) {
                Logger.info(this, "Stopping PubSub provider: " + pubSubProvider.getClass().getSimpleName());
                pubSubProvider.stop();
                
                // Give PubSub threads time to stop cleanly
                Thread.sleep(1000);
                
                Logger.info(this, "PubSub provider stopped successfully");
            } else {
                Logger.info(this, "No PubSub provider found to shutdown");
            }
            
            Logger.info(this, "PubSub shutdown completed - DB connection errors should be prevented");
        } catch (Exception e) {
            Logger.warn(this, "PubSub shutdown failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getTimeoutSeconds() {
        return 10; // PubSub shutdown should be quick
    }
}