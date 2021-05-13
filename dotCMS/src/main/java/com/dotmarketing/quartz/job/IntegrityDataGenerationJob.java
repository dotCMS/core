package com.dotmarketing.quartz.job;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.http.DotExecutionException;
import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.rest.IntegrityResource;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import java.util.Date;

/**
 * Quartz stateful job in charge of generating the integrity data for a provided endpoint id and request id.
 * It calls in place functionality at {link IntegrityUtil} to do the actual generation of the zip file.
 *
 * It also implements the {@link InterruptableJob} interface to support cancellation at any time.
 */
public class IntegrityDataGenerationJob extends DotStatefulJob {

    public static final String JOB_NAME = "IntegrityDataGenerationJob";
    public static final String JOB_GROUP = "dotcms_jobs";
    public static final String TRIGGER_NAME = "IntegrityDataGenerationTrigger";
    public static final String TRIGGER_GROUP = "integrity_data_generation_triggers";

    private JobExecutionContext jobContext;

    /**
     * Code to execute when Quartz calls the job.
     * Takes endpoint and request id from context to tries to generate check data zip file not without adding some
     * metadata to have control of what is going on.
     *
     * @param jobContext job context
     */
    @Override
    @CloseDBIfOpened
    public void run(final JobExecutionContext jobContext) {
        this.jobContext = jobContext;
        final JobDataMap jobDataMap = this.jobContext.getJobDetail().getJobDataMap();
        final String requesterKey = (String) jobDataMap.get(IntegrityUtil.REQUESTER_KEY);

        IntegrityUtil.cleanUpIntegrityData(requesterKey);

        final String requestId = (String) jobDataMap.get(IntegrityUtil.INTEGRITY_DATA_REQUEST_ID);
        IntegrityUtil.saveIntegrityDataStatus(
                requesterKey,
                requestId,
                IntegrityResource.ProcessStatus.PROCESSING);

        try {
            // Actual integrity data file generation
            IntegrityUtil.generateDataToCheckZip(requesterKey);
            // Integrity data generation went ok
            IntegrityUtil.saveIntegrityDataStatus(
                    requesterKey,
                    requestId,
                    IntegrityResource.ProcessStatus.FINISHED);
            Logger.info(
                    IntegrityDataGenerationJob.class,
                    String.format("Job execution for endpoint %s has finished", requesterKey));
        } catch (DotExecutionException e) {
            // Error has happened while generating integrity data
            Logger.error(IntegrityDataGenerationJob.class, "Error generating data to check", e);
            IntegrityUtil.saveIntegrityDataStatus(
                    requesterKey,
                    requestId,
                    IntegrityResource.ProcessStatus.ERROR,
                    String.format("Error generating data to check: %s", e.getMessage()));
        }
    }

    /**
     * Creates {@link JobDataMap} and {@link JobDetail} instances with integrity check data to trigger actual job.
     *
     * @param key JWT token key if you are using JWT token in Push Publish, otherwise end point id
     * @param integrityDataRequestId integrity data request id
     */
    public static void triggerIntegrityDataGeneration(final String key,
                                                      final String integrityDataRequestId) {
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(IntegrityUtil.REQUESTER_KEY, key);
        jobDataMap.put(IntegrityUtil.INTEGRITY_DATA_REQUEST_ID, integrityDataRequestId);

        final JobDetail jobDetail = new JobDetail(JOB_NAME, JOB_GROUP, IntegrityDataGenerationJob.class);
        jobDetail.setJobDataMap(jobDataMap);
        jobDetail.setDurability(false);
        jobDetail.setVolatility(false);
        jobDetail.setRequestsRecovery(true);

        final SimpleTrigger trigger = new SimpleTrigger(
                TRIGGER_NAME,
                TRIGGER_GROUP,
                new Date(System.currentTimeMillis()));
        HibernateUtil.addCommitListenerNoThrow(Sneaky.sneaked(() -> {
           getJobScheduler().scheduleJob(jobDetail, trigger);
        }));
    }

    /**
     * Encapsulates Scheduler to use.
     * @return a scheduler
     * @throws SchedulerException
     */
    public static Scheduler getJobScheduler() throws SchedulerException {
        return QuartzUtils.getScheduler();
    }

}
