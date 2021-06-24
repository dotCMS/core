package com.dotmarketing.quartz.job;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Config;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Stateful job used to delete old ES indices.
 *
 * It Deletes the live/working inactive indices older than the inactive live/working sets indicated to be
 * kept by the config property MAX_INACTIVE_INDEX_SETS_TO_KEEP. If property value is not set it defaults to 2.
 *
 * The job is by default scheduled to be run once a day at 1am.
 */
public class DeleteInactiveLiveWorkingIndicesJob extends DotStatefulJob {

    @Override
    @WrapInTransaction
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {
        APILocator.getESIndexAPI().deleteInactiveLiveWorkingIndices(
                Config.getIntProperty("MAX_INACTIVE_INDEX_SETS_TO_KEEP", 2));
    }

}
