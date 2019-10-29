package com.dotmarketing.init;

import static com.dotmarketing.util.WebKeys.DOTCMS_DISABLE_WEBSOCKET_PROTOCOL;

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
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.BinaryCleanupJob;
import com.dotmarketing.quartz.job.CalendarReminderThread;
import com.dotmarketing.quartz.job.CleanUnDeletedUsersJob;
import com.dotmarketing.quartz.job.ContentFromEmailJob;
import com.dotmarketing.quartz.job.ContentReindexerThread;
import com.dotmarketing.quartz.job.ContentReviewThread;
import com.dotmarketing.quartz.job.DeleteOldClickstreams;
import com.dotmarketing.quartz.job.FreeServerFromClusterJob;
import com.dotmarketing.quartz.job.ServerHeartbeatJob;
import com.dotmarketing.quartz.job.TrashCleanupJob;
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
			Scheduler sched = QuartzUtils.getStandardScheduler();
			JobDetail job;
			CronTrigger trigger;
			Calendar calendar;
			boolean isNew;

		        if(Config.getBooleanProperty("ENABLE_CONTENT_REVIEW_THREAD")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("ContentReviewJob", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("ContentReviewJob", DOTCMS_JOB_GROUP_NAME, ContentReviewThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("ContentReviewJob", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("ContentReviewJob", DOTCMS_JOB_GROUP_NAME, ContentReviewThread.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND, Config.getIntProperty("EXEC_INIT_DELAY"));
					trigger = new CronTrigger("trigger3", "group3", "ContentReviewJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null, Config.getStringProperty("CONTENT_REVIEW_THREAD_CRON_EXPRESSION"));
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
				if ((job = sched.getJobDetail("ContentReviewJob", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("ContentReviewJob", DOTCMS_JOB_GROUP_NAME);
				}
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

			//Calendar Reminder Job

			if(Config.getBooleanProperty("CALENDAR_REMINDER_THREAD")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("CalendarReminderJob", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("CalendarReminderJob", DOTCMS_JOB_GROUP_NAME, CalendarReminderThread.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("CalendarReminderJob", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("CalendarReminderJob", DOTCMS_JOB_GROUP_NAME, CalendarReminderThread.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND,Config.getIntProperty("CALENDAR_REMINDER_THREAD_INIT_DELAY"));
					trigger = new CronTrigger("trigger8", "group8", "CalendarReminderJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null, Config.getStringProperty("CALENDAR_REMINDER_THREAD_CRON_EXPRESSION"));
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
				if ((job = sched.getJobDetail("CalendarReminderJob", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("CalendarReminderJob", DOTCMS_JOB_GROUP_NAME);
				}
			}

			//END Calendar Reminder Job



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



			if(UtilMethods.isSet(Config.getStringProperty("DASHBOARD_POPULATE_TABLES_CRON_EXPRESSION"))) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("DashboardJobImpl", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("DashboardJobImpl", DOTCMS_JOB_GROUP_NAME, DashboardProxy.getDashboardJobImplClass());
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("DashboardJobImpl", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("DashboardJobImpl", DOTCMS_JOB_GROUP_NAME, DashboardProxy.getDashboardJobImplClass());
						isNew = true;
					}catch(IllegalArgumentException e){
						//Only enter here in case of "Job class must implement the Job interface." exception after version migration
						job = new JobDetail("DashboardJobImpl", DOTCMS_JOB_GROUP_NAME, DashboardProxy.getDashboardJobImplClass());
						isNew = false;
					}
					calendar = GregorianCalendar.getInstance();
				    trigger = new CronTrigger("trigger15", "group15", "DashboardJobImpl", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null,Config.getStringProperty("DASHBOARD_POPULATE_TABLES_CRON_EXPRESSION"));
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
				if ((job = sched.getJobDetail("DashboardJobImpl", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("DashboardJobImpl", DOTCMS_JOB_GROUP_NAME);
				}
			}

			if(Config.getBooleanProperty("ENABLE_CREATE_CONTENT_FROM_EMAIL")) {
				try {
					isNew = false;

					try {
						if ((job = sched.getJobDetail("ContentFromEmailJob", DOTCMS_JOB_GROUP_NAME)) == null) {
							job = new JobDetail("ContentFromEmailJob", DOTCMS_JOB_GROUP_NAME, ContentFromEmailJob.class);
							isNew = true;
						}
					} catch (SchedulerException se) {
						sched.deleteJob("ContentFromEmailJob", DOTCMS_JOB_GROUP_NAME);
						job = new JobDetail("ContentFromEmailJob", DOTCMS_JOB_GROUP_NAME, ContentFromEmailJob.class);
						isNew = true;
					}
					calendar = GregorianCalendar.getInstance();
					calendar.add(Calendar.SECOND, Config.getIntProperty("CONTENT_FROM_EMAIL_INIT_DELAY"));
					trigger = new CronTrigger("trigger17", "group17", "ContentFromEmailJob", DOTCMS_JOB_GROUP_NAME, calendar.getTime(), null, Config.getStringProperty("CONTENT_FROM_EMAIL_CRON_EXPRESSION"));
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
				if ((job = sched.getJobDetail("ContentFromEmailJob", DOTCMS_JOB_GROUP_NAME)) != null) {
					sched.deleteJob("ContentFromEmailJob", DOTCMS_JOB_GROUP_NAME);
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
			if(Config.getBooleanProperty("ENABLE_PUBLISHER_QUEUE_THREAD")) {
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
                        if ((job = sched.getJobDetail("XMLSitemap", DOTCMS_JOB_GROUP_NAME)) == null) {
                            job = new JobDetail("XMLSitemap",DOTCMS_JOB_GROUP_NAME, com.dotcms.xmlsitemap.XMLSitemapJob.class);
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        sched.deleteJob("XMLSitemap",DOTCMS_JOB_GROUP_NAME);
                        job = new JobDetail("XMLSitemap",DOTCMS_JOB_GROUP_NAME, com.dotcms.xmlsitemap.XMLSitemapJob.class);
                        isNew = true;
                    }
                    calendar = GregorianCalendar.getInstance();
                    trigger = new CronTrigger("trigger21", "group21", "XMLSitemap",DOTCMS_JOB_GROUP_NAME, calendar.getTime(),
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
                if ((job = sched.getJobDetail("XMLSitemap", DOTCMS_JOB_GROUP_NAME)) != null) {
                    sched.deleteJob("XMLSitemap", DOTCMS_JOB_GROUP_NAME);
                }
            }

            /*
              SCHEDULE SERVER HEARTBEAT JOB
              For this JOB we will use a local Scheduler in order to store the scheduling information within memory.
             */
            if ( Config.getBooleanProperty( "ENABLE_SERVER_HEARTBEAT", true ) ) {

                Scheduler localScheduler = QuartzUtils.getLocalScheduler();
                String jobName = "ServerHeartbeatJob";
                String jobGroup = DOTCMS_JOB_GROUP_NAME;
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
