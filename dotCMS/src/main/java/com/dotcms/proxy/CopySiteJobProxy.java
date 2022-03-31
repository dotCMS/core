package com.dotcms.proxy;

import com.dotmarketing.quartz.job.HostCopyOptions;

/**
 * Proxy to interact with copy site ee implementation
 * @author jsanca
 */
public interface CopySiteJobProxy {

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
