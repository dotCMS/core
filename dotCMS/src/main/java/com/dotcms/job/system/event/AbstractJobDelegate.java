package com.dotcms.job.system.event;

import com.dotcms.business.CloseDBIfOpened;
import java.io.Serializable;

import com.dotcms.util.Delegate;
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
public abstract class AbstractJobDelegate implements Delegate<JobDelegateDataBean>, Serializable {

	@Override
	@CloseDBIfOpened
	public void execute(JobDelegateDataBean data) {
		try {
			executeDelegate(data);
		} catch (Exception e) {
			Logger.error(this, "An error occurred when running the Job Delegate: " + this.getClass(), e);
		}
	}

	/**
	 * Runs the delegate and lets a failure propagate, instead of logging and swallowing it as
	 * {@link #execute(JobDelegateDataBean)} does.
	 *
	 * <p>Callers that record progress after a delegate runs need this: with {@code execute} the
	 * caller cannot tell a successful run from a failed one, so it commits progress over work that
	 * never happened. {@code SystemEventsJob} persists its delivery cursor after the delegates run,
	 * and used {@code execute}, so a failing read still advanced the cursor and permanently stranded
	 * every event in the skipped span (issue #36827).
	 *
	 * <p>{@code execute} keeps its swallowing behaviour for the jobs that rely on it — a failed run
	 * there has nothing to corrupt.
	 *
	 * @param data the bean containing the information the delegate needs
	 * @throws Exception whatever the delegate threw
	 */
	@CloseDBIfOpened
	public void executeOrThrow(final JobDelegateDataBean data) throws Exception {
		executeDelegate(data);
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
