package com.dotcms.shutdown.tasks;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(60)
public class CacheSystemShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "Cache system";
    }

    @Override
    public void run() {
        try {
            CacheLocator.getCacheAdministrator().shutdown();
        } catch (Exception e) {
            Logger.warn(this, "Cache shutdown failed: " + e.getMessage());
        }
    }
}
