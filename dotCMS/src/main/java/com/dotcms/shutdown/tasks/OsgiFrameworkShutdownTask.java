package com.dotcms.shutdown.tasks;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(90)
public class OsgiFrameworkShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "OSGi framework";
    }

    @Override
    public void run() {
        try {
            Logger.debug(this, "Shutting down OSGi framework");
            // Use OSGIUtil's stopFramework method which handles proper cleanup
            org.apache.felix.framework.OSGIUtil osgiUtil = org.apache.felix.framework.OSGIUtil.getInstance();
            if (osgiUtil.isInitialized()) {
                osgiUtil.stopFramework();
                Logger.debug(this, "OSGi framework shutdown completed");
            } else {
                Logger.debug(this, "OSGi framework not initialized, skipping shutdown");
            }
        } catch (Exception e) {
            Logger.warn(this, "OSGi framework shutdown failed: " + e.getMessage());
        }
    }

    @Override
    public int getTimeoutSeconds() {
        return 15;
    }
}
