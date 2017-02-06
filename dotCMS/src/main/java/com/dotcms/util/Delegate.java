package com.dotcms.util;


/**
 * <p>
 *     A delegate class is basically a class that executes a specific task, it is usually used to decouple some generic task from the specific one (the delegate will be the specialization in the routine).
 * </p>
 * <p>
 * So for instance you may have a Delegate to perform a Quartz Job receiving an specific information from the Quartz. A  Delegate is a class that can be registered to a Quartz Job. Delegate
 * classes will receive basic information from such a Job that will allow them to perform any custom action.
 * For example, a delegate can connect to a running service and notify a
 * component that one or more new events have occurred. This way, other services
 * and even UI components can react to the new information and provide a useful
 * output to the user.
 * </p>
 *
 * <p>
 * You can also used a Delegate to perform a task in a thread or anything other generic algorithm routine that needs some specific and unknown part.
 * </p>
 *
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 13, 2016
 *
 */
public interface Delegate<T> {

	/**
	 * This method will be called by the parent routine, delegating the specific job.
	 * 
	 * @param data
	 *            - The required information sent by the parent routine.
	 */
	public void execute(T data);

} // E:O:F:Delegate.
