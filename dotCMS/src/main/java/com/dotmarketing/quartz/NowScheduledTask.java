package com.dotmarketing.quartz;

import java.io.Serializable;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

public class NowScheduledTask extends ScheduledTask implements Serializable {

    private static final long serialVersionUID = 1L;

    final String jobName;
    final Job quartzJob;
    final Map<String, Object> properties;

	public NowScheduledTask(String jobName, Job quartzJob, Map<String, Object> properties)  {
		super();
		this.jobName=jobName;
		this.quartzJob=quartzJob;
		this.properties=properties;

	}

	public void run() throws SchedulerException{
		// Create the JobDetail 
		JobDetail jobDetail = new JobDetail(this.jobName, Scheduler.DEFAULT_GROUP, quartzJob.getClass());  
		jobDetail.setJobDataMap(new JobDataMap(this.properties));
		Trigger trigger = TriggerUtils.makeImmediateTrigger(0, 0);   
		trigger.setName(jobName+"-"+ System.currentTimeMillis());  
		DotSchedulerFactory.getInstance().getScheduler().scheduleJob(jobDetail, trigger);
	}


	public static long getSerialVersionUID() {
		return serialVersionUID;
	}
	


}