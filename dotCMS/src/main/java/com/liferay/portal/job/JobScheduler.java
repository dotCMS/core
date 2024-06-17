package com.liferay.portal.job;

import java.util.Date;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import com.dotmarketing.util.Logger;
import com.liferay.util.Time;

/**
 * <a href="JobScheduler.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun
 * @version $Revision: 1.12 $
 *
 */
public class JobScheduler {

	public static void schedule(IntervalJob intervalJob) throws SchedulerException {
		Date startTime = new Date(System.currentTimeMillis() + Time.MINUTE * 3);

		JobDetail jobDetail = JobBuilder.newJob(intervalJob.getClass())
				.withIdentity(intervalJob.getClass().getName(), Scheduler.DEFAULT_GROUP)
				.build();

		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(intervalJob.getClass().getName() + "_TRIGGER", Scheduler.DEFAULT_GROUP)
				.startAt(startTime)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule()
						.withIntervalInMilliseconds(intervalJob.getInterval())
						.repeatForever())
				.build();

		scheduleJob(jobDetail, trigger);
	}

	public static void scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
		getScheduler().scheduleJob(jobDetail, trigger);
	}

	public static void scheduleJob(Trigger trigger) throws SchedulerException {
		getScheduler().scheduleJob(trigger);
	}

	public static void shutdown() {
		getInstance().shutdownScheduler();
	}

	public static void triggerJob(String jobName, String groupName) throws SchedulerException {
		getScheduler().triggerJob(JobKey.jobKey(jobName, groupName));
	}

	public static void unscheduleJob(String triggerName, String groupName) throws SchedulerException {
		getScheduler().unscheduleJob(TriggerKey.triggerKey(triggerName, groupName));
	}

	private static JobScheduler getInstance() {
		if (instance == null) {
			synchronized (JobScheduler.class) {
				if (instance == null) {
					instance = new JobScheduler();
				}
			}
		}
		return instance;
	}

	private static Scheduler getScheduler() {
		return getInstance().scheduler;
	}

	private JobScheduler() {
		startScheduler();
	}

	private void startScheduler() {
		StdSchedulerFactory sf = new StdSchedulerFactory();
		try {
			scheduler = sf.getScheduler();
			scheduler.start();
		} catch (SchedulerException se) {
			Logger.error(this, se.getMessage(), se);
		}
	}

	private void shutdownScheduler() {
		try {
			if (!scheduler.isShutdown()) {
				scheduler.shutdown();
			}
		} catch (SchedulerException se) {
			Logger.error(this, se.getMessage(), se);
		}
	}

	private static JobScheduler instance;

	private Scheduler scheduler;
}