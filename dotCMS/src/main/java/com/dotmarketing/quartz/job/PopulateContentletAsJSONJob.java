package com.dotmarketing.quartz.job;

import com.dotcms.util.content.json.PopulateContentletAsJSONUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.quartz.*;

/**
 * Job created to populate in the Contentlet table missing contentlet_as_json columns.
 */
public class PopulateContentletAsJSONJob extends DotStatefulJob {

    private static final String EXCLUDING_ASSET_SUB_TYPE = "excludingAssetSubType";
    private static final String CONFIG_PROPERTY_HOURS_INTERVAL = "populateContentletAsJSONJob.hours.interval";
    private static final Lazy<Integer> HOURS_INTERVAL = Lazy.of(() -> Config.getIntProperty(
            CONFIG_PROPERTY_HOURS_INTERVAL, 4));

    @Override
    public void run(JobExecutionContext jobContext) throws JobExecutionException {

        final var jobDataMap = jobContext.getJobDetail().getJobDataMap();

        final String excludingAssetSubType;
        if (jobDataMap.containsKey(EXCLUDING_ASSET_SUB_TYPE)) {
            excludingAssetSubType = (String) jobDataMap.get(EXCLUDING_ASSET_SUB_TYPE);
        } else {
            excludingAssetSubType = null;
        }

        try {
            // Executing the populate contentlet as JSON logic
            new PopulateContentletAsJSONUtil().populateExcludingAssetSubType(excludingAssetSubType);

            // Removing the job if everything went well
            removeJob();
        } catch (SchedulerException e) {
            Logger.error(this, String.format("Unable to remove [%s] job",
                    PopulateContentletAsJSONJob.class.getName()), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Fires the job to populate the missing contentlet_as_json columns.
     */
    public static void fireJob(final String excludingAssetSubType) {

        final var jobName = getJobName();
        final var groupName = getJobGroupName();

        final var scheduler = QuartzUtils.getScheduler();

        // Checking if the job already exists
        var jobDetail = Try.of(() -> scheduler.getJobDetail(jobName, groupName))
                .onFailure(e -> {
                    Logger.error(PopulateContentletAsJSONJob.class,
                            String.format("Error retrieving job detail [%s, %s]", jobName, groupName), e);
                }).getOrNull();

        if (jobDetail != null) {
            Logger.info(PopulateContentletAsJSONJob.class,
                    String.format("Job [%s, %s] already exists, skipping creation", jobName, groupName));
            return;
        }

        // Creating the job
        final var jobDataMap = new JobDataMap();
        jobDataMap.put(EXCLUDING_ASSET_SUB_TYPE, excludingAssetSubType);

        jobDetail = new JobDetail(
                jobName, groupName, PopulateContentletAsJSONJob.class
        );

        jobDetail.setJobDataMap(jobDataMap);
        jobDetail.setDurability(false);
        jobDetail.setVolatility(false);
        jobDetail.setRequestsRecovery(true);

        // This trigger will fire the job every 4 hours
        final var trigger = TriggerUtils.makeHourlyTrigger(HOURS_INTERVAL.get());
        trigger.setName(jobName);
        trigger.setGroup(groupName);
        trigger.setJobName(jobName);
        trigger.setJobGroup(groupName);
        trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            Logger.error(PopulateContentletAsJSONJob.class, "Error scheduling populate content as JSON job", e);
            throw new DotRuntimeException("Error scheduling populate content as JSON job", e);
        }
    }

    /**
     * Removes the PopulateContentletAsJSONJob from the scheduler
     */
    @VisibleForTesting
    static void removeJob() throws SchedulerException {
        QuartzUtils.removeJob(getJobName(), getJobGroupName());
    }

    @VisibleForTesting
    static String getJobName() {
        return PopulateContentletAsJSONJob.class.getSimpleName();
    }

    @VisibleForTesting
    static String getJobGroupName() {
        return getJobName() + "_Group";
    }

}
