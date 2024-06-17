package com.dotmarketing.quartz;

import java.io.Serializable;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.triggers.SimpleTriggerImpl;

public class NowScheduledTask extends ScheduledTask implements Serializable {

	private static final long serialVersionUID = 1L;

	final String jobName;
	final Job quartzJob;
	final Map<String, Object> properties;

	public NowScheduledTask(String jobName, Job quartzJob, Map<String, Object> properties) {
		super();
		this.jobName = jobName;
		this.quartzJob = quartzJob;
		this.properties = properties;
	}

	public void run() throws SchedulerException {
		// Create the JobDetail
		JobDetail jobDetail = JobBuilder.newJob(quartzJob.getClass())
				.withIdentity(this.jobName, Scheduler.DEFAULT_GROUP)
				.usingJobData(new JobDataMap(this.properties))
				.build();

		// Create a Trigger that will fire immediately
		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(jobName + "-" + System.currentTimeMillis(), Scheduler.DEFAULT_GROUP)
				.startNow()
				.build();

		DotSchedulerFactory.getInstance().getScheduler().scheduleJob(jobDetail, trigger);
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}
}