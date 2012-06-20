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
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import com.dotcms.enterprise.DashboardProxy;
import com.dotmarketing.business.cluster.mbeans.Cluster;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.BinaryCleanupJob;
import com.dotmarketing.quartz.job.CalendarReminderThread;
import com.dotmarketing.quartz.job.CleanBlockCacheScheduledTask;
import com.dotmarketing.quartz.job.ContentFromEmailJob;
import com.dotmarketing.quartz.job.ContentReindexerThread;
import com.dotmarketing.quartz.job.ContentReviewThread;
import com.dotmarketing.quartz.job.DeleteOldClickstreams;
import com.dotmarketing.quartz.job.DeliverCampaignThread;
import com.dotmarketing.quartz.job.DistReindexJournalCleanupThread;
import com.dotmarketing.quartz.job.DistReindexJournalCleanupThread2;
import com.dotmarketing.quartz.job.PopBouncedMailThread;
import com.dotmarketing.quartz.job.TrashCleanupJob;
import com.dotmarketing.quartz.job.UpdateRatingThread;
import com.dotmarketing.quartz.job.UsersToDeleteThread;
import com.dotmarketing.quartz.job.WebDavCleanupJob;
import com.dotmarketing.servlets.InitServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 *
 * Initializes all dotCMS startup jobs
 * @author David H Torres
 *
 */
public class DotInitScheduler {

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
						sched.rescheduleJob("trigger11", "group8", trigger);
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

			if(UtilMethods.isSet(Config.getStringProperty("CLEAN_BLOCK_CACHE_JOB_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("CleanBlockCacheScheduledTask", "dotcms_jobs")) == null) {
							job = new JobDetail("CleanBlockCacheScheduledTask", "dotcms_jobs", CleanBlockCacheScheduledTask.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("CleanBlockCacheScheduledTask", "dotcms_jobs");
						job = new JobDetail("CleanBlockCacheScheduledTask", "dotcms_jobs", CleanBlockCacheScheduledTask.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger16", "group16", "CleanBlockCacheScheduledTask", "dotcms_jobs", calendar.getTime(), null, Config.getStringProperty("CLEAN_BLOCK_CACHE_JOB_CRON_EXPRESSION"));
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
					sched.addJob(job, true);

					if (isNew)
						sched.scheduleJob(trigger);
					else
						sched.rescheduleJob("trigger16", "group16", trigger);
				} catch (Exception e) {
					Logger.error(DotInitScheduler.class, e.getMessage(),e);
				}
			} else {
		        Logger.info(DotInitScheduler.class, "CleanBlockCacheScheduledTask Cron Job schedule disabled on this server");
		        Logger.info(DotInitScheduler.class, "Deleting CleanBlockCacheScheduledTask Job");
				if ((job = sched.getJobDetail("CleanBlockCacheScheduledTask", "dotcms_jobs")) != null) {
					sched.deleteJob("CleanBlockCacheScheduledTask", "dotcms_jobs");
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

	        QuartzUtils.startSchedulers();

		} catch (SchedulerException e) {
			Logger.fatal(DotInitScheduler.class, "An error as ocurred scheduling critical startup task of dotCMS, the system will shutdown immediately, " + e.toString(), e);
			throw e;
		}
	}



}