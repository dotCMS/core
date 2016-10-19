package com.dotcms.job;


/**
 * A Job Delegate is a class that can be registered to a Quartz Job. Delegate
 * classes will receive basic information from such a Job that will allow them
 * to perform any custom action.
 * <p>
 * For example, a delegate can connect to a running service and notify a
 * component that one or more new events have occurred. This way, other services
 * and even UI components can react to the new information and provide a useful
 * output to the user.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 13, 2016
 *
 */
public interface JobDelegate<T> {

	/**
	 * Triggers the main routine of this delegate.
	 * 
	 * @param data
	 *            - The required information sent by the Quartz Job.
	 */
	public void execute(T data);

}
