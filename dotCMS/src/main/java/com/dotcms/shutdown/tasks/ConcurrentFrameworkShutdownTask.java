package com.dotcms.shutdown.tasks;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(70)
public class ConcurrentFrameworkShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "Concurrent framework";
    }

    @Override
    public void run() {
        try {
            DotConcurrentFactory.getInstance().shutdownAndDestroy();
        } catch (Exception e) {
            Logger.warn(this, "Concurrent framework shutdown failed: " + e.getMessage());
        }
    }
}
