package com.dotmarketing.quartz;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.quartz.Trigger;

/**
 * 
 * This is utility class to wrap a quartz job and its configured trigger, so if you have a quartz job that is 
 * scheduled multiple times with multiple trigger you can end up with multiple instances of this object
 *   
 * @author David
 *
 */
public abstract class ScheduledTask implements Serializable {

    private static final long serialVersionUID = 1L;

    //Job Properties
    private String jobName;
    private String jobGroup;
    private String jobDescription;
    private String javaClass;
    private boolean isVolatile;
    private boolean durability;
    
    //Trigger properties
    private String triggerName;
    private String triggerGroup;
    private Date startDate;
    private Date endDate;
    private int misfireInstruction = Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;
    private int priority = 5;
    
    //dotCMS scheduler
    private boolean sequentialScheduled = false;

    private Map<String, Object> properties;
    
	public ScheduledTask(String jobName, String jobGroup, String jobDescription, String javaClass, boolean isVolatile, String triggerName, String triggerGroup,
			Date startDate, Date endDate, int misfireInstruction, int priority, boolean sequentialScheduled, Map<String, Object> properties) {
		super();
		this.jobName = jobName;
		this.jobGroup = jobGroup;
		this.jobDescription = jobDescription;
		this.javaClass = javaClass;
		this.isVolatile = isVolatile;
		this.triggerName = triggerName;
		this.triggerGroup = triggerGroup;
		this.startDate = startDate;
		this.endDate = endDate;
		this.misfireInstruction = misfireInstruction;
		this.priority = priority;
		this.sequentialScheduled = sequentialScheduled;
		this.properties = properties;
	}

	
	public ScheduledTask(String jobName, String jobGroup, String jobDescription, String javaClass, Date startDate, Date endDate, int misfireInstruction,
			Map<String, Object> properties) {
		super();
		this.jobName = jobName;
		this.jobGroup = jobGroup;
		this.jobDescription = jobDescription;
		this.javaClass = javaClass;
		this.startDate = startDate;
		this.endDate = endDate;
		this.misfireInstruction = misfireInstruction;
		this.properties = properties;
	}

	public ScheduledTask () {
		
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}
	
	public Date getEndDate() {
		return endDate;
	}
	
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	public String getJavaClassName() {
		return javaClass;
	}
	
	public void setJavaClassName(String javaClass) {
		this.javaClass = javaClass;
	}
	
	public String getJobDescription() {
		return jobDescription;
	}
	
	public void setJobDescription(String jobDescription) {
		this.jobDescription = jobDescription;
	}
	
	public String getJobName() {
		return jobName;
	}
	
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public String getJobGroup() {
		return jobGroup;
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}
	
	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public void setMisfireInstruction(int misfireInstruction) {
		this.misfireInstruction = misfireInstruction;
	}

	public int getMisfireInstruction() {
		return misfireInstruction;
	}

	public void setVolatile(boolean isVolatile) {
		this.isVolatile = isVolatile;
	}

	public boolean isVolatile() {
		return isVolatile;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}

	public void setTriggerName(String triggerName) {
		this.triggerName = triggerName;
	}

	public String getTriggerName() {
		return triggerName;
	}

	public void setTriggerGroup(String triggerGroup) {
		this.triggerGroup = triggerGroup;
	}

	public String getTriggerGroup() {
		return triggerGroup;
	}

	public void setSequentialScheduled(boolean sequentialScheduled) {
		this.sequentialScheduled = sequentialScheduled;
	}

	public boolean isSequentialScheduled() {
		return sequentialScheduled;
	}

	public void setDurability(boolean durability) {
		this.durability = durability;
	}

	public boolean getDurability() {
		return durability;
	}

}