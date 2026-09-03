package com.dotcms.job.system.event.delegate.bean;

import java.io.Serializable;

import org.quartz.JobExecutionContext;

import com.dotcms.job.system.event.SystemEventsCursorTracker;
import com.dotcms.util.Delegate;

/**
 * Contains the basic information required for a {@link Delegate} to perform
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
	/** Not serializable, and only meaningful within the running JVM that created it. */
	private final transient SystemEventsCursorTracker cursorTracker;

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
		this(jobContext, lastCallback, null);
	}

	/**
	 * Creates an instance carrying the delivery cursor tracker, so the delegate can suppress the
	 * repeat deliveries the overlap window necessarily produces.
	 *
	 * @param jobContext
	 *            - The {@link JobExecutionContext} of a Quartz Job.
	 * @param lastCallback
	 *            - The date/time in milliseconds from which the Job requested new information.
	 * @param cursorTracker
	 *            - The {@link SystemEventsCursorTracker} of the running poller, or {@code null} for
	 *            jobs that do not consume the queue.
	 */
	public JobDelegateDataBean(JobExecutionContext jobContext, long lastCallback,
			SystemEventsCursorTracker cursorTracker) {
		this.jobContext = jobContext;
		this.lastCallback = lastCallback;
		this.cursorTracker = cursorTracker;
	}

	/**
	 * Returns the delivery cursor tracker of the running poller, if one was supplied.
	 *
	 * @return The {@link SystemEventsCursorTracker}, or {@code null}.
	 */
	public SystemEventsCursorTracker getCursorTracker() {
		return cursorTracker;
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
