package com.dotmarketing.quartz;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(50)
public class QuartzSchedulersShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "Quartz schedulers";
    }

    @Override
    public void run() {
        try {
            Logger.debug(this, "Shutting down Quartz schedulers");
            QuartzUtils.stopSchedulers();
            Logger.debug(this, "Quartz schedulers shutdown completed");
        } catch (Exception e) {
            Logger.warn(this, "Quartz shutdown failed: " + e.getMessage());
        }
    }

    @Override
    public int getTimeoutSeconds() {
        return 20;
    }
}
