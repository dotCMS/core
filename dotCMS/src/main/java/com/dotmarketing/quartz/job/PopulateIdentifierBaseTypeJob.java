package com.dotmarketing.quartz.job;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerUtils;

import java.util.Date;

import java.util.List;

/**
 * Quartz job that populates the {@code base_type} column on the {@code identifier} table in the
 * background. Scheduled by the startup task {@code Task260331AddBaseTypeColumnToIdentifier} so
 * that large customer databases are not blocked during startup.
 *
 * <p>The job removes itself from the scheduler once population completes — including when it
 * detects that no rows need updating (migration already finished).
 */
public class PopulateIdentifierBaseTypeJob extends DotStatefulJob {

    private static final String CONFIG_PROPERTY_HOURS_INTERVAL =
            "populateIdentifierBaseTypeJob.hours.interval";
    private static final Lazy<Integer> HOURS_INTERVAL = Lazy.of(
            () -> Config.getIntProperty(CONFIG_PROPERTY_HOURS_INTERVAL, 4));

    private static final String PENDING_ROWS_QUERY =
            "SELECT 1 FROM identifier " +
            "WHERE base_type IS NULL AND asset_subtype IS NOT NULL LIMIT 1";

    @Override
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {
        Logger.info(this, "PopulateIdentifierBaseTypeJob: starting");
        try {
            if (!hasPendingRows()) {
                Logger.info(this, "PopulateIdentifierBaseTypeJob: no rows with null base_type found — migration already complete, unscheduling");
                removeJob();
                return;
            }

            final int updated = new PopulateIdentifierBaseTypeUtil().populate();

            if (updated == 0) {
                Logger.info(this, "PopulateIdentifierBaseTypeJob: no rows updated — migration already complete, unscheduling");
            } else {
                Logger.info(this, "PopulateIdentifierBaseTypeJob: completed — " + updated + " rows updated, unscheduling");
            }
            removeJob();
        } catch (final SchedulerException e) {
            Logger.error(this, "PopulateIdentifierBaseTypeJob: unable to remove job after completion", e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Schedules the job. Skips scheduling if there are no rows with a null {@code base_type},
     * meaning the migration has already been completed. Safe to call multiple times — a no-op if
     * the job is already scheduled.
     */
    public static void fireJob() {
        if (!hasPendingRows()) {
            Logger.info(PopulateIdentifierBaseTypeJob.class,
                    "PopulateIdentifierBaseTypeJob: no rows with null base_type — migration already complete, skipping scheduling");
            return;
        }

        final String jobName = getJobName();
        final String groupName = getJobGroupName();
        final var scheduler = QuartzUtils.getScheduler();

        final var jobDetail = Try.of(() -> scheduler.getJobDetail(jobName, groupName))
                .onFailure(e -> Logger.error(PopulateIdentifierBaseTypeJob.class,
                        String.format("Error retrieving job detail [%s, %s]", jobName, groupName), e))
                .getOrNull();

        if (jobDetail != null) {
            Logger.info(PopulateIdentifierBaseTypeJob.class,
                    String.format("Job [%s, %s] already scheduled — skipping", jobName, groupName));
            return;
        }

        final var newJobDetail = new JobDetail(jobName, groupName, PopulateIdentifierBaseTypeJob.class);
        newJobDetail.setJobDataMap(new JobDataMap());
        newJobDetail.setDurability(false);
        newJobDetail.setVolatility(false);
        newJobDetail.setRequestsRecovery(true);

        final var trigger = TriggerUtils.makeHourlyTrigger(HOURS_INTERVAL.get());
        trigger.setName(jobName);
        trigger.setGroup(groupName);
        trigger.setJobName(jobName);
        trigger.setJobGroup(groupName);
        trigger.setStartTime(new Date()); // fire immediately on first schedule
        trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

        try {
            scheduler.scheduleJob(newJobDetail, trigger);
            Logger.info(PopulateIdentifierBaseTypeJob.class,
                    "PopulateIdentifierBaseTypeJob scheduled successfully");
        } catch (final Exception e) {
            throw new DotRuntimeException("Error scheduling PopulateIdentifierBaseTypeJob", e);
        }
    }

    /**
     * Returns {@code true} if any {@code identifier} rows still have a null {@code base_type}
     * with a non-null {@code asset_subtype} — i.e., there is migration work remaining.
     */
    static boolean hasPendingRows() {
        try {
            final List<?> result = new DotConnect()
                    .setSQL(PENDING_ROWS_QUERY)
                    .loadObjectResults();
            return !result.isEmpty();
        } catch (final DotDataException e) {
            Logger.error(PopulateIdentifierBaseTypeJob.class,
                    "Error checking for pending base_type rows: " + e.getMessage(), e);
            // Err on the side of running — better to do unnecessary work than skip needed work
            return true;
        }
    }

    @VisibleForTesting
    static void removeJob() throws SchedulerException {
        QuartzUtils.removeJob(getJobName(), getJobGroupName());
    }

    @VisibleForTesting
    static String getJobName() {
        return PopulateIdentifierBaseTypeJob.class.getSimpleName();
    }

    @VisibleForTesting
    static String getJobGroupName() {
        return getJobName() + "_Group";
    }

}
