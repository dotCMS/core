package com.dotmarketing.quartz;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.quartz.SimpleTrigger;

public class SimpleScheduledTask extends ScheduledTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private int repeatCount = SimpleTrigger.REPEAT_INDEFINITELY;
    private long repeatInterval = 0;

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}
	
	
	public SimpleScheduledTask() {
		super();
	}


	public SimpleScheduledTask(String jobName, String jobGroup, String jobDescription, String javaClass,
			boolean isVolatile, String triggerName, String triggerGroup, Date startDate, Date endDate,
			int misfireInstruction, int priority, boolean sequentialScheduled, Map<String, Object> properties,
			int repeatCount, long repeatInterval) {
		super(jobName, jobGroup, jobDescription, javaClass, isVolatile, triggerName, triggerGroup, startDate, endDate,
				misfireInstruction, priority, sequentialScheduled, properties);
		this.repeatCount = repeatCount;
		this.repeatInterval = repeatInterval;
	}


	public SimpleScheduledTask(String jobName, String jobGroup, String jobDescription, String javaClass,
			Date startDate, Date endDate, int misfireInstruction, Map<String, Object> properties,
			int repeatCount, long repeatInterval) {
		super(jobName, jobGroup, jobDescription, javaClass, startDate, endDate, misfireInstruction, properties);
		this.repeatCount = repeatCount;
		this.repeatInterval = repeatInterval;
	}


	public void setRepeatCount(int repeatCount) {
		this.repeatCount = repeatCount;
	}

	public int getRepeatCount() {
		return repeatCount;
	}

	public void setRepeatInterval(long repeatInterval) {
		this.repeatInterval = repeatInterval;
	}

	public long getRepeatInterval() {
		return repeatInterval;
	}
}