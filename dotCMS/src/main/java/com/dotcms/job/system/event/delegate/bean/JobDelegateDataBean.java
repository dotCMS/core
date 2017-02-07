package com.dotcms.job.system.event.delegate.bean;

import java.io.Serializable;

import org.quartz.JobExecutionContext;

import com.dotcms.job.JobDelegate;

/**
 * Contains the basic information required for a {@link JobDelegate} to perform
 * its purpose. The Quartz Job will include as much useful data as possible in
 * order to send it to the respective delegate and assist it.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 13, 2016
 *
 */
@SuppressWarnings("serial")
public class JobDelegateDataBean implements Serializable {

	private final JobExecutionContext jobContext;
	private final long lastCallback;

	/**
	 * Creates an instance of a {@code JobDelegateDataBean} class.
	 * 
	 * @param jobContext
	 *            - The {@link JobExecutionContext} of a Quartz Job.
	 * @param lastCallback
	 *            - The date/time in milliseconds corresponding to the last time
	 *            that the Job requested for new information.
	 */
	public JobDelegateDataBean(JobExecutionContext jobContext, long lastCallback) {
		this.jobContext = jobContext;
		this.lastCallback = lastCallback;
	}

	/**
	 * Returns the execution context of the Job that called this delegate.
	 * 
	 * @return The {@link JobExecutionContext}.
	 */
	public JobExecutionContext getJobContext() {
		return jobContext;
	}

	/**
	 * Returns the date/time in milliseconds of the last time that the Job
	 * requested for new data.
	 * 
	 * @return That date/time of the last request.
	 */
	public long getLastCallback() {
		return lastCallback;
	}

	@Override
	public String toString() {
		return "JobDelegateBean [jobContext=" + jobContext + ", lastCallback=" + lastCallback + "]";
	}

}
