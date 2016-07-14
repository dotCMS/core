package com.dotcms.web.websocket.delegate.bean;

import java.io.Serializable;

import org.quartz.JobExecutionContext;

/**
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Jul 13, 2016
 *
 */
@SuppressWarnings("serial")
public class JobDelegateDataBean implements Serializable {

	private final JobExecutionContext jobContext;
	private final long lastCallback;

	/**
	 * 
	 * @param jobContext
	 * @param lastCallback
	 */
	public JobDelegateDataBean(JobExecutionContext jobContext, long lastCallback) {
		this.jobContext = jobContext;
		this.lastCallback = lastCallback;
	}

	/**
	 * 
	 * @return
	 */
	public JobExecutionContext getJobContext() {
		return jobContext;
	}

	/**
	 * 
	 * @return
	 */
	public long getLastCallback() {
		return lastCallback;
	}

	@Override
	public String toString() {
		return "JobDelegateBean [jobContext=" + jobContext + ", lastCallback=" + lastCallback + "]";
	}

}
