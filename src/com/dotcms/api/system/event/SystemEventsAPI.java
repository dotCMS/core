package com.dotcms.api.system.event;

import java.util.Collection;

import com.dotmarketing.exception.DotDataException;

/**
 * Allows users and other services to record and retrieve different types of
 * events that dotCMS and custom services (specially UI components) can react
 * to. For example, system events can be:
 * <ul>
 * <li>An entry in the Notifications component.</li>
 * <li>A contentlet that has been added to or deleted from the system which will
 * display a message on screen.</li>
 * <li>A message indicating that the enterprise license is about to expire.</li>
 * </ul>
 * <p>
 * The idea behind the System Events API is to provide message queue that
 * services can read at specific moments in time, which are not limited to
 * simple notifications.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public interface SystemEventsAPI {

	/**
	 * 
	 * @param event
	 * @param payload
	 */
	public void push(SystemEvent systemEvent) throws DotDataException;

	/**
	 * 
	 * @param createdDate
	 * @return
	 */
	public Collection<SystemEvent> getEventsSince(long createdDate) throws DotDataException;

	/**
	 * 
	 * @return
	 */
	public Collection<SystemEvent> getAll() throws DotDataException;

	/**
	 * 
	 * @param deleteEvents
	 */
	public void deleteEvents(long toDate) throws DotDataException;

	/**
	 * 
	 * @param deleteEvents
	 */
	public void deleteEvents(long fromDate, long toDate) throws DotDataException;

	/**
	 * 
	 */
	public void deleteAll() throws DotDataException;

}
