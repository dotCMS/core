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

import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Job created to populate in the Contentlet table missing contentlet_as_json columns.
 */
public class PopulateContentletAsJSONJob extends DotStatefulJob {

    private static final String JOB_DATA_EXCLUDING_ASSET_SUB_TYPE = "excludingAssetSubType";
    private static final String JOB_DATA_ALL_CONTENTS_ALL_VERSIONS = "allContentsAllVersions";
    private static final String CONFIG_PROPERTY_HOURS_INTERVAL = "populateContentletAsJSONJob.hours.interval";
    private static final Lazy<Integer> HOURS_INTERVAL = Lazy.of(() -> Config.getIntProperty(
            CONFIG_PROPERTY_HOURS_INTERVAL, 4));

    /**
     * Executes the job logic, which first involves populating the working and live versions of contentlets as JSON.
     * After the first population process finishes, it registers another job to migrate all the remaining contentlets.
     *
     * @param jobContext The JobExecutionContext object containing the job execution context.
     * @throws JobExecutionException if there is an error executing the job.
     */
    @Override
    public void run(JobExecutionContext jobContext) throws JobExecutionException {

        final var jobDataMap = jobContext.getJobDetail().getJobDataMap();

        String excludingAssetSubType = null;
        Boolean forAllContentsAllVersions = null;

        if (jobDataMap.containsKey(JOB_DATA_ALL_CONTENTS_ALL_VERSIONS)) {
            forAllContentsAllVersions = (Boolean) jobDataMap.get(JOB_DATA_ALL_CONTENTS_ALL_VERSIONS);
        } else if (jobDataMap.containsKey(JOB_DATA_EXCLUDING_ASSET_SUB_TYPE)) {
            excludingAssetSubType = (String) jobDataMap.get(JOB_DATA_EXCLUDING_ASSET_SUB_TYPE);
        }

        try {
            // Executing the populate contentlet as JSON logic
            if (forAllContentsAllVersions != null && forAllContentsAllVersions) {
                new PopulateContentletAsJSONUtil().populateEverything();
            } else {
                new PopulateContentletAsJSONUtil().populateExcludingAssetSubType(excludingAssetSubType);
            }

            // Removing the job if everything went well
            removeJob();

            // If the populate for working and live versions is done we fire the job to populate the missing contentlets
            if (forAllContentsAllVersions == null) {
                fireJobAllContentsAllVersions();
            }

        } catch (SchedulerException e) {
            Logger.error(this, String.format("Unable to remove [%s] job",
                    PopulateContentletAsJSONJob.class.getName()), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Fires the job to populate the missing contentlet_as_json columns for all contents and all versions.
     */
    private static void fireJobAllContentsAllVersions() {

        final var jobDataMap = new JobDataMap();
        jobDataMap.put(JOB_DATA_ALL_CONTENTS_ALL_VERSIONS, true);

        fireJob(jobDataMap);
    }

    /**
     * Fires the job to populate the missing contentlet_as_json columns excluding a specific asset sub-type.
     *
     * @param excludingAssetSubType The asset sub-type to exclude.
     */
    public static void fireJob(final String excludingAssetSubType) {

        final var jobDataMap = new JobDataMap();
        jobDataMap.put(JOB_DATA_EXCLUDING_ASSET_SUB_TYPE, excludingAssetSubType);

        fireJob(jobDataMap);
    }

    /**
     * Fires the job to populate the missing contentlet_as_json columns.
     *
     * @param jobDataMap The JobDataMap containing the job data.
     */
    private static void fireJob(final JobDataMap jobDataMap) {

        final var jobName = getJobName();
        final var groupName = getJobGroupName();

        final var scheduler = QuartzUtils.getScheduler();

        // Checking if the job already exists
        var jobDetail = Try.of(() -> scheduler.getJobDetail(new JobKey(jobName, groupName)))
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
        jobDetail = newJob(PopulateContentletAsJSONJob.class)
                .withIdentity(jobName, groupName)
                .setJobData(jobDataMap)
                .storeDurably(false)
                .requestRecovery(true)
                .build();

        // This trigger will fire the job every 4 hours
        final var trigger = newTrigger()
                .withIdentity(jobName, groupName)
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInHours(HOURS_INTERVAL.get())
                        .repeatForever())
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            Logger.error(PopulateContentletAsJSONJob.class, "Error scheduling populate content as JSON job", e);
            throw new DotRuntimeException("Error scheduling populate content as JSON job", e);
        }
    }

    /**
     * Removes the PopulateContentletAsJSONJob from the scheduler
     *
     * @throws SchedulerException if there is an error removing the job.
     */
    @VisibleForTesting
    static void removeJob() throws SchedulerException {
        QuartzUtils.removeJob(getJobName(), getJobGroupName());
    }

    /**
     * Gets the job name.
     *
     * @return The job name.
     */
    @VisibleForTesting
    static String getJobName() {
        return PopulateContentletAsJSONJob.class.getSimpleName();
    }

    /**
     * Gets the job group name.
     *
     * @return The job group name.
     */
    @VisibleForTesting
    static String getJobGroupName() {
        return getJobName() + "_Group";
    }

}