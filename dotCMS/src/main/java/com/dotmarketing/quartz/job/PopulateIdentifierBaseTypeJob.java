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

    /**
     * Counts only rows that can actually be resolved by the populate query — i.e. rows with a
     * matching entry in the structure table. Orphaned identifiers whose asset_subtype no longer
     * exists in structure are intentionally excluded: the populate loop returns 0 for them and
     * they must not keep the job alive indefinitely.
     */
    private static final String PENDING_ROWS_QUERY =
            "SELECT 1 FROM identifier i " +
            "INNER JOIN structure s ON s.velocity_var_name = i.asset_subtype " +
            "WHERE i.base_type IS NULL LIMIT 1";

    @Override
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {
        Logger.info(this, "PopulateIdentifierBaseTypeJob: starting");
        try {
            if (!hasPendingRows()) {
                Logger.info(this, "PopulateIdentifierBaseTypeJob: no rows with null base_type — migration already complete, unscheduling");
                removeJob();
                return;
            }

            final int updated = new PopulateIdentifierBaseTypeUtil().populate();
            Logger.info(this, "PopulateIdentifierBaseTypeJob: populate() returned — " + updated + " rows updated");

            // Only unschedule when the migration is confirmed complete.
            // populate() may return early due to interruption, leaving pending rows;
            // in that case we keep the job scheduled so it resumes on the next trigger.
            if (!hasPendingRows()) {
                Logger.info(this, "PopulateIdentifierBaseTypeJob: migration complete, unscheduling");
                removeJob();
            } else {
                Logger.info(this, "PopulateIdentifierBaseTypeJob: pending rows remain (interrupted or partial run) — will resume on next trigger");
            }
        } catch (final SchedulerException e) {
            Logger.error(this, "PopulateIdentifierBaseTypeJob: unable to remove job", e);
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
     * and have a matching entry in the {@code structure} table — i.e., there is migration work
     * that the populate query can actually complete. Orphaned rows (asset_subtype with no
     * matching structure entry) are excluded so they cannot keep the job running indefinitely.
     */
    public static boolean hasPendingRows() {
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
