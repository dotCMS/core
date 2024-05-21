package com.dotmarketing.quartz.job;

import com.dotcms.timemachine.business.TimeMachineAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.quartz.DotStatefulJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This job is responsible for removing old backup folders from the Time Machine folder path.
 *
 * Every time the {@link TimeMachineJob} runs, it creates a folder inside the Time Machine path (one for each language).
 * Inside this new folder, it stores all the files produced during the Time Machine process.
 * These files are necessary to render all the pages on the selected sites.
 *
 * This job is tasked with cleaning up the Time Machine Path folder by removing folders older than
 * the value specified in the PRUNE_TIMEMACHINE_OLDER_THAN_DAYS configuration property.
 *
 * The Time Machine Path is set using the TIMEMACHINE_PATH property.
 *
 * @see TimeMachineJob
 */
public class PruneTimeMachineBackupJob extends DotStatefulJob {

    final TimeMachineAPI timeMachineAPI = APILocator.getTimeMachineAPI();

    @Override
    public void run(JobExecutionContext jobContext) throws JobExecutionException {
        timeMachineAPI.removeOldTimeMachineBackupsFiles();
    }
}
