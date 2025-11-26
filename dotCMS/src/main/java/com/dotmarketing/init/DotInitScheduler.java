package com.dotmarketing.init;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.job.system.event.DeleteOldSystemEventsJob;
import com.dotcms.job.system.event.SystemEventsJob;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.telemetry.job.MetricsStatsJob;
import com.dotcms.workflow.EscalationThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.AccessTokenRenewJob;
import com.dotmarketing.quartz.job.BinaryCleanupJob;
import com.dotmarketing.quartz.job.CleanUnDeletedUsersJob;
import com.dotmarketing.quartz.job.DeleteInactiveLiveWorkingIndicesJob;
import com.dotmarketing.quartz.job.DeleteSiteSearchIndicesJob;
import com.dotmarketing.quartz.job.DropOldContentVersionsJob;
import com.dotmarketing.quartz.job.FreeServerFromClusterJob;
import com.dotmarketing.quartz.job.PruneTimeMachineBackupJob;
import com.dotmarketing.quartz.job.ServerHeartbeatJob;
import com.dotmarketing.quartz.job.StartEndScheduledExperimentsJob;
import com.dotmarketing.quartz.job.UsersToDeleteThread;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static com.dotmarketing.util.WebKeys.DOTCMS_DISABLE_WEBSOCKET_PROTOCOL;

/**
 * Initializes all dotCMS startup jobs.
 *
 * @author David H Torres
 * @version 1.0
 * @since Feb 22, 2012
 *
 */
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
			CronTrigger trigger;
			Calendar calendar;
			boolean isNew;

			// remove unused old jobs
			deleteOldJobs();

			if(Config.getBooleanProperty("ENABLE_USERS_TO_DELETE_THREAD", false)) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME, UsersToDeleteThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME, UsersToDeleteThread.class);
						isNew = true;
					}
					calendar = Calendar.getInstance();
					calendar.add(Calendar.MINUTE, 6);
					trigger = new CronTrigger("trigger7", "group7", "UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null, Config.getStringProperty("USERS_TO_DELETE_THREAD_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger7", "group7", trigger);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			} else {
		        Logger.info(DotInitScheduler.class, "Users To Delete Cron Thread schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting UsersToDeleteJob Job");
				if ((job = sched.getJobDetail("UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME);
				}
			}

			if(UtilMethods.isSet(Config.getStringProperty("BINARY_CLEANUP_JOB_CRON_EXPRESSION", null))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME, BinaryCleanupJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME, BinaryCleanupJob.class);
						isNew = true;
					}
					calendar = Calendar.getInstance();
					calendar.add(Calendar.MINUTE, 7);
				    trigger = new CronTrigger("trigger11", "group11", "BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null,Config.getStringProperty("BINARY_CLEANUP_JOB_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger11", "group11", trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
		        Logger.info(DotInitScheduler.class, "BinaryCleanupJob Cron Job schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting BinaryCleanupJob Job");
				if ((job = sched.getJobDetail("BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME);
				}
			}

			final String publishQueueJobName = "PublishQueueJob";
			//SCHEDULE PUBLISH QUEUE JOB
			if(Config.getBooleanProperty("ENABLE_PUBLISHER_QUEUE_THREAD", true)) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail(publishQueueJobName, DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail(publishQueueJobName, DOTCMS_JOB_GROUP_NAME, PublisherQueueJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob(publishQueueJobName, DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail(publishQueueJobName, DOTCMS_JOB_GROUP_NAME, PublisherQueueJob.class);
						isNew = true;
					}
					calendar = Calendar.getInstance();
					calendar.add(Calendar.MINUTE, 3);
				    trigger = new CronTrigger("trigger19", "group19", publishQueueJobName, DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null,Config.getStringProperty("PUBLISHER_QUEUE_THREAD_CRON_EXPRESSION","0 0/1 * * * "));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger19", "group19", trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
		        Logger.info(DotInitScheduler.class, "PublishQueueJob Cron Job schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting PublishQueueJob Job");
				if (null != sched.getJobDetail(publishQueueJobName, DOTCMS_JOB_GROUP_NAME)) {
					sched.deleteJob(publishQueueJobName, DOTCMS_JOB_GROUP_NAME);
				}
			}

			//SCHEDULE ESCALATION THREAD JOB
			String ETjobName = "EscalationThreadJob";
			String ETjobGroup = DOTCMS_JOB_GROUP_NAME;
			String ETtriggerName = "trigger24";
			String ETtriggerGroup = "group24";

			if(Config.getBooleanProperty("ESCALATION_ENABLE", true)) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail(ETjobName, ETjobGroup)) == null) {
							job = new JobDetail(ETjobName, ETjobGroup, EscalationThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob(ETjobName, ETjobGroup);
						job = new JobDetail(ETjobName, ETjobGroup, EscalationThread.class);
						isNew = true;
					}
					calendar = Calendar.getInstance();
					calendar.add(Calendar.MINUTE, 10);
					trigger = new CronTrigger(ETtriggerName, ETtriggerGroup, ETjobName, ETjobGroup, calendar.getTime(), null,Config.getStringProperty("ESCALATION_CHECK_INTERVAL_CRON", "0/30 * * * * ?"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob(ETtriggerName, ETtriggerGroup, trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
				if ((sched.getJobDetail(ETjobName, ETjobGroup)) != null) {
					sched.deleteJob(ETjobName, ETjobGroup);
				}
			}

			//Schedule FreeServerFromClusterJob.
			String FSCjobName = "FreeServerFromClusterJob";
			String FSCobGroup = DOTCMS_JOB_GROUP_NAME;
			String FSCtriggerName = "trigger25";
			String FSCtriggerGroup = "group25";

			if ( Config.getBooleanProperty( "ENABLE_SERVER_HEARTBEAT", true ) ) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail(FSCjobName, FSCobGroup)) == null) {
							job = new JobDetail(FSCjobName, FSCobGroup, FreeServerFromClusterJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob(FSCjobName, FSCobGroup);
						job = new JobDetail(FSCjobName, FSCobGroup, FreeServerFromClusterJob.class);
						isNew = true;
					}
					calendar = Calendar.getInstance();
					calendar.add(Calendar.MINUTE, 2);
					trigger = new CronTrigger(FSCtriggerName, FSCtriggerGroup, FSCjobName, FSCobGroup, calendar.getTime(), null,Config.getStringProperty("HEARTBEAT_CRON_EXPRESSION", "0 0/1 * * * ?"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob(FSCtriggerName, FSCtriggerGroup, trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
				if ((sched.getJobDetail(FSCjobName, FSCobGroup)) != null) {
					sched.deleteJob(FSCjobName, FSCobGroup);
				}
			}

            //Schedule CleanUnDeletedUsersJob
            String CUUjobName = "CleanUnDeletedUsersJob";
            String CUUjobGroup = DOTCMS_JOB_GROUP_NAME;
            String CUUtriggerName = "trigger26";
            String CUUtriggerGroup = "group26";

            if ( Config.getBooleanProperty( "ENABLE_CLEAN_UNDELETED_USERS", true ) ) {
                try {
                    isNew = false;

                    try {
                        if ((job = sched.getJobDetail(CUUjobName, CUUjobGroup)) == null) {
                            job = new JobDetail(CUUjobName, CUUjobGroup, CleanUnDeletedUsersJob.class);
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob(CUUjobName, CUUjobGroup);
                        job = new JobDetail(CUUjobName, CUUjobGroup, CleanUnDeletedUsersJob.class);
                        isNew = true;
                    }
                    calendar = Calendar.getInstance();
					calendar.add(Calendar.MINUTE, 30);
                    //By default, the job runs once a day at 12 AM
                    trigger = new CronTrigger(CUUtriggerName, CUUtriggerGroup, CUUjobName, CUUjobGroup, calendar.getTime(), null,Config.getStringProperty("CLEAN_USERS_CRON_EXPRESSION", "0 0 0 1/1 * ? *"));
                    trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
                    sched.addJob(job, true);

                    if (isNew)
                        sched.scheduleJob(trigger);
                    else
                        sched.rescheduleJob(CUUtriggerName, CUUtriggerGroup, trigger);
                } catch (Exception e) {
                    Logger.error(DotInitScheduler.class, e.getMessage(),e);
                }
            } else {
                if ((sched.getJobDetail(CUUjobName, CUUjobGroup)) != null) {
                    sched.deleteJob(CUUjobName, CUUjobGroup);
                }
            }

			addDropOldContentVersionsJob();
			if ( !Config.getBooleanProperty(DOTCMS_DISABLE_WEBSOCKET_PROTOCOL, false) ) {
				// Enabling the System Events Job
				addSystemEventsJob();
			}
			// start the server heartbeat job
			addServerHeartbeatJob();
			// Enabling the Delete Old System Events Job
			addDeleteOldSystemEvents(sched);

			//Enable the delete old ES Indices Job
			addDeleteOldESIndicesJob(sched);
			//Enable the delete old SS Indices Job
			addDeleteOldSiteSearchIndicesJob(sched);
			AccessTokenRenewJob.AccessTokensRenewJobScheduler.schedule();
			addStartEndScheduledExperimentsJob(sched);
			addPruneOldTimeMachineBackups(sched);
			addTelemetryMetricsStatsJob(sched);

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
		final String triggerName  = "trigger33";
		final String triggerGroup = "group33";
		final JobBuilder dropOldContentVersionsJob = new JobBuilder()
				.setJobClass(DropOldContentVersionsJob.class)
				.setJobName(DropOldContentVersionsJob.JOB_NAME)
				.setJobGroup(DOTCMS_JOB_GROUP_NAME)
				.setTriggerName(triggerName)
				.setTriggerGroup(triggerGroup)
				.setCronExpressionProp(DropOldContentVersionsJob.CRON_EXPR_PROP)
				.setCronExpressionPropDefault(DropOldContentVersionsJob.CRON_EXPRESSION.get())
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

			JobBuilder deleteOldSystemEventsJob = new JobBuilder().setJobClass(DeleteOldSystemEventsJob.class)
					.setJobName(DOSEjobName)
					.setJobGroup(DOTCMS_JOB_GROUP_NAME)
					.setTriggerName(DOSEtriggerName)
					.setTriggerGroup(DOSEtriggerGroup)
					.setCronExpressionProp("DELETE_OLD_SYSTEM_EVENTS_CRON_EXPRESSION")
					.setCronExpressionPropDefault("0 0 0 1/3 * ? *")
					.setCronMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
			scheduleJob(deleteOldSystemEventsJob);
		} else {

			if ((sched.getJobDetail(DOSEjobName, DOTCMS_JOB_GROUP_NAME)) != null) {
				sched.deleteJob(DOSEjobName, DOTCMS_JOB_GROUP_NAME);
			}
		}

	} // addDeleteOldSystemEvents.

	private static void addSystemEventsJob () {

		if (Config.getBooleanProperty("ENABLE_SYSTEM_EVENTS", true)) {
			try {

				final int initialDelay = Config.getIntProperty("SYSTEM_EVENTS_INITIAL_DELAY", 0);
				final int delaySeconds = Config.getIntProperty("SYSTEM_EVENTS_DELAY_SECONDS", 5); // runs every 5 seconds.
				DotConcurrentFactory.getScheduledThreadPoolExecutor().scheduleWithFixedDelay(new SystemEventsJob(), initialDelay, delaySeconds, TimeUnit.SECONDS);
			} catch (Exception e) {

				Logger.info(DotInitScheduler.class, e.toString());
			}
		}
	} // addSystemEventsJob.


	private static void addDeleteOldESIndicesJob (final Scheduler scheduler) {
		try {
			final String jobName      = "DeleteOldESIndicesJob";
			final String triggerName  = "trigger30";
			final String triggerGroup = "group30";

			if (Config.getBooleanProperty( "ENABLE_DELETE_OLD_ES_INDICES_JOB", true)) {

				final JobBuilder deleteOldESIndicesJob = new JobBuilder().setJobClass(
						DeleteInactiveLiveWorkingIndicesJob.class)
						.setJobName(jobName)
						.setJobGroup(DOTCMS_JOB_GROUP_NAME)
						.setTriggerName(triggerName)
						.setTriggerGroup(triggerGroup)
						.setCronExpressionProp("DELETE_OLD_ES_INDICES_JOB_CRON_EXPRESSION")
						.setCronExpressionPropDefault("0 0 1 ? * *")
						.setCronMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
				scheduleJob(deleteOldESIndicesJob);
			} else {
				if ((scheduler.getJobDetail(jobName, DOTCMS_JOB_GROUP_NAME)) != null) {
					scheduler.deleteJob(jobName, DOTCMS_JOB_GROUP_NAME);
				}
			}
		} catch (Exception e) {

			Logger.info(DotInitScheduler.class, e.toString());
		}
	}


/**
 * This method is used to schedule or delete a Quartz job that deletes old Site Search indices.
 * The job is scheduled based on a cron expression defined in the system configuration.
 *
 * @param scheduler The Quartz scheduler instance used to schedule.
 */
private static void addDeleteOldSiteSearchIndicesJob (final Scheduler scheduler) {
	try {
		// Define the job and trigger names and group
		final String jobName      = "RemoveOldSiteSearchIndicesJob";
		final String triggerName  = "trigger34";
		final String triggerGroup = "group34";

		// Check if the job is enabled in the system configuration
		if (Config.getBooleanProperty( "ENABLE_DELETE_OLD_SS_INDICES_JOB", true)) {

			// If the job is enabled, build the job details
			final JobBuilder deleteOldSSIndicesJob = new JobBuilder().setJobClass(
							DeleteSiteSearchIndicesJob.class)
					.setJobName(jobName)
					.setJobGroup(DOTCMS_JOB_GROUP_NAME)
					.setTriggerName(triggerName)
					.setTriggerGroup(triggerGroup)
					.setCronExpressionProp("DELETE_OLD_SS_INDICES_JOB_CRON_EXPRESSION")
					// Set the default cron expression to run every Sunday at midnight
					.setCronExpressionPropDefault("0 0 0 ? * SUN *")
					.setCronMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
			scheduleJob(deleteOldSSIndicesJob);
		} else {
			// If the job is not enabled, delete it from the scheduler if it exists
			if ((scheduler.getJobDetail(jobName, DOTCMS_JOB_GROUP_NAME)) != null) {
				scheduler.deleteJob(jobName, DOTCMS_JOB_GROUP_NAME);
			}
		}
	} catch (Exception e) {
		Logger.info(DotInitScheduler.class, e.toString());
	}
}

	/**
	 * This method is used to schedule or delete a Quartz job that deletes old Time Machine backups.
	 * The job is scheduled based on a cron expression defined in the system configuration.
	 *
	 * @param scheduler The Quartz scheduler instance used to schedule.
	 */
	private static void addPruneOldTimeMachineBackups (final Scheduler scheduler) {
		try {
			// Define the job and trigger names and group
			final String jobName      = "PruneTimeMachineBackupJob";
			final String triggerName  = "trigger35";
			final String triggerGroup = "group35";

			// Check if the job is enabled in the system configuration
			if (Config.getBooleanProperty( "ENABLE_PRUNE_OLD_TIMEMACHINE_BACKUPS_JOB", true)) {

				// If the job is enabled, build the job details
				final JobBuilder pruneOldTimeMachineJobs = new JobBuilder().setJobClass(
								PruneTimeMachineBackupJob.class)
						.setJobName(jobName)
						.setJobGroup(DOTCMS_JOB_GROUP_NAME)
						.setTriggerName(triggerName)
						.setTriggerGroup(triggerGroup)
						.setCronExpressionProp("PRUNE_OLD_TIMEMACHINE_BACKUPS_JOB_CRON_EXPRESSION")
						// Set the default cron expression to run every Sunday at midnight
						.setCronExpressionPropDefault("0 0 0 ? * SUN *")
						.setCronMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
				scheduleJob(pruneOldTimeMachineJobs);
			} else {
				// If the job is not enabled, delete it from the scheduler if it exists
				if ((scheduler.getJobDetail(jobName, DOTCMS_JOB_GROUP_NAME)) != null) {
					scheduler.deleteJob(jobName, DOTCMS_JOB_GROUP_NAME);
				}
			}
		} catch (Exception e) {
			Logger.info(DotInitScheduler.class, e.toString());
		}
	}

	/**
	 * Adds the {@link MetricsStatsJob} Quartz Job to the scheduler during the startup
	 * process.
	 */
	private static void addTelemetryMetricsStatsJob(final Scheduler scheduler) {
		if (Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_TELEMETRY_CORE_ENABLED, true)) {
			final String triggerName  = "trigger36";
			final String triggerGroup = "group36";
			final JobBuilder telemetryMetricsStatsJob = new JobBuilder()
					.setJobClass(MetricsStatsJob.class)
					.setJobName(MetricsStatsJob.JOB_NAME)
					.setJobGroup(DOTCMS_JOB_GROUP_NAME)
					.setTriggerName(triggerName)
					.setTriggerGroup(triggerGroup)
					.setCronExpressionProp(MetricsStatsJob.CRON_EXPR_PROP)
					.setCronExpressionPropDefault(MetricsStatsJob.CRON_EXPRESSION.get())
					.setCronMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
			if (Boolean.FALSE.equals(MetricsStatsJob.ENABLED.get())) {
				telemetryMetricsStatsJob.enabled(false);
			}
			scheduleJob(telemetryMetricsStatsJob);
		} else {
            try {
				// If the job is not enabled, delete it from the scheduler if it exists
                if (null != (scheduler.getJobDetail(MetricsStatsJob.JOB_NAME, DOTCMS_JOB_GROUP_NAME))) {
                    scheduler.deleteJob(MetricsStatsJob.JOB_NAME, DOTCMS_JOB_GROUP_NAME);
                }
            } catch (final SchedulerException e) {
				Logger.warn(DotInitScheduler.class, e.toString());
            }
        }
	}

   private static void addServerHeartbeatJob () {

       final int initialDelay = Config.getIntProperty("SERVER_HEARTBEAT_INITIAL_DELAY_SECONDS", 60);
       final int delaySeconds = Config.getIntProperty("SERVER_HEARTBEAT_RUN_EVERY_SECONDS", 60); // runs every 5 seconds.
       
       DotConcurrentFactory.getScheduledThreadPoolExecutor().scheduleAtFixedRate(() -> {
           Try.run(() -> new ServerHeartbeatJob().execute(null)).onFailure(e->Logger.warnAndDebug(DotInitScheduler.class, e));
       }, initialDelay, delaySeconds, TimeUnit.SECONDS);
    } 
	

	/**
	 * Creates a Quartz Job and schedules it for execution. If the Job has
	 * already been registered, it will be re-scheduled to run again.
	 *
	 * @param jobBuilder
	 *            - Class containing all the information that the Quartz Job
	 *            needs to execute.
	 */
	private static void scheduleJob(final JobBuilder jobBuilder) {
		try {
			final Scheduler scheduler = QuartzUtils.getScheduler();
			if (!jobBuilder.enabled) {
				Logger.info(DotInitScheduler.class, String.format("%s Quartz Job schedule disabled on this server", jobBuilder.jobName));
				Logger.info(DotInitScheduler.class, String.format("Deleting %s Job", jobBuilder.jobName));
				if ((scheduler.getJobDetail(jobBuilder.jobName, DOTCMS_JOB_GROUP_NAME)) != null) {
					scheduler.deleteJob(jobBuilder.jobName, DOTCMS_JOB_GROUP_NAME);
				}
				return;
			}
			JobDetail job;
			boolean isNew = false;
			try {
				if ((job = scheduler.getJobDetail(jobBuilder.jobName, jobBuilder.jobGroup)) == null) {
					job = new JobDetail(jobBuilder.jobName, jobBuilder.jobGroup, jobBuilder.jobClass);
					isNew = true;
				}
			} catch (final SchedulerException e) {
				// Try to re-create the job once more
				scheduler.deleteJob(jobBuilder.jobName, jobBuilder.jobGroup);
				job = new JobDetail(jobBuilder.jobName, jobBuilder.jobGroup, jobBuilder.jobClass);
				isNew = true;
			}
			final Calendar calendar = Calendar.getInstance();
			final CronTrigger trigger = new CronTrigger(jobBuilder.triggerName, jobBuilder.triggerGroup, jobBuilder.jobName,
					jobBuilder.jobGroup, calendar.getTime(), null, Config.getStringProperty(jobBuilder.cronExpressionProp,
							jobBuilder.cronExpressionPropDefault));
			trigger.setMisfireInstruction(jobBuilder.cronMisfireInstruction);
			scheduler.addJob(job, true);
			if (isNew) {
				scheduler.scheduleJob(trigger);
			} else {
				scheduler.rescheduleJob(jobBuilder.triggerName, jobBuilder.triggerGroup, trigger);
			}
		} catch (final Exception e) {
			Logger.error(DotInitScheduler.class, "An error occurred when initializing the '" + jobBuilder.jobName + "': "
					+ e.getMessage(), e);
		}
	}

	private static void addStartEndScheduledExperimentsJob(final Scheduler scheduler) {
		try {
			final String jobName      = "StartEndScheduledExperimentsJob";
			final String triggerName  = "trigger32";
			final String triggerGroup = "group32";

			if (Config.getBooleanProperty( "ENABLE_START_END_SCHEDULED_EXPERIMENTS_JOB", true)) {
				final JobBuilder endFinalizedExperimentsJob = new JobBuilder().setJobClass(
								StartEndScheduledExperimentsJob.class)
						.setJobName(jobName)
						.setJobGroup(DOTCMS_JOB_GROUP_NAME)
						.setTriggerName(triggerName)
						.setTriggerGroup(triggerGroup)
						.setCronExpressionProp("START_END_SCHEDULED_EXPERIMENTS_JOB_CRON_EXPRESSION")
						.setCronExpressionPropDefault("0 /30 * ? * *")
						.setCronMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
				scheduleJob(endFinalizedExperimentsJob);
			} else {
				if ((scheduler.getJobDetail(jobName, DOTCMS_JOB_GROUP_NAME)) != null) {
					scheduler.deleteJob(jobName, DOTCMS_JOB_GROUP_NAME);
				}
			}
		} catch (Exception e) {
			Logger.info(DotInitScheduler.class, e.toString());
		}
	}

	/**
	 * Utility builder class for creating and scheduling Quartz Jobs.
	 *
	 * @author Jose Castro
	 * @version 3.7
	 * @since Jul 15, 2016
	 *
	 */
	private static final class JobBuilder {

		private Class<? extends Job> jobClass = null;
		private boolean enabled = true;
		private String jobName = "";
		private String jobGroup = DOTCMS_JOB_GROUP_NAME;
		private String triggerName = "";
		private String triggerGroup = "";
		private String cronExpressionProp = "";
		private String cronExpressionPropDefault = "";
		private int cronMisfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;

		/**
		 * Sets the class representing the Quartz Job to execute.
		 *
		 * @param jobClass
		 *            - The Quartz job.
		 */
		public JobBuilder setJobClass(Class<? extends Job> jobClass) {
			this.jobClass = jobClass;
			return this;
		}

		/**
		 * Enables/disabled the specified Job in the current dotCMS instance.
		 *
		 * @param enabled If the Job must be scheduled, set this to {@code true}. If it must be
		 *                deleted from the scheduler, set this to {@code false}.
		 *
		 * @return The Quartz Job.
		 */
		public JobBuilder enabled(final boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		/**
		 * Sets the name of the Job.
		 *
		 * @param jobName
		 *            - The job name.
		 */
		public JobBuilder setJobName(String jobName) {
			this.jobName = jobName;
			return this;
		}

		/**
		 * Sets the name of the Quartz Group the Job will belong to.
		 *
		 * @param jobGroup
		 *            - The job group name.
		 */
		public JobBuilder setJobGroup(String jobGroup) {
			this.jobGroup = jobGroup;
			return this;
		}

		/**
		 * Sets the name of the trigger for the specified Job.
		 *
		 * @param triggerName
		 *            - The trigger name.
		 */
		public JobBuilder setTriggerName(String triggerName) {
			this.triggerName = triggerName;
			return this;
		}

		/**
		 * Sets the name of the trigger group for the specified Job.
		 *
		 * @param triggerGroup
		 *            - The trigger group name.
		 */
		public JobBuilder setTriggerGroup(String triggerGroup) {
			this.triggerGroup = triggerGroup;
			return this;
		}

		/**
		 * Sets the property name in the {@code dotmarketing-config.properties}
		 * file that contains the user-defined cron expression which defines the
		 * execution times of the Job.
		 *
		 * @param cronExpressionProp
		 *            - The cron expression property name.
		 */
		public JobBuilder setCronExpressionProp(String cronExpressionProp) {
			this.cronExpressionProp = cronExpressionProp;
			return this;
		}

		/**
		 * Sets the default value of the Job cron expression in case it's not
		 * defined in the {@code dotmarketing-config.properties} configuration
		 * file.
		 *
		 * @param cronExpressionPropDefault
		 *            - The default cron expression value.
		 */
		public JobBuilder setCronExpressionPropDefault(String cronExpressionPropDefault) {
			this.cronExpressionPropDefault = cronExpressionPropDefault;
			return this;
		}

		/**
		 * Determines what the Quartz handle needs to do if the job scheduler
		 * discovers a misfire situation, i.e., if the application was not
		 * able to start the job at the specified date/time.
		 *
		 * @param cronMisfireInstruction
		 *            - The miss-fire instruction.
		 */
		public JobBuilder setCronMisfireInstruction(int cronMisfireInstruction) {
			this.cronMisfireInstruction = cronMisfireInstruction;
			return this;
		}

	}

}
