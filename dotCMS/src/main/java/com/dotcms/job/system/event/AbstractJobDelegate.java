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
 * <p>
 * A Job Delegate is a class that can be registered to one or more Quartz Jobs.
 * Delegate classes will receive basic information from them that will allow
 * them to perform any custom action upon its scheduled call. For example, a
 * delegate can connect to a running service and notify a component that a Job
 * has been triggered.
 * 
 * @author Jose Castro
 * @version 3.7
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
