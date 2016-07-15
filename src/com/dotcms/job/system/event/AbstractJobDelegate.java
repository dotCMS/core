package com.dotcms.job.system.event;

import java.io.Serializable;

import com.dotcms.job.JobDelegate;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

/**
 * Base class for Quartz Job Delegate classes. This base class is useful
 * specially to abstract aspects of the delegates functionality.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Jul 14, 2016
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractJobDelegate implements JobDelegate<JobDelegateDataBean>, Serializable {

	@Override
	public void execute(JobDelegateDataBean data) {
		try {
			executeDelegate(data);
		} catch (Exception e) {
			Logger.error(this, "An error occurred when running the Job Delegate: " + this.getClass(), e);
		} finally {
			// The main reason for this abstraction is to ensure that the
			// database connection is released and closed after executing this
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.warn(this, e.getMessage(), e);
			} finally {
				DbConnectionFactory.closeConnection();
			}
		}
	}

	/**
	 * The main entry point to the execution of this delegate class.
	 * 
	 * @param data
	 *            - The bean containing all the necessary information for the
	 *            delegate to run.
	 * @throws Exception
	 *             An error occurred when executing the delegate.
	 */
	protected abstract void executeDelegate(JobDelegateDataBean data) throws Exception;

}
