package com.dotmarketing.portlets.calendar.business;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

public interface EventAPI {
	/**
	 * Search for an specific event by inode also checks that the given user has permissions 
	 * @param inode
	 * @param respectFrontendRoles
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */

	public Event findbyInode(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	/**
	 * Search for an specific event by identifier also checks that the given user has permissions 
	 * if the user is null the it will check for frontend access the event
	 * @param id
	 * @param live
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Event find(String id, boolean live, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * 
	 * @param startDate
	 * @param endDate
	 * @param tags
	 * @param keyword
	 * @param categories
	 * @return
	 * @throws DotSecurityException 
	 */
	public List<Event> find(Date startDate, Date endDate, String[] tags,
			String[] keywords, List<Category> categories, boolean liveOnly, boolean includeArchived, int offset, int limit,
			User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * 
	 * @param hostId
	 * @param startDate
	 * @param endDate
	 * @param tags
	 * @param keyword
	 * @param categories
	 * @return
	 * @throws DotSecurityException 
	 */
	public List<Event> find(String hostId, Date startDate, Date endDate, String[] tags,
			String[] keywords, List<Category> categories, boolean liveOnly, boolean includeArchived, int offset, int limit,
			User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	

	/**
	 * This method retrieves the related events based on the baseEvent categories and tags
	 * @param baseEvent
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException This exception is thrown if the user doesn't have permission 
	 *  		consult the baseEvent
	 */
	public List<Event> findRelatedEvents(Event baseEvent, Date fromDate, Date toDate, boolean live, User user, boolean respectFrontendRoles) 
		throws DotDataException, DotSecurityException;

	
	/**
	 * This method retrieves the list of categories associated to the given event
	 * @param ev
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Category> getCategories(Event ev, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException;

	/**
	 * Associates the given categories to the event It checks the user
	 * permissions on the given categories before save. this method first
	 * removes the actual event categories (only the ones the user has
	 * permissions on) and then assigns the ones passed as parameter.
	 * 
	 * @param ev
	 * @param cats
	 * @param user
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void setCategories(Event ev, List<Category> cats, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException;

	

	/**
	 * Associates the given list of contentlets using the relationship this
	 * methods removes old associated content and reset the relatioships based
	 * on the list of content passed as parameter
	 * 
	 * @param rel
	 * @param cont
	 *            The order in the list matters
	 * @param user
	 *            The user is checked to see if he has the required permissions
	 *            to modify the event relationship
	 */
	public void setRelatedContent(Event ev, Relationship rel,
			List<Contentlet> cont, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException;

	/**
	 * Associates the given list of contentlets using the relationship
	 * 
	 * @param rel
	 * @param cont
	 *            The order in the list matters
	 * @param user
	 *            The user is checked to see if he has the required permissions
	 *            to modify the event relationship
	 */
	public List<Contentlet> getRelatedContent(Event ev, Relationship rel,
			User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the content structure used by events
	 * @return
	 */
	public Structure getEventStructure() throws DotDataException;

	/**
	 * Retrieves the content structure used by events
	 * @return
	 */
	public Structure getFacilityStructure() throws DotDataException;

	/**
	 * Retrieves the content structure used by events
	 * @return
	 */
	public Structure getBuildingStructure() throws DotDataException;

	/**
	 * This method receive an Event and return the iCal format of the event.
	 * @param event The event to be transformed in the iCal format.
	 * @param recurrenceStartDate
	 * @param recurrenceEndDate
	 * @return A String that represents the iCal format of the event and could be used for download, email, etc. 
	 */
	
	public String createVCalendarInfo(Event event, Date recurrenceStartDate, Date recurrenceEndDate, Host host);
	
	/**
	 * Disconnects an event from the recurrence
	 * WILL COMMIT YOUR TRANSACTION IF YOU HAVE ONE
	 * @param event The base event of the recurrence
	 * @param user 
	 * @param startDate The startDate of the event to disconnect
	 * @param endDate The endDate of the event to disconnect
	 * @return The disconnected event with the given start and end dates
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws ParseException
	 */
	public Event disconnectEvent(Event event, User user, Date startDate, Date endDate) throws DotDataException, DotSecurityException, ParseException;
	
	
	
	
}
