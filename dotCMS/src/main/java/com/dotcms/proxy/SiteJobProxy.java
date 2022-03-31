package com.dotcms.proxy;

import com.dotmarketing.quartz.job.HostCopyOptions;

/**
 * Proxy to interact with site ee implementations
 * @author jsanca
 */
public interface SiteJobProxy {

    /**
     * Fires the job
     * @param destinationHostId
     * @param sourceHostId
     * @param hostCopyOptions
     * @param userId
     */
    void fireJob(final String destinationHostId,
            final String sourceHostId,
            final HostCopyOptions hostCopyOptions,
            final String userId);
}
