package com.dotmarketing.quartz;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class CronScheduledTask extends ScheduledTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cronExpression;

    
	public CronScheduledTask() {
		super();
	}

    @Deprecated
	public CronScheduledTask(String jobName, String jobGroup, String jobDescription, String javaClass, boolean isVolatile, String triggerName,
			String triggerGroup, Date startDate, Date endDate, int misfireInstruction, int priority, boolean sequentialScheduled,
			Map<String, Object> properties, String cronExpression) {
		super(jobName, jobGroup, jobDescription, javaClass, isVolatile, triggerName, triggerGroup, startDate, endDate, misfireInstruction, priority,
				sequentialScheduled, properties);
		this.cronExpression = cronExpression;
	}

	/**
	 * Build a CronScheduledTask
	 * We no longer use the sequential flag
	 * @param jobName
	 * @param jobGroup
	 * @param jobDescription
	 * @param javaClass
	 * @param isVolatile
	 * @param triggerName
	 * @param triggerGroup
	 * @param startDate
	 * @param endDate
	 * @param misfireInstruction
	 * @param priority
	 * @param properties
	 * @param cronExpression
	 */
	public CronScheduledTask(final String jobName, final String jobGroup, final String jobDescription, final String javaClass, final boolean isVolatile, final String triggerName,
			final String triggerGroup, final Date startDate, final Date endDate, final int misfireInstruction, final int priority,
			final Map<String, Object> properties, final String cronExpression) {
		super(jobName, jobGroup, jobDescription, javaClass, isVolatile, triggerName, triggerGroup, startDate, endDate, misfireInstruction, priority,
				false, properties);
		this.cronExpression = cronExpression;
	}

	public CronScheduledTask(String jobName, String jobGroup, String jobDescription, String javaClass, Date startDate, Date endDate, int misfireInstruction,
			Map<String, Object> properties, String cronExpression) {
		super(jobName, jobGroup, jobDescription, javaClass, startDate, endDate, misfireInstruction, properties);
		this.cronExpression = cronExpression;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}
	
	public String getCronExpression() {
		return cronExpression;
	}
	
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

}