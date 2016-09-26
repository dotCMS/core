package com.dotcms.api.system.event;

import java.util.Collection;

import com.dotcms.api.system.event.dto.SystemEventDTO;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;

/**
 * This API allows users and other services to record and retrieve different
 * types of events that dotCMS and custom services (specially UI components) can
 * react to. For example, system events can be:
 * <ul>
 * <li>An entry in the Notifications component.</li>
 * <li>A contentlet that has been added to or deleted from the system which will
 * display a message on screen.</li>
 * <li>A message indicating that the enterprise license is about to expire.</li>
 * </ul>
 * <p>
 * The idea behind the System Events API is to provide message queue that
 * services can read at specific moments in time, which are not limited to
 * simple notifications. For example, existing features such as the Site Browser
 * or the Publishing Queue can react to incoming notifications and display
 * information to the user, or even refresh their content to reflect new
 * changes.
 * <p>
 * For practical examples, a Quartz job can be constantly monitoring system
 * events added to the internal message queue and trigger a process based on
 * their information or payload.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public interface SystemEventsAPI {

	/**
	 * Pushes a new System Event to the message queue. The {@link SystemEvent}
	 * is supposed to contain all the information it needs.
	 * 
	 * @param event
	 *            - The {@link SystemEvent} object.
	 * @throws DotDataException
	 *             An error occurred when saving the event.
	 */
	public void push(SystemEvent systemEvent) throws DotDataException;

	public void push(SystemEventType event, Payload payload) throws DotDataException;

	/**
	 * Returns a list of {@link SystemEvent} objects that were created from a
	 * specific date up to the present.
	 * 
	 * @param fromDate
	 *            - The date from which system events will be selected.
	 * @return The collection of {@link SystemEvent} objects.
	 * @throws DotDataException
	 *             An error occurred when retrieving the list of events.
	 */
	public Collection<SystemEvent> getEventsSince(long fromDate) throws DotDataException;

	/**
	 * Returns the list of all {@link SystemEvent} objects in the database.
	 * 
	 * @return The complete collection of {@link SystemEvent} objects.
	 * @throws DotDataException
	 *             An error occurred when retrieving the events.
	 */
	public Collection<SystemEvent> getAll() throws DotDataException;

	/**
	 * Deletes all the {@link SystemEvent} objects up to the specified date.
	 * 
	 * @param toDate
	 *            - The date up to which all system events will be deleted.
	 * @throws DotDataException
	 *             An error occurred when deleting the events.
	 */
	public void deleteEvents(long toDate) throws DotDataException;

	/**
	 * Deletes all the {@link SystemEvent} objects that fall into the specified
	 * date range.
	 * 
	 * @param fromDate
	 *            - The lower boundary of the date range.
	 * @param toDate
	 *            - The upper boundary of the date range.
	 * @throws DotDataException
	 *             An error occurred when deleting the events.
	 */
	public void deleteEvents(long fromDate, long toDate) throws DotDataException;

	/**
	 * Deletes all the {@link SystemEventDTO} objects from the database.
	 * 
	 * @throws DotDataException
	 *             An error occurred when deleting the events.
	 */
	public void deleteAll() throws DotDataException;

}
