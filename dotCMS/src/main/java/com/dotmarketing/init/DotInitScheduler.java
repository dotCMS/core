package com.dotmarketing.init;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.job.system.event.DeleteOldSystemEventsJob;
import com.dotcms.job.system.event.SystemEventsJob;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.workflow.EscalationThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.*;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import static com.dotmarketing.util.WebKeys.DOTCMS_DISABLE_ELASTIC_READONLY_MONITOR;
import static com.dotmarketing.util.WebKeys.DOTCMS_DISABLE_WEBSOCKET_PROTOCOL;

public class DotInitScheduler {

    public static final String DOTCMS_JOB_GROUP_NAME = "dotcms_jobs";
    public static final String CRON_EXPRESSION_EVERY_5_MINUTES = "0 */5 * ? * *";

    private static void deleteOldJobs() throws DotDataException {
        // remove unused old jobs
        QuartzUtils.deleteJobDB("WebDavCleanupJob", DOTCMS_JOB_GROUP_NAME);
        QuartzUtils.deleteJobDB("TrashCleanupJob", DOTCMS_JOB_GROUP_NAME);
        QuartzUtils.deleteJobDB("DeleteOldClickstreams", DOTCMS_JOB_GROUP_NAME);
        QuartzUtils.deleteJobDB("linkchecker", DOTCMS_JOB_GROUP_NAME);
        QuartzUtils.deleteJobDB("ContentReindexerJob", DOTCMS_JOB_GROUP_NAME);
    }

	/**
	 * Configures and initializes every system Job to run on dotCMS.
	 *
	 * @throws SchedulerException
	 *             An error occurred when trying to schedule one of our system
	 *             jobs.
	 */
    public static void start() throws Exception {
        try {
            final Scheduler sched = QuartzUtils.getScheduler();
            JobDetail job;
            Trigger trigger;
            Calendar calendar;
            boolean isNew;



            // remove unused old jobs
            deleteOldJobs();

            if (Config.getBooleanProperty("ENABLE_USERS_TO_DELETE_THREAD", false)) {
                try {
                    isNew = false;
                    JobKey jobKey = new JobKey("UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME);
                    try {
                        job = sched.getJobDetail(jobKey);
                        if (job == null) {
                            job =
                                    JobBuilder.newJob(UsersToDeleteThread.class)
                                            .withIdentity(jobKey)
                                            .build();
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob(jobKey);
                        job =
                                JobBuilder.newJob(UsersToDeleteThread.class)
                                        .withIdentity(jobKey)
                                        .build();
                        isNew = true;
                    }
                    calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, 6);
                    trigger =
                            TriggerBuilder.newTrigger()
                                    .withIdentity("trigger7", "group7")
                                    .forJob(jobKey)
                                    .withSchedule(
                                            CronScheduleBuilder.cronSchedule(
                                                            Config.getStringProperty(
                                                                    "USERS_TO_DELETE_THREAD_CRON_EXPRESSION"))
                                                    .withMisfireHandlingInstructionDoNothing())
                                    .startAt(calendar.getTime())
                                    .build();
                    sched.addJob(job, true);

                    if (isNew) sched.scheduleJob(trigger);
                    else sched.rescheduleJob(trigger.getKey(), trigger);
                } catch (Exception e) {
                    Logger.info(DotInitScheduler.class, e.toString());
                }
            } else {
                Logger.info(
                        DotInitScheduler.class,
                        "Users To Delete Cron Thread schedule disabled on this server");
                Logger.info(DotInitScheduler.class, "Deleting UsersToDeleteJob Job");
                JobKey jobKey = new JobKey("UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME);
                if (sched.getJobDetail(jobKey) != null) {
                    sched.deleteJob(jobKey);
                }
            }

            if (UtilMethods.isSet(
                    Config.getStringProperty("BINARY_CLEANUP_JOB_CRON_EXPRESSION", null))) {
                try {
                    isNew = false;
                    JobKey jobKey = new JobKey("BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME);
                    try {
                        job = sched.getJobDetail(jobKey);
                        if (job == null) {
                            job =
                                    JobBuilder.newJob(BinaryCleanupJob.class)
                                            .withIdentity(jobKey)
                                            .build();
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob(jobKey);
                        job =
                                JobBuilder.newJob(BinaryCleanupJob.class)
                                        .withIdentity(jobKey)
                                        .build();
                        isNew = true;
                    }
                    calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, 7);
                    trigger =
                            TriggerBuilder.newTrigger()
                                    .withIdentity("trigger11", "group11")
                                    .forJob(jobKey)
                                    .withSchedule(
                                            CronScheduleBuilder.cronSchedule(
                                                            Config.getStringProperty(
                                                                    "BINARY_CLEANUP_JOB_CRON_EXPRESSION"))
                                                    .withMisfireHandlingInstructionDoNothing())
                                    .startAt(calendar.getTime())
                                    .build();
                    sched.addJob(job, true);

                    if (isNew) sched.scheduleJob(trigger);
                    else sched.rescheduleJob(trigger.getKey(), trigger);
                } catch (Exception e) {
                    Logger.error(DotInitScheduler.class, e.getMessage(), e);
                }
            } else {
                Logger.info(
                        DotInitScheduler.class,
                        "BinaryCleanupJob Cron Job schedule disabled on this server");
                Logger.info(DotInitScheduler.class, "Deleting BinaryCleanupJob Job");
                JobKey jobKey = new JobKey("BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME);
                if (sched.getJobDetail(jobKey) != null) {
                    sched.deleteJob(jobKey);
                }
            }

            final String publishQueueJobName = "PublishQueueJob";
            // SCHEDULE PUBLISH QUEUE JOB
            if (Config.getBooleanProperty("ENABLE_PUBLISHER_QUEUE_THREAD", true)) {
                try {
                    isNew = false;
                    JobKey jobKey = new JobKey(publishQueueJobName, DOTCMS_JOB_GROUP_NAME);
                    try {
                        job = sched.getJobDetail(jobKey);
                        if (job == null) {
                            job =
                                    JobBuilder.newJob(PublisherQueueJob.class)
                                            .withIdentity(jobKey)
                                            .build();
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob(jobKey);
                        job =
                                JobBuilder.newJob(PublisherQueueJob.class)
                                        .withIdentity(jobKey)
                                        .build();
                        isNew = true;
                    }
                    calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, 3);
                    trigger =
                            TriggerBuilder.newTrigger()
                                    .withIdentity("trigger19", "group19")
                                    .forJob(jobKey)
                                    .withSchedule(
                                            CronScheduleBuilder.cronSchedule(
                                                            Config.getStringProperty(
                                                                    "PUBLISHER_QUEUE_THREAD_CRON_EXPRESSION",
                                                                    "0 0/1 * * * ?"))
                                                    .withMisfireHandlingInstructionDoNothing())
                                    .startAt(calendar.getTime())
                                    .build();
                    sched.addJob(job, true);

                    if (isNew) sched.scheduleJob(trigger);
                    else sched.rescheduleJob(trigger.getKey(), trigger);
                } catch (Exception e) {
                    Logger.error(DotInitScheduler.class, e.getMessage(), e);
                }
            } else {
                Logger.info(
                        DotInitScheduler.class,
                        "PublishQueueJob Cron Job schedule disabled on this server");
                Logger.info(DotInitScheduler.class, "Deleting PublishQueueJob Job");
                JobKey jobKey = new JobKey(publishQueueJobName, DOTCMS_JOB_GROUP_NAME);
                if (sched.getJobDetail(jobKey) != null) {
                    sched.deleteJob(jobKey);
                }
            }

            // SCHEDULE ESCALATION THREAD JOB
            String ETjobName = "EscalationThreadJob";
            String ETjobGroup = DOTCMS_JOB_GROUP_NAME;
            String ETtriggerName = "trigger24";
            String ETtriggerGroup = "group24";

            if (Config.getBooleanProperty("ESCALATION_ENABLE", true)) {
                try {
                    isNew = false;
                    JobKey jobKey = new JobKey(ETjobName, ETjobGroup);
                    try {
                        job = sched.getJobDetail(jobKey);
                        if (job == null) {
                            job =
                                    JobBuilder.newJob(EscalationThread.class)
                                            .withIdentity(jobKey)
                                            .build();
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob(jobKey);
                        job =
                                JobBuilder.newJob(EscalationThread.class)
                                        .withIdentity(jobKey)
                                        .build();
                        isNew = true;
                    }
                    calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, 10);
                    trigger =
                            TriggerBuilder.newTrigger()
                                    .withIdentity(ETtriggerName, ETtriggerGroup)
                                    .forJob(jobKey)
                                    .withSchedule(
                                            CronScheduleBuilder.cronSchedule(
                                                            Config.getStringProperty(
                                                                    "ESCALATION_CHECK_INTERVAL_CRON",
                                                                    "0/30 * * * * ?"))
                                                    .withMisfireHandlingInstructionDoNothing())
                                    .startAt(calendar.getTime())
                                    .build();
                    sched.addJob(job, true);

                    if (isNew) sched.scheduleJob(trigger);
                    else sched.rescheduleJob(trigger.getKey(), trigger);
                } catch (Exception e) {
                    Logger.error(DotInitScheduler.class, e.getMessage(), e);
                }
            } else {
                JobKey jobKey = new JobKey(ETjobName, ETjobGroup);
                if (sched.getJobDetail(jobKey) != null) {
                    sched.deleteJob(jobKey);
                }
            } // Schedule FreeServerFromClusterJob.
            String FSCjobName = "FreeServerFromClusterJob";
            String FSCjobGroup = DOTCMS_JOB_GROUP_NAME;
            String FSCtriggerName = "trigger25";
            String FSCtriggerGroup = "group25";

            if (Config.getBooleanProperty("ENABLE_SERVER_HEARTBEAT", true)) {
                try {
                    isNew = false;
                    JobKey jobKey = new JobKey(FSCjobName, FSCjobGroup);
                    try {
                        job = sched.getJobDetail(jobKey);
                        if (job == null) {
                            job =
                                    JobBuilder.newJob(FreeServerFromClusterJob.class)
                                            .withIdentity(jobKey)
                                            .build();
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob(jobKey);
                        job =
                                JobBuilder.newJob(FreeServerFromClusterJob.class)
                                        .withIdentity(jobKey)
                                        .build();
                        isNew = true;
                    }
                    calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, 2);
                    trigger =
                            TriggerBuilder.newTrigger()
                                    .withIdentity(FSCtriggerName, FSCtriggerGroup)
                                    .forJob(jobKey)
                                    .withSchedule(
                                            CronScheduleBuilder.cronSchedule(
                                                            Config.getStringProperty(
                                                                    "HEARTBEAT_CRON_EXPRESSION",
                                                                    "0 0/1 * * * ?"))
                                                    .withMisfireHandlingInstructionDoNothing())
                                    .startAt(calendar.getTime())
                                    .build();
                    sched.addJob(job, true);

                    if (isNew) sched.scheduleJob(trigger);
                    else sched.rescheduleJob(trigger.getKey(), trigger);
                } catch (Exception e) {
                    Logger.error(DotInitScheduler.class, e.getMessage(), e);
                }
            } else {
                JobKey jobKey = new JobKey(FSCjobName, FSCjobGroup);
                if (sched.getJobDetail(jobKey) != null) {
                    sched.deleteJob(jobKey);
                }
            }

            //Schedule CleanUnDeletedUsersJob
            String CUUjobName = "CleanUnDeletedUsersJob";
            String CUUjobGroup = DOTCMS_JOB_GROUP_NAME;
            String CUUtriggerName = "trigger26";
            String CUUtriggerGroup = "group26";

            if (Config.getBooleanProperty("ENABLE_CLEAN_UNDELETED_USERS", true)) {
                try {
                    isNew = false;
                    JobKey jobKey = new JobKey(CUUjobName, CUUjobGroup);
                    try {
                        job = sched.getJobDetail(jobKey);
                        if (job == null) {
                            job =
                                    JobBuilder.newJob(CleanUnDeletedUsersJob.class)
                                            .withIdentity(jobKey)
                                            .build();
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob(jobKey);
                        job =
                                JobBuilder.newJob(CleanUnDeletedUsersJob.class)
                                        .withIdentity(jobKey)
                                        .build();
                        isNew = true;
                    }
                    calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, 30);
                    trigger =
                            TriggerBuilder.newTrigger()
                                    .withIdentity(CUUtriggerName, CUUtriggerGroup)
                                    .forJob(jobKey)
                                    .withSchedule(
                                            CronScheduleBuilder.cronSchedule(
                                                            Config.getStringProperty(
                                                                    "CLEAN_USERS_CRON_EXPRESSION",
                                                                    "0 0 0 1/1 * ? *"))
                                                    .withMisfireHandlingInstructionFireAndProceed())
                                    .startAt(calendar.getTime())
                                    .build();
                    sched.addJob(job, true);

                    if (isNew) sched.scheduleJob(trigger);
                    else sched.rescheduleJob(trigger.getKey(), trigger);
                } catch (Exception e) {
                    Logger.error(DotInitScheduler.class, e.getMessage(), e);
                }
            } else {
                JobKey jobKey = new JobKey(CUUjobName, CUUjobGroup);
                if (sched.getJobDetail(jobKey) != null) {
                    sched.deleteJob(jobKey);
                }
            }

            addDropOldContentVersionsJob();
            if (!Config.getBooleanProperty(DOTCMS_DISABLE_WEBSOCKET_PROTOCOL, false)) {
                addSystemEventsJob();
            }

			// start the server heartbeat job
            addServerHeartbeatJob();

			// Enabling the Delete Old System Events Job
            addDeleteOldSystemEvents(sched);

            if (!Config.getBooleanProperty(DOTCMS_DISABLE_ELASTIC_READONLY_MONITOR, false)) {
                addElasticReadOnlyMonitor(sched);
            }

			//Enable the delete old ES Indices Job
            addDeleteOldESIndicesJob(sched);

			//Enable the delete old SS Indices Job
            addDeleteOldSiteSearchIndicesJob(sched);

            AccessTokenRenewJob.AccessTokensRenewJobScheduler.schedule();

            addStartEndScheduledExperimentsJob(sched);

            addPruneOldTimeMachineBackups(sched);

            //Starting the sequential and standard Schedulers
            QuartzUtils.startSchedulers();
        } catch (final SchedulerException e) {
			Logger.fatal(DotInitScheduler.class, "An error occurred when scheduling critical " +
					"startup task in dotCMS. The system will shutdown immediately: " + ExceptionUtil.getErrorMessage(e), e);
            throw e;
        }
    }

	/**
	 * Adds the {@link DropOldContentVersionsJob} Quartz Job to the scheduler during the startup
	 * process.
	 */
    private static void addDropOldContentVersionsJob() {
        final String triggerName = "trigger33";
        final String triggerGroup = "group33";
        final DotJobBuilder dropOldContentVersionsJob =
                new DotJobBuilder()
                        .setJobClass(DropOldContentVersionsJob.class)
                        .setJobName(DropOldContentVersionsJob.JOB_NAME)
                        .setJobGroup(DOTCMS_JOB_GROUP_NAME)
                        .setTriggerName(triggerName)
                        .setTriggerGroup(triggerGroup)
                        .setCronExpressionProp(DropOldContentVersionsJob.CRON_EXPR_PROP)
                        .setCronExpressionPropDefault(
                                DropOldContentVersionsJob.CRON_EXPRESSION.get())
                        .setCronMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        if (Boolean.FALSE.equals(DropOldContentVersionsJob.ENABLED.get())) {
            dropOldContentVersionsJob.enabled(false);
        }
        scheduleJob(dropOldContentVersionsJob);
    }

    private static void addDeleteOldSystemEvents(final Scheduler sched) throws SchedulerException {
        String DOSEjobName = "DeleteOldSystemEventsJob";
        String DOSEtriggerName = "trigger28";
        String DOSEtriggerGroup = "group28";

        if (Config.getBooleanProperty("ENABLE_DELETE_OLD_SYSTEM_EVENTS", true)) {
            final DotJobBuilder deleteOldSystemEventsJob =
                    new DotJobBuilder()
                            .setJobClass(DeleteOldSystemEventsJob.class)
                            .setJobName(DOSEjobName)
                            .setJobGroup(DOTCMS_JOB_GROUP_NAME)
                            .setTriggerName(DOSEtriggerName)
                            .setTriggerGroup(DOSEtriggerGroup)
                            .setCronExpressionProp("DELETE_OLD_SYSTEM_EVENTS_CRON_EXPRESSION")
                            .setCronExpressionPropDefault("0 0 0 1/3 * ? *")
                            .setCronMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
            scheduleJob(deleteOldSystemEventsJob);
        } else {
            JobKey jobKey = new JobKey(DOSEjobName, DOTCMS_JOB_GROUP_NAME);
            if (sched.getJobDetail(jobKey) != null) {
                sched.deleteJob(jobKey);
            }
        }

	} // addDeleteOldSystemEvents.

    private static void addSystemEventsJob() {
        if (Config.getBooleanProperty("ENABLE_SYSTEM_EVENTS", true)) {
            try {

                final int initialDelay = Config.getIntProperty("SYSTEM_EVENTS_INITIAL_DELAY", 0);
                final int delaySeconds =
                        Config.getIntProperty(
                                "SYSTEM_EVENTS_DELAY_SECONDS", 5); // runs every 5 seconds.
                DotConcurrentFactory.getScheduledThreadPoolExecutor()
                        .scheduleWithFixedDelay(
                                new SystemEventsJob(),
                                initialDelay,
                                delaySeconds,
                                TimeUnit.SECONDS);
            } catch (Exception e) {

                Logger.info(DotInitScheduler.class, e.toString());
            }
        }
	} // addSystemEventsJob.

    private static void addElasticReadOnlyMonitor(final Scheduler scheduler) {
        try {
            final String jobName = "EsReadOnlyMonitorJob";
            final String triggerName = "trigger29";
            final String triggerGroup = "group98";

            if (Config.getBooleanProperty("ENABLE_ELASTIC_READ_ONLY_MONITOR", true)) {
                final DotJobBuilder elasticReadOnlyMonitorJob =
                        new DotJobBuilder()
                                .setJobClass(EsReadOnlyMonitorJob.class)
                                .setJobName(jobName)
                                .setJobGroup(DOTCMS_JOB_GROUP_NAME)
                                .setTriggerName(triggerName)
                                .setTriggerGroup(triggerGroup)
                                .setCronExpressionProp("ELASTIC_READ_ONLY_MONITOR_CRON_EXPRESSION")
                                .setCronExpressionPropDefault(
                                        Config.getStringProperty(
                                                "ELASTIC_READ_ONLY_MONITOR_CRON_EXPRESSION",
                                                CRON_EXPRESSION_EVERY_5_MINUTES))
                                .setCronMisfireInstruction(
                                        CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                scheduleJob(elasticReadOnlyMonitorJob);
            } else {
                JobKey jobKey = new JobKey(jobName, DOTCMS_JOB_GROUP_NAME);
                if (scheduler.getJobDetail(jobKey) != null) {
                    scheduler.deleteJob(jobKey);
                }
            }
        } catch (Exception e) {
            Logger.info(DotInitScheduler.class, e.toString());
        }
    }

    private static void addDeleteOldESIndicesJob(final Scheduler scheduler) {
        try {
            final String jobName = "DeleteOldESIndicesJob";
            final String triggerName = "trigger30";
            final String triggerGroup = "group30";

            if (Config.getBooleanProperty("ENABLE_DELETE_OLD_ES_INDICES_JOB", true)) {
                final DotJobBuilder deleteOldESIndicesJob =
                        new DotJobBuilder()
                                .setJobClass(DeleteInactiveLiveWorkingIndicesJob.class)
                                .setJobName(jobName)
                                .setJobGroup(DOTCMS_JOB_GROUP_NAME)
                                .setTriggerName(triggerName)
                                .setTriggerGroup(triggerGroup)
                                .setCronExpressionProp("DELETE_OLD_ES_INDICES_JOB_CRON_EXPRESSION")
                                .setCronExpressionPropDefault("0 0 1 ? * *")
                                .setCronMisfireInstruction(
                                        CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                scheduleJob(deleteOldESIndicesJob);
            } else {
                JobKey jobKey = new JobKey(jobName, DOTCMS_JOB_GROUP_NAME);
                if (scheduler.getJobDetail(jobKey) != null) {
                    scheduler.deleteJob(jobKey);
                }
            }
        } catch (Exception e) {
            Logger.info(DotInitScheduler.class, e.toString());
        }
    }

    private static void addDeleteOldSiteSearchIndicesJob(final Scheduler scheduler) {
        try {
            final String jobName = "RemoveOldSiteSearchIndicesJob";
            final String triggerName = "trigger34";
            final String triggerGroup = "group34";

            if (Config.getBooleanProperty("ENABLE_DELETE_OLD_SS_INDICES_JOB", true)) {
                final DotJobBuilder deleteOldSSIndicesJob =
                        new DotJobBuilder()
                                .setJobClass(DeleteSiteSearchIndicesJob.class)
                                .setJobName(jobName)
                                .setJobGroup(DOTCMS_JOB_GROUP_NAME)
                                .setTriggerName(triggerName)
                                .setTriggerGroup(triggerGroup)
                                .setCronExpressionProp("DELETE_OLD_SS_INDICES_JOB_CRON_EXPRESSION")
                                .setCronExpressionPropDefault("0 0 0 ? * SUN *")
                                .setCronMisfireInstruction(
                                        CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                scheduleJob(deleteOldSSIndicesJob);
            } else {
                JobKey jobKey = new JobKey(jobName, DOTCMS_JOB_GROUP_NAME);
                if (scheduler.getJobDetail(jobKey) != null) {
                    scheduler.deleteJob(jobKey);
                }
            }
        } catch (Exception e) {
            Logger.info(DotInitScheduler.class, e.toString());
        }
    }

    private static void addPruneOldTimeMachineBackups(final Scheduler scheduler) {
        try {
            final String jobName = "PruneTimeMachineBackupJob";
            final String triggerName = "trigger35";
            final String triggerGroup = "group35";

            if (Config.getBooleanProperty("ENABLE_PRUNE_OLD_TIMEMACHINE_BACKUPS_JOB", true)) {
                final DotJobBuilder pruneOldTimeMachineJobs =
                        new DotJobBuilder()
                                .setJobClass(PruneTimeMachineBackupJob.class)
                                .setJobName(jobName)
                                .setJobGroup(DOTCMS_JOB_GROUP_NAME)
                                .setTriggerName(triggerName)
                                .setTriggerGroup(triggerGroup)
                                .setCronExpressionProp(
                                        "PRUNE_OLD_TIMEMACHINE_BACKUPS_JOB_CRON_EXPRESSION")
                                .setCronExpressionPropDefault("0 0 0 ? * SUN *")
                                .setCronMisfireInstruction(
                                        CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                scheduleJob(pruneOldTimeMachineJobs);
            } else {
                JobKey jobKey = new JobKey(jobName, DOTCMS_JOB_GROUP_NAME);
                if (scheduler.getJobDetail(jobKey) != null) {
                    scheduler.deleteJob(jobKey);
                }
            }
        } catch (Exception e) {
            Logger.info(DotInitScheduler.class, e.toString());
        }
    }

    private static void addServerHeartbeatJob() {
        final int initialDelay =
                Config.getIntProperty("SERVER_HEARTBEAT_INITIAL_DELAY_SECONDS", 60);
        final int delaySeconds = Config.getIntProperty("SERVER_HEARTBEAT_RUN_EVERY_SECONDS", 60);

        DotConcurrentFactory.getScheduledThreadPoolExecutor()
                .scheduleAtFixedRate(
                        () -> {
                            Try.run(() -> new ServerHeartbeatJob().execute(null))
                                    .onFailure(e -> Logger.warnAndDebug(DotInitScheduler.class, e));
                        },
                        initialDelay,
                        delaySeconds,
                        TimeUnit.SECONDS);
    }

    private static void scheduleJob(final DotJobBuilder jobBuilder) {
        try {
            final Scheduler scheduler = QuartzUtils.getScheduler();
            if (!jobBuilder.enabled) {
                Logger.info(
                        DotInitScheduler.class,
                        String.format(
                                "%s Quartz Job schedule disabled on this server",
                                jobBuilder.jobName));
                Logger.info(
                        DotInitScheduler.class,
                        String.format("Deleting %s Job", jobBuilder.jobName));
                JobKey jobKey = new JobKey(jobBuilder.jobName, DOTCMS_JOB_GROUP_NAME);
                if (scheduler.getJobDetail(jobKey) != null) {
                    scheduler.deleteJob(jobKey);
                }
                return;
            }
            JobDetail job;
            boolean isNew = false;
            JobKey jobKey = new JobKey(jobBuilder.jobName, jobBuilder.jobGroup);
            try {
                job = scheduler.getJobDetail(jobKey);
                if (job == null) {
                    job = JobBuilder.newJob(jobBuilder.jobClass).withIdentity(jobKey).build();
                    isNew = true;
                }
            } catch (final SchedulerException e) {
                scheduler.deleteJob(jobKey);
                job = JobBuilder.newJob(jobBuilder.jobClass).withIdentity(jobKey).build();
                isNew = true;
            }
            final Calendar calendar = Calendar.getInstance();
            final Trigger trigger =
                    TriggerBuilder.newTrigger()
                            .withIdentity(jobBuilder.triggerName, jobBuilder.triggerGroup)
                            .forJob(jobKey)
                            .withSchedule(
                                    CronScheduleBuilder.cronSchedule(
                                                    Config.getStringProperty(
                                                            jobBuilder.cronExpressionProp,
                                                            jobBuilder.cronExpressionPropDefault))
                                            .withMisfireHandlingInstructionDoNothing())
                            .startAt(calendar.getTime())
                            .build();
            scheduler.addJob(job, true);
            if (isNew) {
                scheduler.scheduleJob(trigger);
            } else {
                scheduler.rescheduleJob(trigger.getKey(), trigger);
            }
        } catch (final Exception e) {
            Logger.error(
                    DotInitScheduler.class,
                    "An error occurred when initializing the '"
                            + jobBuilder.jobName
                            + "': "
                            + e.getMessage(),
                    e);
        }
    }

    private static void addStartEndScheduledExperimentsJob(final Scheduler scheduler) {
        try {
            final String jobName = "StartEndScheduledExperimentsJob";
            final String triggerName = "trigger32";
            final String triggerGroup = "group32";

            if (Config.getBooleanProperty("ENABLE_START_END_SCHEDULED_EXPERIMENTS_JOB", true)) {
                final DotJobBuilder endFinalizedExperimentsJob =
                        new DotJobBuilder()
                                .setJobClass(StartEndScheduledExperimentsJob.class)
                                .setJobName(jobName)
                                .setJobGroup(DOTCMS_JOB_GROUP_NAME)
                                .setTriggerName(triggerName)
                                .setTriggerGroup(triggerGroup)
                                .setCronExpressionProp(
                                        "START_END_SCHEDULED_EXPERIMENTS_JOB_CRON_EXPRESSION")
                                .setCronExpressionPropDefault("0 /30 * ? * *")
                                .setCronMisfireInstruction(
                                        CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                scheduleJob(endFinalizedExperimentsJob);
            } else {
                JobKey jobKey = new JobKey(jobName, DOTCMS_JOB_GROUP_NAME);
                if (scheduler.getJobDetail(jobKey) != null) {
                    scheduler.deleteJob(jobKey);
                }
            }
        } catch (Exception e) {
            Logger.info(DotInitScheduler.class, e.toString());
        }
    }

    private static final class DotJobBuilder {
        private Class<? extends Job> jobClass = null;
        private boolean enabled = true;
        private String jobName = "";
        private String jobGroup = DOTCMS_JOB_GROUP_NAME;
        private String triggerName = "";
        private String triggerGroup = "";
        private String cronExpressionProp = "";
        private String cronExpressionPropDefault = "";
        private int cronMisfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;

        public DotJobBuilder setJobClass(Class<? extends Job> jobClass) {
            this.jobClass = jobClass;
            return this;
        }

        public DotJobBuilder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public DotJobBuilder setJobName(String jobName) {
            this.jobName = jobName;
            return this;
        }

        public DotJobBuilder setJobGroup(String jobGroup) {
            this.jobGroup = jobGroup;
            return this;
        }

        public DotJobBuilder setTriggerName(String triggerName) {
            this.triggerName = triggerName;
            return this;
        }

        public DotJobBuilder setTriggerGroup(String triggerGroup) {
            this.triggerGroup = triggerGroup;
            return this;
        }

        public DotJobBuilder setCronExpressionProp(String cronExpressionProp) {
            this.cronExpressionProp = cronExpressionProp;
            return this;
        }

        public DotJobBuilder setCronExpressionPropDefault(String cronExpressionPropDefault) {
            this.cronExpressionPropDefault = cronExpressionPropDefault;
            return this;
        }

        public DotJobBuilder setCronMisfireInstruction(int cronMisfireInstruction) {
            this.cronMisfireInstruction = cronMisfireInstruction;
            return this;
        }
    }
}