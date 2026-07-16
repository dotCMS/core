package com.dotcms.enterprise.license;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(10)
public class LicenseCleanupShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "License cleanup";
    }

    @Override
    public void run() {
        try {
            LicenseUtil.freeLicenseOnRepo();
        } catch (Exception e) {
            Logger.warn(this, "License cleanup failed: " + e.getMessage());
        }
    }
}