package com.dotmarketing.quartz.job;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.quartz.DotStatefulJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Stateful job used to delete old SiteSearch indices.
 */
public class DeleteSiteSearchIndicesJob extends DotStatefulJob {

    @Override
    @WrapInTransaction
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {
        APILocator.getSiteSearchAPI().deleteOldSiteSearchIndices();
    }

}
