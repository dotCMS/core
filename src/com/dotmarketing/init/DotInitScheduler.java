package com.dotmarketing.init;

import java.lang.management.ManagementFactory;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import com.dotcms.enterprise.DashboardProxy;
import com.dotcms.enterprise.linkchecker.LinkCheckerJob;
import com.dotcms.job.system.event.DeleteOldSystemEventsJob;
import com.dotcms.job.system.event.SystemEventsJob;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.workflow.EscalationThread;
import com.dotmarketing.business.cluster.mbeans.Cluster;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.BinaryCleanupJob;
import com.dotmarketing.quartz.job.CalendarReminderThread;
import com.dotmarketing.quartz.job.ContentFromEmailJob;
import com.dotmarketing.quartz.job.ContentReindexerThread;
import com.dotmarketing.quartz.job.ContentReviewThread;
import com.dotmarketing.quartz.job.DeleteInactiveClusterServersJob;
import com.dotmarketing.quartz.job.DeleteOldClickstreams;
import com.dotmarketing.quartz.job.DeliverCampaignThread;
import com.dotmarketing.quartz.job.DistReindexJournalCleanupThread;
import com.dotmarketing.quartz.job.DistReindexJournalCleanupThread2;
import com.dotmarketing.quartz.job.FreeServerFromClusterJob;
import com.dotmarketing.quartz.job.PopBouncedMailThread;
import com.dotmarketing.quartz.job.ServerHeartbeatJob;
import com.dotmarketing.quartz.job.TrashCleanupJob;
import com.dotmarketing.quartz.job.UpdateRatingThread;
import com.dotmarketing.quartz.job.UsersToDeleteThread;
import com.dotmarketing.quartz.job.WebDavCleanupJob;
import com.dotmarketing.servlets.InitServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

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

	/**
	 * Configures and initializes every system Job to run on dotCMS.
	 * 
	 * @throws SchedulerException
	 *             An error occurred when trying to schedule one of our system
	 *             jobs.
	 */
	public static void start() throws SchedulerException {
		try {
			Scheduler sched = QuartzUtils.getStandardScheduler();
			JobDetail job;
			CronTrigger trigger;
			Calendar calendar;
			boolean isNew;

			if(Config.getBooleanProperty("ENABLE_DELIVER_CAMPAIGN_THREAD")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("DeliverCampaignJob", "dotcms_jobs")) == null) {
							job = new JobDetail("DeliverCampaignJob", "dotcms_jobs", DeliverCampaignThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("DeliverCampaignJob", "dotcms_jobs");
						job = new JobDetail("DeliverCampaignJob", "dotcms_jobs", DeliverCampaignThread.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND, Config.getIntProperty("DELIVER_CAMPAIGN_THREAD_INIT_DELAY"));
					trigger = new CronTrigger("trigger1", "group1", "DeliverCampaignJob", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("DELIVER_CAMPAIGN_THREAD_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger1", "group1", trigger);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			} else {
		        Logger.info(DotInitScheduler.class, "Deliver Campaign Cron Thread schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting DeliverCampaignJob Job");
				if ((job = sched.getJobDetail("DeliverCampaignJob", "dotcms_jobs")) != null) {
					sched.deleteJob("DeliverCampaignJob", "dotcms_jobs");
				}
			}

			if(Config.getBooleanProperty("ENABLE_UPDATE_RATINGS_THREAD")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("UpdateRatingJob", "dotcms_jobs")) == null) {
							job = new JobDetail("UpdateRatingJob", "dotcms_jobs", UpdateRatingThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("UpdateRatingJob", "dotcms_jobs");
						job = new JobDetail("UpdateRatingJob", "dotcms_jobs", UpdateRatingThread.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND, Config.getIntProperty("UPDATE_RATINGS_THREAD_INIT_DELAY"));
					trigger = new CronTrigger("trigger2", "group2", "UpdateRatingJob", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("UPDATE_RATINGS_THREAD_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger2", "group2", trigger);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			} else {
		        Logger.info(DotInitScheduler.class, "Update Rating Cron Thread schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting UpdateRatingJob Job");
				if ((job = sched.getJobDetail("UpdateRatingJob", "dotcms_jobs")) != null) {
					sched.deleteJob("UpdateRatingJob", "dotcms_jobs");
				}
			}

			if(Config.getBooleanProperty("ENABLE_CONTENT_REVIEW_THREAD")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("ContentReviewJob", "dotcms_jobs")) == null) {
							job = new JobDetail("ContentReviewJob", "dotcms_jobs", ContentReviewThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("ContentReviewJob", "dotcms_jobs");
						job = new JobDetail("ContentReviewJob", "dotcms_jobs", ContentReviewThread.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND, Config.getIntProperty("EXEC_INIT_DELAY"));
					trigger = new CronTrigger("trigger3", "group3", "ContentReviewJob", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("CONTENT_REVIEW_THREAD_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger3", "group3", trigger);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			} else {
		        Logger.info(DotInitScheduler.class, "Content Review Cron Thread schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting ContentReviewJob Job");
				if ((job = sched.getJobDetail("ContentReviewJob", "dotcms_jobs")) != null) {
					sched.deleteJob("ContentReviewJob", "dotcms_jobs");
				}
			}


	        try {
	        	//Register cluster MBean

	        	MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				ObjectName name;

				name = new ObjectName("org.dotcms:type=Cluster");
				Cluster clusteMBean = new Cluster();
				mbs.registerMBean(clusteMBean, name);
			} catch (MalformedObjectNameException e) {
				Logger.debug(InitServlet.class,"MalformedObjectNameException: " + e.getMessage(),e);
			} catch (InstanceAlreadyExistsException e) {
				Logger.debug(InitServlet.class,"InstanceAlreadyExistsException: " + e.getMessage(),e);
			} catch (MBeanRegistrationException e) {
				Logger.debug(InitServlet.class,"MBeanRegistrationException: " + e.getMessage(),e);
			} catch (NotCompliantMBeanException e) {
				Logger.debug(InitServlet.class,"NotCompliantMBeanException: " + e.getMessage(),e);
			} catch (NullPointerException e) {
				Logger.debug(InitServlet.class,"NullPointerException: " + e.getMessage(),e);
			}

			if(Config.getBooleanProperty("ENABLE_CONTENT_REINDEXATION_THREAD")) {
				try {
					ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
					scheduledThreadPoolExecutor.scheduleWithFixedDelay(new ContentReindexerThread(), Config.getIntProperty("EXEC_CONTENT_REINDEXATION_INIT_DELAY"), Config.getIntProperty("EXEC_CONTENT_REINDEXATION_DELAY"), TimeUnit.SECONDS);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			} else {
		        Logger.info(DotInitScheduler.class, "Automatic Content Reindexation Cron Thread schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting ContentReindexerJob Job");
				if ((job = sched.getJobDetail("ContentReindexerJob", "dotcms_jobs")) != null) {
					sched.deleteJob("ContentReindexerJob", "dotcms_jobs");
				}
			}

			//Bounces popper task
			if(Config.getBooleanProperty("ENABLE_POP_BOUNCES_THREAD")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("PopBouncedMailJob", "dotcms_jobs")) == null) {
							job = new JobDetail("PopBouncedMailJob", "dotcms_jobs", PopBouncedMailThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("PopBouncedMailJob", "dotcms_jobs");
						job = new JobDetail("PopBouncedMailJob", "dotcms_jobs", PopBouncedMailThread.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND, Config.getIntProperty("EXEC_POP_BOUNCES_INIT_DELAY"));
					trigger = new CronTrigger("trigger6", "group6", "PopBouncedMailJob", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("POP_BOUNCES_THREAD_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger6", "group6", trigger);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			} else {
		        Logger.info(DotInitScheduler.class, "Automatic Bounces Retrieval Cron Thread schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting PopBouncedMailJob Job");
		        if ((job = sched.getJobDetail("PopBouncedMailJob", "dotcms_jobs")) != null) {
					sched.deleteJob("PopBouncedMailJob", "dotcms_jobs");
				}
			}

			if(Config.getBooleanProperty("ENABLE_USERS_TO_DELETE_THREAD")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("UsersToDeleteJob", "dotcms_jobs")) == null) {
							job = new JobDetail("UsersToDeleteJob", "dotcms_jobs", UsersToDeleteThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("UsersToDeleteJob", "dotcms_jobs");
						job = new JobDetail("UsersToDeleteJob", "dotcms_jobs", UsersToDeleteThread.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND, Config.getIntProperty("USERS_TO_DELETE_THREAD_INIT_DELAY"));
					trigger = new CronTrigger("trigger7", "group7", "UsersToDeleteJob", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("USERS_TO_DELETE_THREAD_CRON_EXPRESSION"));
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
				if ((job = sched.getJobDetail("UsersToDeleteJob", "dotcms_jobs")) != null) {
					sched.deleteJob("UsersToDeleteJob", "dotcms_jobs");
				}
			}

			//Calendar Reminder Job

			if(Config.getBooleanProperty("CALENDAR_REMINDER_THREAD")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("CalendarReminderJob", "dotcms_jobs")) == null) {
							job = new JobDetail("CalendarReminderJob", "dotcms_jobs", CalendarReminderThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("CalendarReminderJob", "dotcms_jobs");
						job = new JobDetail("CalendarReminderJob", "dotcms_jobs", CalendarReminderThread.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND,Config.getIntProperty("CALENDAR_REMINDER_THREAD_INIT_DELAY"));
					trigger = new CronTrigger("trigger8", "group8", "CalendarReminderJob", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("CALENDAR_REMINDER_THREAD_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger8", "group8", trigger);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			} else {
		        Logger.info(DotInitScheduler.class, "Calendar Reminder Cron Thread schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting CalendarReminderJob Job");
				if ((job = sched.getJobDetail("CalendarReminderJob", "dotcms_jobs")) != null) {
					sched.deleteJob("CalendarReminderJob", "dotcms_jobs");
				}
			}

			//END Calendar Reminder Job



			if(UtilMethods.isSet(Config.getStringProperty("WEBDAV_CLEANUP_JOB_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("WebDavCleanupJob", "dotcms_jobs")) == null) {
							job = new JobDetail("WebDavCleanupJob", "dotcms_jobs", WebDavCleanupJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("WebDavCleanupJob", "dotcms_jobs");
						job = new JobDetail("WebDavCleanupJob", "dotcms_jobs", WebDavCleanupJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					trigger = new CronTrigger("trigger10", "group10", "WebDavCleanupJob", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("WEBDAV_CLEANUP_JOB_CRON_EXPRESSION"));
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
				if ((job = sched.getJobDetail("WebDavCleanupJob", "dotcms_jobs")) != null) {
					sched.deleteJob("WebDavCleanupJob", "dotcms_jobs");
				}
			}
			//http://jira.dotmarketing.net/browse/DOTCMS-1073
			if(UtilMethods.isSet(Config.getStringProperty("BINARY_CLEANUP_JOB_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("BinaryCleanupJob", "dotcms_jobs")) == null) {
							job = new JobDetail("BinaryCleanupJob", "dotcms_jobs", BinaryCleanupJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("BinaryCleanupJob", "dotcms_jobs");
						job = new JobDetail("BinaryCleanupJob", "dotcms_jobs", BinaryCleanupJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger11", "group11", "BinaryCleanupJob", "dotcms_jobs", calendar.getTime(), null,Config.getStringProperty("BINARY_CLEANUP_JOB_CRON_EXPRESSION"));
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
				if ((job = sched.getJobDetail("BinaryCleanupJob", "dotcms_jobs")) != null) {
					sched.deleteJob("BinaryCleanupJob", "dotcms_jobs");
				}
			}


			if(UtilMethods.isSet(Config.getStringProperty("TRASH_CLEANUP_JOB_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("TrashCleanupJob", "dotcms_jobs")) == null) {
							job = new JobDetail("TrashCleanupJob", "dotcms_jobs", TrashCleanupJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("TrashCleanupJob", "dotcms_jobs");
						job = new JobDetail("TrashCleanupJob", "dotcms_jobs", TrashCleanupJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger12", "group12", "TrashCleanupJob", "dotcms_jobs", calendar.getTime(), null,Config.getStringProperty("TRASH_CLEANUP_JOB_CRON_EXPRESSION"));
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
				if ((job = sched.getJobDetail("TrashCleanupJob", "dotcms_jobs")) != null) {
					sched.deleteJob("TrashCleanupJob", "dotcms_jobs");
				}
			}

			if(UtilMethods.isSet(Config.getStringProperty("DIST_REINDEX_JOURNAL_CLEANUP_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("ReindexJournalCleanupJob", "dotcms_jobs")) == null) {
							job = new JobDetail("ReindexJournalCleanupJob", "dotcms_jobs", DistReindexJournalCleanupThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("ReindexJournalCleanupJob", "dotcms_jobs");
						job = new JobDetail("ReindexJournalCleanupJob", "dotcms_jobs", DistReindexJournalCleanupThread.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger13", "group13", "ReindexJournalCleanupJob", "dotcms_jobs", calendar.getTime(), null,Config.getStringProperty("DIST_REINDEX_JOURNAL_CLEANUP_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger13", "group13", trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
		        Logger.info(DotInitScheduler.class, "ReindexJournalCleanupJob Cron Job schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting ReindexJournalCleanupJob Job");
				if ((job = sched.getJobDetail("ReindexJournalCleanupJob", "dotcms_jobs")) != null) {
					sched.deleteJob("ReindexJournalCleanupJob", "dotcms_jobs");
				}
			}

			if(UtilMethods.isSet(Config.getStringProperty("DIST_REINDEX_JOURNAL_CLEANUP_2_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("ReindexJournalCleanupJob2", "dotcms_jobs")) == null) {
							job = new JobDetail("ReindexJournalCleanupJob2", "dotcms_jobs", DistReindexJournalCleanupThread2.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("ReindexJournalCleanupJob2", "dotcms_jobs");
						job = new JobDetail("ReindexJournalCleanupJob2", "dotcms_jobs", DistReindexJournalCleanupThread2.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger14", "group14", "ReindexJournalCleanupJob2", "dotcms_jobs", calendar.getTime(), null,Config.getStringProperty("DIST_REINDEX_JOURNAL_CLEANUP_2_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger14", "group14", trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
		        Logger.info(DotInitScheduler.class, "ReindexJournalCleanupJob2 Cron Job schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting ReindexJournalCleanupJob2 Job");
				if ((job = sched.getJobDetail("ReindexJournalCleanupJob2", "dotcms_jobs")) != null) {
					sched.deleteJob("ReindexJournalCleanupJob2", "dotcms_jobs");
				}
			}

			if(UtilMethods.isSet(Config.getStringProperty("DASHBOARD_POPULATE_TABLES_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("DashboardJobImpl", "dotcms_jobs")) == null) {
							job = new JobDetail("DashboardJobImpl", "dotcms_jobs", DashboardProxy.getDashboardJobImplClass());
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("DashboardJobImpl", "dotcms_jobs");
						job = new JobDetail("DashboardJobImpl", "dotcms_jobs", DashboardProxy.getDashboardJobImplClass());
						isNew = true;
					}catch(IllegalArgumentException e){
						//Only enter here in case of "Job class must implement the Job interface." exception after version migration 
						job = new JobDetail("DashboardJobImpl", "dotcms_jobs", DashboardProxy.getDashboardJobImplClass());
						isNew = false;						
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger15", "group15", "DashboardJobImpl", "dotcms_jobs", calendar.getTime(), null,Config.getStringProperty("DASHBOARD_POPULATE_TABLES_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger15", "group15", trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
		        Logger.info(DotInitScheduler.class, "DashboardJobImpl Cron Job schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting DashboardJobImpl Job");
				if ((job = sched.getJobDetail("DashboardJobImpl", "dotcms_jobs")) != null) {
					sched.deleteJob("DashboardJobImpl", "dotcms_jobs");
				}
			}

			if(Config.getBooleanProperty("ENABLE_CREATE_CONTENT_FROM_EMAIL")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("ContentFromEmailJob", "dotcms_jobs")) == null) {
							job = new JobDetail("ContentFromEmailJob", "dotcms_jobs", ContentFromEmailJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("ContentFromEmailJob", "dotcms_jobs");
						job = new JobDetail("ContentFromEmailJob", "dotcms_jobs", ContentFromEmailJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND, Config.getIntProperty("CONTENT_FROM_EMAIL_INIT_DELAY"));
					trigger = new CronTrigger("trigger17", "group17", "ContentFromEmailJob", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("CONTENT_FROM_EMAIL_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger17", "group17", trigger);
				} catch (Exception e) {
					Logger.info(DotInitScheduler.class, e.toString());
				}
			} else {
		        Logger.info(DotInitScheduler.class, "Content From Email Job schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting ContentFromEmailJob Job");
				if ((job = sched.getJobDetail("ContentFromEmailJob", "dotcms_jobs")) != null) {
					sched.deleteJob("ContentFromEmailJob", "dotcms_jobs");
				}
			}

			if(Config.getBooleanProperty("ENABLE_DELETE_OLDER_CLICKSTREAMS", false)){
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("DeleteOldClickstreams", "dotcms_jobs")) == null) {
							job = new JobDetail("DeleteOldClickstreams", "dotcms_jobs", DeleteOldClickstreams.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("DeleteOldClickstreams", "dotcms_jobs");
						job = new JobDetail("DeleteOldClickstreams", "dotcms_jobs", DeleteOldClickstreams.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					trigger = new CronTrigger("trigger18", "group18", "DeleteOldClickstreams", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("DELETE_OLDER_CLICKSTREAMS_CRON_EXPRESSION"));
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
			if(Config.getBooleanProperty("ENABLE_PUBLISHER_QUEUE_THREAD")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("PublishQueueJob", "dotcms_jobs")) == null) {
							job = new JobDetail("PublishQueueJob", "dotcms_jobs", PublisherQueueJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("PublishQueueJob", "dotcms_jobs");
						job = new JobDetail("PublishQueueJob", "dotcms_jobs", PublisherQueueJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger19", "group19", "PublishQueueJob", "dotcms_jobs", calendar.getTime(), null,Config.getStringProperty("PUBLISHER_QUEUE_THREAD_CRON_EXPRESSION"));
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
				if ((job = sched.getJobDetail("PublishQueueJob", "dotcms_jobs")) != null) {
					sched.deleteJob("PublishQueueJob", "dotcms_jobs");
				}
			}


			final String lc="linkchecker";
            final String lg="dotcms_jobs";
			if(Config.getBooleanProperty("linkchecker.enablejob",true)) {
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

			if(Config.getBooleanProperty("org.dotcms.XMLSitemap.ENABLE",false)) {
                try {

                    isNew = false;

                    try {
                        if ((job = sched.getJobDetail("XMLSitemap", "dotcms_jobs")) == null) {
                            job = new JobDetail("XMLSitemap","dotcms_jobs", com.dotcms.xmlsitemap.XMLSitemapJob.class);
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob("XMLSitemap","dotcms_jobs");
                        job = new JobDetail("XMLSitemap","dotcms_jobs", com.dotcms.xmlsitemap.XMLSitemapJob.class);
                        isNew = true;
                    }
                    calendar = GregorianCalendar.getInstance();
                    trigger = new CronTrigger("trigger21", "group21", "XMLSitemap","dotcms_jobs", calendar.getTime(),
                                  null,Config.getStringProperty("org.dotcms.XMLSitemap.CRON_EXPRESSION","1 1 1 * * ?"));
                    trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
                    sched.addJob(job, true);

                    if (isNew)
                        sched.scheduleJob(trigger);
                    else {
                        CronTrigger existing=(CronTrigger)sched.getTrigger("trigger21", "group21");
                        if(!existing.getCronExpression().equals(trigger.getCronExpression())) {
                            sched.rescheduleJob("trigger21", "group21", trigger);
                        }
                    }
                } catch (Exception e) {
                    Logger.error(DotInitScheduler.class, e.getMessage(),e);
                }
            } else {
                Logger.info(DotInitScheduler.class, "XMLSitemapJob Cron Job schedule disabled on this server");
                if ((job = sched.getJobDetail("XMLSitemap", "dotcms_jobs")) != null) {
                    sched.deleteJob("XMLSitemap", "dotcms_jobs");
                }
            }

            /*
              SCHEDULE SERVER HEARTBEAT JOB
              For this JOB we will use a local Scheduler in order to store the scheduling information within memory.
             */
            if ( Config.getBooleanProperty( "ENABLE_SERVER_HEARTBEAT", true ) ) {

                Scheduler localScheduler = QuartzUtils.getLocalScheduler();
                String jobName = "ServerHeartbeatJob";
                String jobGroup = "dotcms_jobs";
                String triggerName = "trigger22";
                String triggerGroup = "group22";

                try {
                    //Job detail
                    job = new JobDetail( jobName, jobGroup, ServerHeartbeatJob.class );
                    calendar = GregorianCalendar.getInstance();
                    //Trigger
                    trigger = new CronTrigger( triggerName, triggerGroup, jobName, jobGroup, calendar.getTime(), null, Config.getStringProperty( "HEARTBEAT_CRON_EXPRESSION" ) );
                    trigger.setMisfireInstruction( CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW );

                    //Schedule the Job
                    localScheduler.addJob( job, true );
                    localScheduler.scheduleJob( trigger );

                    //Starting the local quartz Scheduler
                    QuartzUtils.startLocalScheduler();
                } catch ( Exception e ) {
                    Logger.error( DotInitScheduler.class, e.getMessage(), e );
                }
            }

			//SCHEDULE REMOVE INACTIVE CLUSTER SERVERS JOB
            String jobName = "RemoveInactiveClusterServerJob";
            String jobGroup = "dotcms_jobs";
            String triggerName = "trigger23";
            String triggerGroup = "group23";
            if(Config.getBooleanProperty("ENABLE_REMOVE_INACTIVE_CLUSTER_SERVER", true)) {
				try {
					isNew = false;
					
					try {
						if ((job = sched.getJobDetail(jobName, jobGroup)) == null) {
							job = new JobDetail(jobName, jobGroup, DeleteInactiveClusterServersJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob(jobName, jobGroup);
						job = new JobDetail(jobName, jobGroup, DeleteInactiveClusterServersJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger(triggerName, triggerGroup, jobName, jobGroup, calendar.getTime(), null,Config.getStringProperty("REMOVE_INACTIVE_CLUSTER_SERVER_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob(triggerName, triggerGroup, trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
				if ((job = sched.getJobDetail(jobName, jobGroup)) != null) {
					sched.deleteJob(jobName, jobGroup);
				}
			}

			//SCHEDULE ESCALATION THREAD JOB
			String ETjobName = "EscalationThreadJob";
			String ETjobGroup = "dotcms_jobs";
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
			String FSCobGroup = "dotcms_jobs";
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
			
			// Enabling the System Events Job
			if (Config.getBooleanProperty("ENABLE_SYSTEM_EVENTS", true)) {
				JobBuilder systemEventsJob = new JobBuilder().setJobClass(SystemEventsJob.class)
						.setJobName("SystemEventsJob")
						.setJobGroup(DOTCMS_JOB_GROUP_NAME)
						.setTriggerName("trigger26")
						.setTriggerGroup("group26")
						.setCronExpressionProp("SYSTEM_EVENTS_CRON_EXPRESSION")
						.setCronExpressionPropDefault("0/5 * * * * ?")
						.setCronMissfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
				scheduleJob(systemEventsJob);
			}
			// Enabling the Delete Old System Events Job
			if (Config.getBooleanProperty("ENABLE_DELETE_OLD_SYSTEM_EVENTS", true)) {
				JobBuilder deleteOldSystemEventsJob = new JobBuilder().setJobClass(DeleteOldSystemEventsJob.class)
						.setJobName("DeleteOldSystemEventsJob")
						.setJobGroup(DOTCMS_JOB_GROUP_NAME)
						.setTriggerName("trigger27")
						.setTriggerGroup("group27")
						.setCronExpressionProp("DELETE_OLD_SYSTEM_EVENTS_CRON_EXPRESSION")
						.setCronExpressionPropDefault("0 0 0 1/3 * ? *")
						.setCronMissfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
				scheduleJob(deleteOldSystemEventsJob);
			}
			
            //Starting the sequential and standard Schedulers
	        QuartzUtils.startSchedulers();
		} catch (SchedulerException e) {
			Logger.fatal(DotInitScheduler.class, "An error as ocurred scheduling critical startup task of dotCMS, the system will shutdown immediately, " + e.toString(), e);
			throw e;
		}
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
			Scheduler sched = QuartzUtils.getStandardScheduler();
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
