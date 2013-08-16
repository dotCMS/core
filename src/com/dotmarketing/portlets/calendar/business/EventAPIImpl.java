package com.dotmarketing.portlets.calendar.business;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.Html;

public class EventAPIImpl implements EventAPI {

	PermissionAPI perAPI;
	EventFactory eventFactory;
	CategoryAPI catAPI;
	ContentletAPI conAPI;

	public EventAPIImpl() {
		perAPI = APILocator.getPermissionAPI();
		eventFactory = FactoryLocator.getEventFactory();
		catAPI = APILocator.getCategoryAPI();
		conAPI = APILocator.getContentletAPI();
	}



	/**
	 * Retrieves a list of event filtering by the given parameters, also
	 * filtering by the events that the given user is able to see, or if the
	 * given user is null it filters by all the events marked for frontend
	 * visibility
	 *
	 * @param fromDate
	 * @param endDate
	 * @param tags
	 * @param keyword
	 * @param categories
	 * @param liveOnly
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Event> find(Date fromDate, Date endDate, String[] tags, String[] keywords, List<Category> categories, boolean liveOnly, boolean includeArchived, int offset,
			int limit, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Event> events = eventFactory.find(fromDate, endDate, tags, keywords, categories, liveOnly, includeArchived, offset, limit, user, respectFrontendRoles);
		events = perAPI.filterCollection(events, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return events;
	}

	/**
	 * Retrieves a list of event filtering by the given parameters, also
	 * filtering by the events that the given user is able to see, or if the
	 * given user is null it filters by all the events marked for frontend
	 * visibility
	 *
	 * @param hostId
	 * @param fromDate
	 * @param endDate
	 * @param tags
	 * @param keyword
	 * @param categories
	 * @param liveOnly
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Event> find(String hostId, Date fromDate, Date endDate, String[] tags, String[] keywords, List<Category> categories, boolean liveOnly, boolean includeArchived, int offset,
			int limit, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Event> events = eventFactory.find(hostId, fromDate, endDate, tags, keywords, categories, liveOnly, includeArchived, offset, limit, user, respectFrontendRoles);
		events = perAPI.filterCollection(events, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return events;
	}


	/**
	 * Retrieves an event based on its identifier,
	 *
	 * @param id
	 *            Identifier of the event to find
	 * @param live
	 *            true if you want to find the live version of it, false if you
	 *            want the working version
	 * @throws DotDataException
	 * @throws DotSecurityException
	 *             If the user doesn't have permissions to see this event
	 */
	public Event find(String id, boolean live, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Event ev = eventFactory.find(RecurrenceUtil.getBaseEventIdentifier(id), live, user, respectFrontendRoles);
		Contentlet cont = new Contentlet();
		cont = conAPI.find(ev.getInode(), user, respectFrontendRoles);
		if (!perAPI.doesUserHavePermission(cont, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permissions to access this event");

		if(ev.isRecurrent()) {
			String[] recDates = RecurrenceUtil.getRecurrenceDates(id);
			if(recDates!=null && recDates.length==2){
				String startDate = recDates[0];
				String endDate = recDates[1];
				if(UtilMethods.isSet(startDate) && UtilMethods.isSet(endDate)){
					ev.setStartDate(new Date(Long.parseLong(startDate)));
					ev.setEndDate(new Date(Long.parseLong(endDate)));
				}
			}
		}
		return ev;
	}

	public Event findbyInode(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Event ev = eventFactory.findbyInode(inode, user, respectFrontendRoles);
		Contentlet cont = new Contentlet();
		cont = conAPI.find(ev.getInode(), user, respectFrontendRoles);
		if (!perAPI.doesUserHavePermission(cont, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permissions to access this event");
		return ev;
	}

	public List<Event> findRelatedEvents(Event baseEvent, Date fromDate, Date toDate, boolean live, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		List<Category> categories = this.getCategories(baseEvent, user, respectFrontendRoles);
		String tags = baseEvent.getTags() == null?"":baseEvent.getTags();
		String []tagsArray=tags.split(",");
		for(int a=0; a < tagsArray.length ; a++ ){
			tagsArray[a]=tagsArray[a].trim();
		}
		List<Event> events = eventFactory.find(fromDate, toDate, tagsArray, null, categories, live, false, 0, -1, user, respectFrontendRoles);
		events = perAPI.filterCollection(events, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return events;
	}

	public List<Category> getCategories(Event ev, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

		List<Category> cats = new ArrayList<Category>();

		Contentlet cont = new Contentlet();
		cont = conAPI.find(ev.getInode(), user, respectFrontendRoles);

		if (!perAPI.doesUserHavePermission(cont, PermissionAPI.PERMISSION_READ, user))
			throw new DotSecurityException("User doesn't have permissions to save events");

		if (cont != null) {
			cats = (List<Category>) catAPI.getParents(cont, false, user, respectFrontendRoles);
		}

		return cats;
	}

	/**
	 * This methods removes all the categories the user is able to remove from
	 * the event and then associates the categories passed as paramater
	 */
	public void setCategories(Event ev, List<Category> cats, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		List<Category> oldcats = new ArrayList<Category>();

		oldcats = getCategories(ev, user, respectFrontendRoles);

		Contentlet cont = new Contentlet();
		cont = conAPI.find(ev.getInode(), user, respectFrontendRoles);

		for (Category category : cats) {
			if (!catAPI.canUseCategory(category, user, false))
				throw new DotSecurityException("User is not able to use the given category inode = " + category.getInode());
		}

		if (!oldcats.isEmpty()) {
			for (Category category : oldcats) {
				if (catAPI.canUseCategory(category, user, false)) {
					catAPI.removeChild(cont, category, user, respectFrontendRoles);
				}
			}
		}
		for (Category node : cats) {
			catAPI.addParent(cont, node, user, respectFrontendRoles);
		}

	}

	public List<Contentlet> getRelatedContent(Event ev, Relationship rel, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Contentlet cont = new Contentlet();
		cont = conAPI.find(ev.getInode(), user, respectFrontendRoles);
		List<Contentlet> contentlets = conAPI.getRelatedContent(cont, rel, user, respectFrontendRoles);
		perAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return contentlets;
	}

	public void setRelatedContent(Event ev, Relationship rel, List<Contentlet> related, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		Contentlet cont = new Contentlet();
		cont = conAPI.find(ev.getInode(), user, respectFrontendRoles);
		conAPI.relateContent(cont, rel, related, user, respectFrontendRoles);

	}

	public Structure getBuildingStructure() throws DotDataException {
		return eventFactory.getBuildingStructure();
	}

	public Structure getEventStructure() throws DotDataException {
		return eventFactory.getEventStructure();
	}

	public Structure getFacilityStructure() throws DotDataException {
		return eventFactory.getLocationStructure();
	}


	public String createVCalendarInfo(Event event, Date recurrenceStartDate, Date recurrenceEndDate, Host host)
	{
		StringBuilder result = new StringBuilder(512);
		result.ensureCapacity(128);

		try {
			SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat timeformat = new SimpleDateFormat("HHmmss");

			java.util.Calendar gcal = new GregorianCalendar();

			Date startDate = event.getStartDate();
			Date endDate = event.getEndDate();

			if(UtilMethods.isSet(recurrenceStartDate)){
				startDate = recurrenceStartDate;
			}

	        if(UtilMethods.isSet(recurrenceEndDate)){
				endDate = recurrenceEndDate;
			}

			gcal.setTime(startDate);
			String startTime = timeformat.format(gcal.getTime());

			gcal.setTime(endDate);
			String endTime = timeformat.format(gcal.getTime());

			result.append("BEGIN:VCALENDAR\n");
			result.append("PRODID:" + Config.getStringProperty("PRODID") + "\n");
			result.append("METHOD:" + Config.getStringProperty("METHOD") + "\n");
			result.append("SCALE:" + Config.getStringProperty("SCALE") + "\n");
			result.append("VERSION:" + Config.getStringProperty("VERSION") + "\n");
			result.append("BEGIN:VEVENT\n");
			try {
				result.append("DTSTART:" + dateformat.format(startDate) + "T" + startTime + "\n");
			} catch(NullPointerException ex) {
				result.append("DTSTART:" + dateformat.format(endDate) + "T\n");
			}
			try {
				result.append("DTEND:" + dateformat.format(startDate) + "T" + endTime + "\n");
			} catch(NullPointerException ex) {
				result.append("DTEND:" + dateformat.format(endDate) + "T\n");
			}
			result.append("LOCATION:" + (event.getLocation() == null ? "" : event.getLocation()) + "\n");
			result.append("UID:" + event.getIdentifier().toUpperCase() + "@" + host.getHostname() + "\n");
			result.append("DESCRIPTION;ENCODING=QUOTED-PRINTABLE:");
			result.append(Html.stripHtml(event.getDescription().trim()).replaceAll("\r\n", "=0D=0A").replaceAll("\n", "=0D=0A"));
			result.append("\n");
			result.append("SUMMARY;ENCODING=QUOTED-PRINTABLE:");
			result.append(event.getTitle());
			result.append("\n");
			result.append("PRIORITY:1\n");
			result.append("END:VEVENT\n");
			result.append("END:VCALENDAR");
		} catch (Exception e) {
			Logger.warn(this, e.toString());
		}
		return result.toString();
	}

	public Event disconnectEvent(Event event, User user, Date startDate, Date endDate) throws DotDataException, DotSecurityException{
		Event newEvent = null;
		if(event!=null && event.isRecurrent()){
			Contentlet newCont = conAPI.copyContentlet(event,user, true);
			newEvent = eventFactory.convertToEvent(newCont);
			newEvent.setDisconnectedFrom(event.getIdentifier());
			newEvent.setRecurrenceDatesToIgnore("");
			newEvent.setRecurs(false);
			newEvent.setRecurrenceDayOfMonth(0);
			newEvent.setRecurrenceDayOfWeek(0);
			newEvent.setRecurrenceDaysOfWeek("");
			newEvent.setRecurrenceInterval(0);
			newEvent.setRecurrenceMonthOfYear(0);
			newEvent.setRecurrenceOccurs("");
			newEvent.setRecurrenceWeekOfMonth(0);
			newEvent.setRecurrenceStart(null);
			newEvent.setRecurrenceEnd(null);
			newEvent.setNoRecurrenceEnd(false);
			newEvent.setOriginalStartDate(event.getStartDate());
			newEvent.setStartDate(startDate);
			newEvent.setEndDate(endDate);
			newEvent.setInode("");
			event.addDateToIgnore(startDate);

			List<Category> eventCategories =  APILocator.getCategoryAPI().getParents(event, user, true);

			Contentlet oldCont  = conAPI.checkout(event.getInode(), user, true);
			oldCont.setStringProperty("recurrenceDatesToIgnore", event.getStringProperty("recurrenceDatesToIgnore"));
			oldCont = conAPI.checkin(oldCont, user, true);
			if(event.isLive())
			    APILocator.getVersionableAPI().setLive(oldCont);
			newEvent = eventFactory.convertToEvent(conAPI.checkin(newEvent, user, true, eventCategories));
		}
		return newEvent;
	}


}
