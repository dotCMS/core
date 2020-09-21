package com.dotmarketing.init;

import static com.dotmarketing.util.WebKeys.DOTCMS_DISABLE_WEBSOCKET_PROTOCOL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import com.dotcms.enterprise.linkchecker.LinkCheckerJob;
import com.dotcms.job.system.event.DeleteOldSystemEventsJob;
import com.dotcms.job.system.event.SystemEventsJob;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.workflow.EscalationThread;
import com.dotmarketing.quartz.DotSchedulerFactory;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.BinaryCleanupJob;
import com.dotmarketing.quartz.job.CleanUnDeletedUsersJob;
import com.dotmarketing.quartz.job.ContentReindexerThread;
import com.dotmarketing.quartz.job.DeleteOldClickstreams;
import com.dotmarketing.quartz.job.FreeServerFromClusterJob;
import com.dotmarketing.quartz.job.ServerHeartbeatJob;
import com.dotmarketing.quartz.job.TrashCleanupJob;
import com.dotmarketing.quartz.job.UsersToDeleteThread;
import com.dotmarketing.quartz.job.WebDavCleanupJob;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

/**
 * Initializes all dotCMS startup jobs.
 *
 * @author David H Torres
 * @version 1.0
 * @since Feb 22, 2012
 *
 */
public class DotInitScheduler {

	private static final String DOTCMS_JOB_GROUP_NAME = "dotcms_jobs";
	public static final String SCHEDULER_COREPOOLSIZE = "SCHEDULER_CORE_POOL_SIZE";

	private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = null;

	/**
	 * Returns the {@link ScheduledThreadPoolExecutor}
	 * @return ScheduledThreadPoolExecutor
	 */
	public static ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {

		if (null == scheduledThreadPoolExecutor) {

			synchronized (DotInitScheduler.class) {

				if (null == scheduledThreadPoolExecutor) {

					final int corePoolSize = Config.getIntProperty(SCHEDULER_COREPOOLSIZE, 10);
					scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
				}
			}
		}

		return scheduledThreadPoolExecutor;
	}

	/**
	 * Configures and initializes every system Job to run on dotCMS.
	 *
	 * @throws SchedulerException
	 *             An error occurred when trying to schedule one of our system
	 *             jobs.
	 */
	public static void start() throws SchedulerException {
		try {
			final Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
			JobDetail job;
			CronTrigger trigger;
			Calendar calendar;
			boolean isNew;


			if(Config.getBooleanProperty("ENABLE_CONTENT_REINDEXATION_THREAD", false)) {
				try {
					ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
					scheduledThreadPoolExecutor.scheduleWithFixedDelay(new ContentReindexerThread(), Config.getIntProperty("EXEC_CONTENT_REINDEXATION_INIT_DELAY"), Config.getIntProperty("EXEC_CONTENT_REINDEXATION_DELAY"), TimeUnit.SECONDS);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			} else {
		        Logger.info(DotInitScheduler.class, "Automatic Content Reindexation Cron Thread schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting ContentReindexerJob Job");
				if ((job = sched.getJobDetail("ContentReindexerJob", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("ContentReindexerJob", DOTCMS_JOB_GROUP_NAME);
				}
			}

			if(Config.getBooleanProperty("ENABLE_USERS_TO_DELETE_THREAD")) {
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
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND, Config.getIntProperty("USERS_TO_DELETE_THREAD_INIT_DELAY"));
					trigger = new CronTrigger("trigger7", "group7", "UsersToDeleteJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null, Config.getStringProperty("USERS_TO_DELETE_THREAD_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
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




			if(UtilMethods.isSet(Config.getStringProperty("WEBDAV_CLEANUP_JOB_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("WebDavCleanupJob", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("WebDavCleanupJob", DOTCMS_JOB_GROUP_NAME, WebDavCleanupJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("WebDavCleanupJob", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("WebDavCleanupJob", DOTCMS_JOB_GROUP_NAME, WebDavCleanupJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					trigger = new CronTrigger("trigger10", "group10", "WebDavCleanupJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null, Config.getStringProperty("WEBDAV_CLEANUP_JOB_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger10", "group8", trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
		        Logger.info(DotInitScheduler.class, "WebDavCleanupJob Cron Job schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting WebDavCleanupJob Job");
				if ((job = sched.getJobDetail("WebDavCleanupJob", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("WebDavCleanupJob", DOTCMS_JOB_GROUP_NAME);
				}
			}
			//http://jira.dotmarketing.net/browse/DOTCMS-1073
			if(UtilMethods.isSet(Config.getStringProperty("BINARY_CLEANUP_JOB_CRON_EXPRESSION"))) {
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
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger11", "group11", "BinaryCleanupJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null,Config.getStringProperty("BINARY_CLEANUP_JOB_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
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


			if(UtilMethods.isSet(Config.getStringProperty("TRASH_CLEANUP_JOB_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("TrashCleanupJob", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("TrashCleanupJob", DOTCMS_JOB_GROUP_NAME, TrashCleanupJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("TrashCleanupJob", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("TrashCleanupJob", DOTCMS_JOB_GROUP_NAME, TrashCleanupJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger12", "group12", "TrashCleanupJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null,Config.getStringProperty("TRASH_CLEANUP_JOB_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger12", "group12", trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
		        Logger.info(DotInitScheduler.class, "TrashCleanupJob Cron Job schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting TrashCleanupJob Job");
				if ((job = sched.getJobDetail("TrashCleanupJob", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("TrashCleanupJob", DOTCMS_JOB_GROUP_NAME);
				}
			}




			if(Config.getBooleanProperty("ENABLE_DELETE_OLDER_CLICKSTREAMS", false)){
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("DeleteOldClickstreams", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("DeleteOldClickstreams", DOTCMS_JOB_GROUP_NAME, DeleteOldClickstreams.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("DeleteOldClickstreams", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("DeleteOldClickstreams", DOTCMS_JOB_GROUP_NAME, DeleteOldClickstreams.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					trigger = new CronTrigger("trigger18", "group18", "DeleteOldClickstreams", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null, Config.getStringProperty("DELETE_OLDER_CLICKSTREAMS_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger18", "group18", trigger);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			}

			//SCHEDULE PUBLISH QUEUE JOB
			if(Config.getBooleanProperty("ENABLE_PUBLISHER_QUEUE_THREAD", true)) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("PublishQueueJob", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("PublishQueueJob", DOTCMS_JOB_GROUP_NAME, PublisherQueueJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("PublishQueueJob", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("PublishQueueJob", DOTCMS_JOB_GROUP_NAME, PublisherQueueJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger19", "group19", "PublishQueueJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null,Config.getStringProperty("PUBLISHER_QUEUE_THREAD_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
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
				if ((job = sched.getJobDetail("PublishQueueJob", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("PublishQueueJob", DOTCMS_JOB_GROUP_NAME);
				}
			}


			final String lc="linkchecker";
            final String lg=DOTCMS_JOB_GROUP_NAME;
			if(Config.getBooleanProperty("linkchecker.enablejob",false)) {
                try {
                    isNew = false;

                    try {
                        if ((job = sched.getJobDetail(lc, lg)) == null) {
                            job = new JobDetail(lc,lg, LinkCheckerJob.class);
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob(lc,lg);
                        job = new JobDetail(lc,lg, LinkCheckerJob.class);
                        isNew = true;
                    }
                    calendar = GregorianCalendar.getInstance();
                    trigger = new CronTrigger("trigger20", "group20", lc,lg, calendar.getTime(),
                                  null,Config.getStringProperty("linkchecker.cronexp","0 0 0/2 * * ?"));
                    trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                    sched.addJob(job, true);

                    if (isNew)
                        sched.scheduleJob(trigger);
                    else {
                        CronTrigger existing=(CronTrigger)sched.getTrigger("trigger20", "group20");
                        if(!existing.getCronExpression().equals(trigger.getCronExpression())) {
                            sched.rescheduleJob("trigger20", "group20", trigger);
                        }
                    }
                } catch (Exception e) {
                    Logger.error(DotInitScheduler.class, e.getMessage(),e);
                }
            } else {
                Logger.info(DotInitScheduler.class, "LinkCheckerJob Cron Job schedule disabled on this server");
                if ((job = sched.getJobDetail(lc, lg)) != null) {
                    sched.deleteJob(lc, lg);
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
					calendar = GregorianCalendar.getInstance();
					trigger = new CronTrigger(ETtriggerName, ETtriggerGroup, ETjobName, ETjobGroup, calendar.getTime(), null,Config.getStringProperty("ESCALATION_CHECK_INTERVAL_CRON", "0/30 * * * * ?"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
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
					calendar = GregorianCalendar.getInstance();
					trigger = new CronTrigger(FSCtriggerName, FSCtriggerGroup, FSCjobName, FSCobGroup, calendar.getTime(), null,Config.getStringProperty("HEARTBEAT_CRON_EXPRESSION", "0 0/1 * * * ?"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
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
                    calendar = GregorianCalendar.getInstance();
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

			if ( !Config.getBooleanProperty(DOTCMS_DISABLE_WEBSOCKET_PROTOCOL, false) ) {
				// Enabling the System Events Job
				addSystemEventsJob();
			}
			
			
			// start the server heartbeat job
			addServerHeartbeatJob();
			
			

			// Enabling the Delete Old System Events Job
			addDeleteOldSystemEvents(sched);

            //Starting the sequential and standard Schedulers
	        QuartzUtils.startSchedulers();
		} catch (SchedulerException e) {
			Logger.fatal(DotInitScheduler.class, "An error as ocurred scheduling critical startup task of dotCMS, the system will shutdown immediately, " + e.toString(), e);
			throw e;
		}
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
					.setCronMissfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
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
				getScheduledThreadPoolExecutor().scheduleWithFixedDelay(new SystemEventsJob(), initialDelay, delaySeconds, TimeUnit.SECONDS);
			} catch (Exception e) {

				Logger.info(DotInitScheduler.class, e.toString());
			}
		}
	} // addSystemEventsJob.
	

   private static void addServerHeartbeatJob () {

       final int initialDelay = Config.getIntProperty("SERVER_HEARTBEAT_INITIAL_DELAY_SECONDS", 60);
       final int delaySeconds = Config.getIntProperty("SERVER_HEARTBEAT_RUN_EVERY_SECONDS", 60); // runs every 5 seconds.
       
       DotInitScheduler.getScheduledThreadPoolExecutor().scheduleAtFixedRate(() -> {
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
			Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
			Calendar calendar;
			JobDetail job;
			CronTrigger trigger;
			boolean isNew = false;
			try {
				if ((job = sched.getJobDetail(jobBuilder.jobName, jobBuilder.jobGroup)) == null) {
					job = new JobDetail(jobBuilder.jobName, jobBuilder.jobGroup, jobBuilder.jobClass);
					isNew = true;
				}
			} catch (SchedulerException e) {
				// Try to re-create the job once more
				sched.deleteJob(jobBuilder.jobName, jobBuilder.jobGroup);
				job = new JobDetail(jobBuilder.jobName, jobBuilder.jobGroup, jobBuilder.jobClass);
				isNew = true;
			}
			calendar = GregorianCalendar.getInstance();
			trigger = new CronTrigger(jobBuilder.triggerName, jobBuilder.triggerGroup, jobBuilder.jobName,
					jobBuilder.jobGroup, calendar.getTime(), null, Config.getStringProperty(jobBuilder.cronExpressionProp,
							jobBuilder.cronExpressionPropDefault));
			trigger.setMisfireInstruction(jobBuilder.cronMissfireInstruction);
			sched.addJob(job, true);
			if (isNew) {
				sched.scheduleJob(trigger);
			} else {
				sched.rescheduleJob(jobBuilder.triggerName, jobBuilder.triggerGroup, trigger);
			}
		} catch (Exception e) {
			Logger.error(DotInitScheduler.class, "An error occurred when initializing the '" + jobBuilder.jobName + "': "
					+ e.getMessage(), e);
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
		private String jobName = "";
		private String jobGroup = DOTCMS_JOB_GROUP_NAME;
		private String triggerName = "";
		private String triggerGroup = "";
		private String cronExpressionProp = "";
		private String cronExpressionPropDefault = "";
		private int cronMissfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;

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
		 * discovers a miss-fire situation, i.e., if the application was not
		 * able to start the job at the specified date/time.
		 *
		 * @param cronMissfireInstruction
		 *            - The miss-fire instruction.
		 */
		public JobBuilder setCronMissfireInstruction(int cronMissfireInstruction) {
			this.cronMissfireInstruction = cronMissfireInstruction;
			return this;
		}

	}

}
