package com.dotmarketing.portlets.calendar.ajax;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.queryParser.ParseException;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.rating.api.RatingAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.business.RecurrenceUtil;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.calendar.model.Event.Occurrency;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.viewtools.CommentsWebAPI;
import com.dotmarketing.viewtools.DateViewWebAPI;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author David
 */
public class CalendarAjax {

	private EventAPI eventAPI;
	private PermissionAPI perAPI;
	private ContentletAPI contAPI;
	private CategoryAPI categoryAPI;
	private UserWebAPI userAPI;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");	
	private static SimpleDateFormat dateFormat2 = new SimpleDateFormat("MM/dd/yyyy");
	private static DateFormat eventRecurrenceEndDateF = new SimpleDateFormat("yyyy-MM-dd");

	public CalendarAjax () {
		eventAPI = APILocator.getEventAPI();
		categoryAPI = APILocator.getCategoryAPI();
		contAPI = APILocator.getContentletAPI();
		userAPI = WebAPILocator.getUserWebAPI();
		perAPI = APILocator.getPermissionAPI();
	}
	
	public Map<String, Object> getEvent (String id, boolean live) 
		throws DotDataException, DotSecurityException, PortalException, SystemException {
		
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		Event ev = eventAPI.find(id, live, user, respectFrontendRoles);
		
		Map<String, Object> eventMap = ev.getMap();
		
		//Loading categories
		List<Map<String, Object>> categoryMaps = new ArrayList<Map<String,Object>>();
		List<Category> eventCategories =  categoryAPI.getParents(ev, user, respectFrontendRoles);
		for(Category cat : eventCategories) {
			categoryMaps.add(cat.getMap());
		}
		eventMap.put("categories", categoryMaps);
		eventMap.put("rating", RatingAPI.getAverageRating(ev.getIdentifier()));
		eventMap.put("votes", RatingAPI.getRatingVotesNumber(ev.getIdentifier()));
		eventMap.put("hasReadPermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles));
		eventMap.put("hasWritePermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles));
		eventMap.put("hasPublishPermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles));
		eventMap.put("readPermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles));
		eventMap.put("writePermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles));
		eventMap.put("publishPermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles));
		eventMap.put("isDisconnected", UtilMethods.isSet(ev.getDisconnectedFrom()));
		CommentsWebAPI cAPI = new CommentsWebAPI();
		cAPI.setUser(user);
		cAPI.setRespectFrontendRoles(respectFrontendRoles);
		eventMap.put("commentsCount", cAPI.getCommentsCount(ev.getInode()));
		
		return eventMap;
		
	}

	public List<Map<String, Object>> findEvents (Date fromDate, Date toDate, String[] tags, 
			String[] keywords, String[] categoriesInodes, boolean live, boolean includeArchived, int offset, int limit) 
		throws DotDataException, DotSecurityException, PortalException, SystemException {
		
		return findEventsByHostFolder(null, fromDate, toDate, tags, keywords, categoriesInodes, live, includeArchived, offset, limit);
	}
	
	
	public List<Map<String, Object>> findEventsForDay(String hostId, String dateStr, String[] tags, 
			String[] keywords, String[] categoriesInodes, boolean live, boolean includeArchived, int offset, int limit) 
			throws DotDataException, DotSecurityException, PortalException, SystemException, java.text.ParseException {
		
		Date fromDate = null;
		Date toDate = null;
		if(UtilMethods.isSet(dateStr)){
			String date = dateFormat.format(dateFormat2.parse(dateStr));
			fromDate =  dateFormat.parse(date);
		}
		
		Calendar fromDateCal = Calendar.getInstance();
		Calendar toDateCal = Calendar.getInstance();
		fromDateCal.setTime(fromDate);
		toDateCal.setTime(fromDate);
		
		fromDateCal.set(Calendar.HOUR, 0);
		fromDateCal.set(Calendar.MINUTE, 0);
		fromDateCal.set(Calendar.SECOND, 0);	
		
		toDateCal.set(Calendar.HOUR, 23);
		toDateCal.set(Calendar.MINUTE, 59);
		toDateCal.set(Calendar.SECOND, 59);
		
		fromDate = fromDateCal.getTime();
		toDate = toDateCal.getTime();
		
		return findEventsByHostId(hostId, fromDate, toDate, tags, keywords, categoriesInodes, live, includeArchived, offset, limit);
		
	}

	
	public List<Map<String, Object>> findEventsByHostFolder(String hostId, Date fromDate, Date toDate, String[] tags, 
			String[] keywords, String[] categoriesInodes, boolean live, boolean includeArchived, int offset, int limit) 
		throws DotDataException, DotSecurityException, PortalException, SystemException {
		
		return findEventsByHostId(hostId, fromDate, toDate, tags, keywords, categoriesInodes, live, includeArchived, offset, limit);
		
	}
	
	public List<Map<String, Object>> findEventsByHostId(String hostId, Date fromDate, Date toDate, String[] tags, 
			String[] keywords, String[] categoriesInodes, boolean live, boolean includeArchived, int offset, int limit) 
			throws DotDataException, DotSecurityException, PortalException, SystemException{
		
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		List<Map<String, Object>> retList = new ArrayList<Map<String,Object>>();
		List<Category> categories = new ArrayList<Category>();
		if(categoriesInodes != null) {
			for (String categoryInode : categoriesInodes) {
				Category cat = categoryAPI.find(categoryInode, user, respectFrontendRoles);
				if(cat != null)
					categories.add(cat);
			}
		}
		List<Event> events = eventAPI.find(hostId, fromDate, toDate, tags, keywords, categories, live, includeArchived, offset, limit, user, respectFrontendRoles);
		for(Event ev : events) {
			Map<String, Object> eventMap = ev.getMap();

			//Loading categories
			List<Map<String, Object>> categoryMaps = new ArrayList<Map<String,Object>>();
			List<Category> eventCategories =  categoryAPI.getParents(ev, user, respectFrontendRoles);
			for(Category cat : eventCategories) {
				categoryMaps.add(cat.getMap());
			}
			
			// http://jira.dotmarketing.net/browse/DOTCMS-6904 
			// we're missing [working, live, deleted] info
			
			// sometimes we mess with identifier adding recurrence info
			String origIdent=ev.getIdentifier();
			String realIdent=APILocator.getIdentifierAPI().findFromInode(ev.getInode()).getId();
			ev.setIdentifier(realIdent);
			eventMap.put("live", ev.isLive());
			eventMap.put("working", ev.isWorking());
			eventMap.put("archived", ev.isArchived());
			eventMap.put("deleted", ev.isArchived());
			eventMap.put("locked", ev.isLocked());
			ev.setIdentifier(origIdent);
			
			eventMap.put("categories", categoryMaps);
			eventMap.put("rating", RatingAPI.getAverageRating(ev.getIdentifier()));
			eventMap.put("votes", RatingAPI.getRatingVotesNumber(ev.getIdentifier()));
			CommentsWebAPI cAPI = new CommentsWebAPI();
			cAPI.setUser(user);
			cAPI.setRespectFrontendRoles(respectFrontendRoles);
			eventMap.put("commentsCount", cAPI.getCommentsCount(ev.getInode()));

			eventMap.put("hasReadPermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_READ, user));
			eventMap.put("hasWritePermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_WRITE, user));
			eventMap.put("hasPublishPermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_PUBLISH, user));
			eventMap.put("readPermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_READ, user));
			eventMap.put("writePermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_WRITE, user));
			eventMap.put("publishPermission", perAPI.doesUserHavePermission(ev, PermissionAPI.PERMISSION_PUBLISH, user));
			eventMap.put("offSet",DateViewWebAPI.getOffSet(ev.getStartDate()));
			eventMap.put("isDisconnected", UtilMethods.isSet(ev.getDisconnectedFrom()));

			retList.add(eventMap);
		}
		return retList;
	}
		


	public List<Map<String, Object>> findRelatedEvents (String parentEvent, Date fromDate, Date toDate, boolean live) 
		throws DotDataException, DotSecurityException, PortalException, SystemException {
		
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;
		
		List<Map<String, Object>> retList = new ArrayList<Map<String,Object>>();
		Event parentEv = eventAPI.find(parentEvent, live, user, respectFrontendRoles);
		List<Event> events = eventAPI.findRelatedEvents(parentEv, fromDate, toDate, live, user, respectFrontendRoles);
		for(Event ev : events) {
			retList.add(ev.getMap());
		}
		return retList;
	}
	
	public void publishEvent (String identifier) throws PortalException, SystemException, DotDataException, DotSecurityException {
		HibernateUtil.startTransaction();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;
		String baseIdent = RecurrenceUtil.getBaseEventIdentifier(identifier);		
		Event ev = eventAPI.find(baseIdent, false, user, respectFrontendRoles);
		try{
			contAPI.publish(ev, user, respectFrontendRoles);
		}catch(Exception e){Logger.error(this, e.getMessage());}
		
		HibernateUtil.commitTransaction();
		if(!contAPI.isInodeIndexed(ev.getInode())){
			Logger.error(this, "Timed out while waiting for index to return");
		}
	}

	public Map<String,Object> unpublishEvent (String identifier) throws PortalException, SystemException, DotDataException, DotSecurityException {
		Map<String,Object> callbackData = new HashMap<String,Object>();//DOTCMS-5199
		List<String> eventUnpublishErrors = new ArrayList<String>();
		HibernateUtil.startTransaction();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		Event ev = eventAPI.find(identifier, false, user, respectFrontendRoles);

		try {	
			contAPI.unpublish(ev, user, respectFrontendRoles);  
		} catch (DotSecurityException e) {
			eventUnpublishErrors.add(e.getLocalizedMessage());
		} catch (DotDataException e) {
			eventUnpublishErrors.add(e.getLocalizedMessage());
		} catch (DotContentletStateException e) {
			eventUnpublishErrors.add(e.getLocalizedMessage());
		}finally{			
			if(eventUnpublishErrors.size() > 0){
				callbackData.put("eventUnpublishErrors", eventUnpublishErrors);								
			}				
		}
		HibernateUtil.commitTransaction();
		if(!contAPI.isInodeIndexed(ev.getInode())){
			Logger.error(this, "Timed out while waiting for index to return");
		}

		return callbackData;
	}

	public void archiveEvent (String identifier) throws PortalException, SystemException, DotDataException, DotSecurityException {
		HibernateUtil.startTransaction();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		Event ev = eventAPI.find(identifier, false, user, respectFrontendRoles); 
		try{
			contAPI.archive(ev, user, respectFrontendRoles);
		}catch(Exception e){Logger.error(this, e.getMessage());}

		HibernateUtil.commitTransaction();
		if(!contAPI.isInodeIndexed(ev.getInode())){
			Logger.error(this, "Timed out while waiting for index to return");
		}
	}
	
	public void archiveDisconnectedEvent (String identifier, boolean putBack) throws PortalException, SystemException, DotDataException, DotSecurityException {
		HibernateUtil.startTransaction();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		Event ev = eventAPI.find(identifier, false, user, respectFrontendRoles);
		if(putBack){
			Event baseEvent = null;
			try{
			   baseEvent =eventAPI.find(ev.getDisconnectedFrom(), false, user, respectFrontendRoles);
			}catch(Exception e){
				Logger.error(this, "Base event not found");
			}
			if(baseEvent!=null){
				try{
				  Date originalStartDate = ev.getOriginalStartDate();
				  baseEvent.deleteDateToIgnore(originalStartDate);
				  APILocator.getContentletAPI().checkin(baseEvent, categoryAPI.getParents(baseEvent, user, true), perAPI.getPermissions(baseEvent), user, false);
				}catch(Exception e){
					Logger.error(this, "Could not put back event in recurrence");
				}
			}
		}
		contAPI.archive(ev, user, respectFrontendRoles);
		
		
		HibernateUtil.commitTransaction();
		if(!contAPI.isInodeIndexed(ev.getInode())){
			Logger.error(this, "Timed out while waiting for index to return");
		}
	}

	
	
	

	public void unarchiveEvent (String identifier) throws PortalException, SystemException, DotDataException, DotSecurityException {
		HibernateUtil.startTransaction();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		Event ev = eventAPI.find(identifier, false, user, respectFrontendRoles);
		try{
			
			if(UtilMethods.isSet(ev.getDisconnectedFrom())){
				Event baseEvent = null;
				try{
				   baseEvent =eventAPI.find(ev.getDisconnectedFrom(), false, user, respectFrontendRoles);
				}catch(Exception e){
					Logger.error(this, "Base event not found");
				}
				if(baseEvent!=null){
					try{
						Date originalStartDate = ev.getOriginalStartDate();
						baseEvent.addDateToIgnore(originalStartDate);
						APILocator.getContentletAPI().checkin(baseEvent, categoryAPI.getParents(baseEvent, user, true), perAPI.getPermissions(baseEvent), user, false);
					}catch(Exception e){
						Logger.error(this, "Could not delete event from recurrence");
					}
				}
			}
			contAPI.unarchive(ev, user, respectFrontendRoles);
			
		}catch(Exception e){Logger.error(this, e.getMessage());}

		HibernateUtil.commitTransaction();
		if(!contAPI.isInodeIndexed(ev.getInode())){
			Logger.error(this, "Timed out while waiting for index to return");
		}
	}	



	public Map<String,Object> deleteEvent (String identifier) throws PortalException, SystemException, DotDataException, DotSecurityException {
		HibernateUtil.startTransaction();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		List<String> eventDeleteErrors = new ArrayList<String>();
		Map<String,Object> callbackData = new HashMap<String,Object>();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		Event ev = eventAPI.find(identifier, false, user, respectFrontendRoles);
		if(ev.isLive()){
			try {	
				contAPI.unpublish(ev, user, respectFrontendRoles);  
			} catch (DotSecurityException e) {
				eventDeleteErrors.add(e.getLocalizedMessage());
			} catch (DotDataException e) {
				eventDeleteErrors.add(e.getLocalizedMessage());
			} catch (DotContentletStateException e) {
				eventDeleteErrors.add(e.getLocalizedMessage());
			}
			try{
				contAPI.archive(ev, user, respectFrontendRoles);
			}catch(Exception e){
				eventDeleteErrors.add(e.getLocalizedMessage());
			}
		}else if(!ev.isArchived()){
			try{
				contAPI.archive(ev, user, respectFrontendRoles);
			}catch(Exception e){
				eventDeleteErrors.add(e.getLocalizedMessage());
			}
		}
		
		try{
			if(ev.isArchived()){
				contAPI.delete(ev, user, respectFrontendRoles);
			}
		}catch(Exception e){	
			eventDeleteErrors.add(e.getLocalizedMessage());
		}finally{
			if(eventDeleteErrors.size() > 0){
				callbackData.put("eventUnpublishErrors", eventDeleteErrors);								
			}				
		}
		if(eventDeleteErrors.size()<=0){
		   HibernateUtil.commitTransaction();
		}

        //At this point we already deleted the content from the index on the delete call
		/*if(!contAPI.isInodeIndexed(ev.getInode())){
			Logger.error(this, "Timed out while waiting for index to return");
		}*/
		
		return callbackData;
	}		
	
	public List<Map<String, Object>> findLocations(String filter) throws DotDataException, DotSecurityException, ParseException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		//Searching for buildings
		Structure buildingStructure = eventAPI.getBuildingStructure();
		Field titleField = buildingStructure.getFieldVar("title");
		String luceneQuery = "+structureInode:" + buildingStructure.getInode();
		List<Map<String, Object>> results = new ArrayList<Map<String,Object>>();
		List<Contentlet> matches = contAPI.search(luceneQuery, -1, 0, titleField.getFieldContentlet(), user, respectFrontendRoles);
		List<Map<String, Object>> facilitiesList = findChildFacilities(matches, filter, user, respectFrontendRoles);
	
		for(Contentlet cont:matches){
			List<Map<String, Object>> facilitiesListCont = new ArrayList<Map<String,Object>>(); 
			Map<String, Object> contMap = cont.getMap();
			if(!UtilMethods.isSet(filter) || facilitiesList.size() > 0 || ((String)contMap.get("title")).contains(filter)) {
				for(Map<String,Object> facility: facilitiesList){
					for(Contentlet building: (ArrayList<Contentlet>)facility.get("buildings")){
						if(building.getIdentifier().equals(cont.getIdentifier()) && !facilitiesListCont.contains(facility)){
							Map<String,Object> facilityMap = new HashMap<String,Object>();
							facilityMap.putAll(facility);
							facilityMap.put("buildings",null);
							facilitiesListCont.add(facilityMap);
							break;
						}
					}

				}
				contMap.put("facilities", facilitiesListCont);
				results.add(contMap);
			}
		}
		return results;
	}

	private List<Map<String, Object>> findChildFacilities(List<Contentlet> buildingConts, String filter, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, ParseException {
		List<Map<String, Object>> results = new ArrayList<Map<String,Object>>();
		
		//Searching for children facilities
		Structure facilityStructure = eventAPI.getFacilityStructure();
		// Facility Structure might be absent http://jira.dotmarketing.net/browse/DOTCMS-6275
		if(facilityStructure.getName()!=null) {
			Field titleField = facilityStructure.getFieldVar("title");
			String luceneQuery = "+structureInode:" + facilityStructure.getInode() + " +(";
			for(Contentlet cont:buildingConts){
				luceneQuery+= " Building-Facility:" + cont.getIdentifier() + " ";
			}
			luceneQuery+=") ";
			if(UtilMethods.isSet(filter))
				luceneQuery += " +" + titleField.getFieldContentlet() + ":" + filter.trim() + "*";
			
			List<Contentlet> matches = contAPI.search(luceneQuery, -1, 0, titleField.getFieldContentlet(), user, respectFrontendRoles);
			List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(eventAPI.getBuildingStructure());
			for(Contentlet cont : matches) {
				List<Contentlet> relCont = new ArrayList<Contentlet>();
				for(Relationship rel: rels){
					if(rel.getChildStructure().equals(eventAPI.getFacilityStructure()) && 
							rel.getParentStructure().equals(eventAPI.getBuildingStructure())){
						relCont.addAll(APILocator.getContentletAPI().getRelatedContent(cont, rel , user, respectFrontendRoles));
					}
				}
				Map<String, Object> contMap = cont.getMap();
				contMap.put("buildings", relCont);
				results.add(contMap);
			}
		}
		return results;
	}


	public Map<String, Object> saveEvent(List<String> formData,
			boolean isAutoSave, boolean isCheckin) throws LanguageException,
			PortalException, SystemException, DotDataException,
			DotSecurityException, java.text.ParseException {

		HibernateUtil.startTransaction();
		ContentletWebAPI contentletWebAPI = WebAPILocator.getContentletWebAPI();
		int tempCount = 0;// To store multiple values opposite to a name. Ex: selected permissions & categories	
		String newInode = "";
		
		String referer = "";
		String language = "";
		String strutsAction = "";		
		String recurrenceDaysOfWeek="";
		
		Map<String,Object> contentletFormData = new HashMap<String,Object>();		
		Map<String,Object> callbackData = new HashMap<String,Object>();
		List<String> saveContentErrors = new ArrayList<String>();
		
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = com.liferay.portal.util.PortalUtil.getUser((HttpServletRequest)req); 
		List<Field> fields = com.dotmarketing.cache.FieldsCache.getFieldsByStructureInode(eventAPI.getEventStructure().getInode());
		String titleField = "";
		String urlTitleField = "";
		String urlTitleFieldValue = "";
		String titleFieldValue = "";
		
		for(Field field : fields){
			if(field.getVelocityVarName().equals("urlTitle")){
				urlTitleField =  field.getFieldContentlet();
			}
			if(field.getVelocityVarName().equals("title")){
				titleField =  field.getFieldContentlet();
			}
			if(UtilMethods.isSet(titleField) && UtilMethods.isSet(urlTitleField)){
				break;
			}

		}
		
		// get the struts_action from the form data
		for (Iterator<String> iterator = formData.iterator(); iterator.hasNext();) {
			String element = iterator.next();	
			if(element!=null) {
    			String elementName = element.substring(0, element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR));		
    
    			if (elementName.startsWith("_EXT") && elementName.endsWith("cmd")) {
    				strutsAction = elementName.substring(0, elementName.indexOf("cmd"));
    				break;
    			}
			}
		}		
		
		// Storing form data into map.
		for (Iterator<String> iterator = formData.iterator(); iterator.hasNext();) {
			String element = iterator.next();			
		
			if (!com.dotmarketing.util.UtilMethods.isSet(element))
				continue;
			
			String elementName = element.substring(0, element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR));
			Object elementValue = element.substring(element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR) + WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR.length());
			
			if (element.startsWith(strutsAction))
				elementName = elementName.substring(elementName
										.indexOf(strutsAction)
										+ strutsAction.length());

			// Placed increments as Map holds unique keys.
			if(elementName.equals("read") 
				||elementName.equals("write")
				||elementName.equals("publish")){
				
				tempCount++;
				elementName = "selected_permission_"+tempCount+elementName;
			}
			
			if(elementName.equals(titleField)){
				titleFieldValue = (String)elementValue;
			}
			
			if(elementName.equals(urlTitleField)){
				urlTitleFieldValue = (String)elementValue;
			}
			
			if(elementName.equals("categories")){
					tempCount++;
					elementName = elementName+tempCount+"_";
			}
			//http://jira.dotmarketing.net/browse/DOTCMS-3232
			if(elementName.equalsIgnoreCase("hostId")){
				callbackData.put("hostOrFolder",true);		
			}
			if(elementName.startsWith("binary")){ 
				String binaryFileValue = (String) elementValue;
				if(UtilMethods.isSet(binaryFileValue) && !binaryFileValue.equals("---removed---")){
					binaryFileValue = ContentletUtil.sanitizeFileName(binaryFileValue);
			
					File binaryFile = new File(APILocator.getFileAPI().getRealAssetPathTmpBinary()
							+ File.separator + user.getUserId() + File.separator + elementName
							+ File.separator + binaryFileValue);
					if(binaryFile.exists())
						binaryFile.delete();
					elementValue = binaryFile;
				}else{
					elementValue = null;
				}
				
			}
			
			if(!UtilMethods.isSet(elementName))
				continue;			
			
			if(elementValue==null)
				elementValue="";
			
			if(elementName.equals("referer"))
				referer = (String)elementValue;
			
			if(elementName.equals("languageId"))
				language = (String)elementValue;
					
			if ( elementName.equals("recurrenceDaysOfWeek")) {
				recurrenceDaysOfWeek= recurrenceDaysOfWeek + elementValue+ ",";
			} 
				contentletFormData.put(elementName, elementValue);
						
		}		
		
			
		contentletFormData.put("recurrenceDaysOfWeek", recurrenceDaysOfWeek);

		if(!UtilMethods.isSet(urlTitleFieldValue) && UtilMethods.isSet(titleFieldValue)){

			urlTitleFieldValue = titleFieldValue.toLowerCase();
			urlTitleFieldValue = urlTitleFieldValue.replace("/^\\s+|\\s+$/g","");
			urlTitleFieldValue = urlTitleFieldValue.replace("/[^a-zA-Z 0-9]+/g"," ");	
			urlTitleFieldValue = urlTitleFieldValue.replace("/\\s/g", "-");
			while(urlTitleFieldValue.indexOf("--") > -1){
				urlTitleFieldValue = urlTitleFieldValue.replace("--","-");	
			}
			contentletFormData.put(urlTitleField, urlTitleFieldValue);
		}
		

		String d1 =(String)contentletFormData.get("date1");
		String d2 =(String)contentletFormData.get("date2");
		String d3 =(String)contentletFormData.get("recurrenceEnds");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
		Date eventStartDate = null;
		Date eventEndDate   = null;
		Date recurrenceEndDate= null;
		try {
			eventStartDate = df.parse(d1);
			eventEndDate = df.parse(d2);
			if(UtilMethods.isSet(d3)){
			   recurrenceEndDate = df2.parse(d3);
			}
		} catch (java.text.ParseException e1) {

		}
		Boolean cont=true;

		if (eventEndDate.before(eventStartDate)){    	
			String errorString = LanguageUtil.get(user,"message.event.endate.before.stardate");
			saveContentErrors.add(errorString);		
		}
		
		if(!contentletFormData.get("recurrenceOccurs").toString().equals("never")) {
    		if(contentletFormData.get("noEndDate")==null || !Boolean.parseBoolean(contentletFormData.get("noEndDate").toString())){
    			if (recurrenceEndDate!=null && recurrenceEndDate.before(eventStartDate) ){    	
    				String errorString = LanguageUtil.get(user,"message.event.recurrence.endate.before.stardate");
    				saveContentErrors.add(errorString);		
    			}
    		}
		}
		
		Calendar start = Calendar.getInstance();
		start.setTime(eventStartDate);
		Calendar end   = Calendar.getInstance();
		end.setTime(eventEndDate);
		
		if(!contentletFormData.get("recurrenceOccurs").toString().equals("never")) {
    		if(end.after(start) && (end.get(Calendar.DAY_OF_MONTH) > start.get(Calendar.DAY_OF_MONTH) || 
    				end.get(Calendar.MONTH) > start.get(Calendar.MONTH) ||
    				end.get(Calendar.YEAR) > start.get(Calendar.YEAR))){
    			contentletFormData.put("recurrenceOccurs", "never");
    		}
		}
		
		if (Boolean.parseBoolean(contentletFormData.get("recurrenceChanged").toString())) {
			if(!UtilMethods.isSet(contentletFormData.get("recurrenceInterval"))){
				String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.interval");
				saveContentErrors.add(errorString);		
			}else{
				try{
				   Long.valueOf((String)contentletFormData.get("recurrenceInterval"));
				}catch(NumberFormatException nfe){
					String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.interval");
					saveContentErrors.add(errorString);		
				}
			}
			
			if(contentletFormData.get("recurrenceOccurs").toString().equals("monthly")){
				if(Boolean.parseBoolean(contentletFormData.get("isSpecificDate").toString()) && 
						!UtilMethods.isSet((String)contentletFormData.get("recurrenceDayOfMonth"))){
					String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.dayofmonth");
					saveContentErrors.add(errorString);	
				}

				if(Boolean.parseBoolean(contentletFormData.get("isSpecificDate").toString()) &&
						UtilMethods.isSet((String)contentletFormData.get("recurrenceDayOfMonth"))){
					try{
						Long.valueOf((String)contentletFormData.get("recurrenceDayOfMonth"));
					}catch (Exception e) {
						String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.dayofmonth");
						saveContentErrors.add(errorString);		
					}
				}else{
					contentletFormData.put("recurrenceDayOfMonth", "0");
				}
			}
			
			if(contentletFormData.get("recurrenceOccurs").toString().equals("annually")){
				
				if(Boolean.parseBoolean(contentletFormData.get("isSpecificDate").toString()) && 
						!UtilMethods.isSet((String)contentletFormData.get("specificDayOfMonthRecY")) && 
						!UtilMethods.isSet((String)contentletFormData.get("specificMonthOfYearRecY"))){
					String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.date");
					saveContentErrors.add(errorString);	
				}
				
				if(Boolean.parseBoolean(contentletFormData.get("isSpecificDate").toString()) &&
						UtilMethods.isSet((String)contentletFormData.get("specificDayOfMonthRecY")) 
						&& UtilMethods.isSet((String)contentletFormData.get("specificMonthOfYearRecY"))){
					try{
						Long.valueOf((String)contentletFormData.get("specificDayOfMonthRecY"));
						contentletFormData.put("recurrenceDayOfMonth", (String)contentletFormData.get("specificDayOfMonthRecY"));
					}catch (Exception e) {
						String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.dayofmonth");
						saveContentErrors.add(errorString);		
					}
					try{
						Long.valueOf((String)contentletFormData.get("specificMonthOfYearRecY"));
						contentletFormData.put("recurrenceMonthOfYear", (String)contentletFormData.get("specificMonthOfYearRecY"));
					}catch (Exception e) {
						String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.monthofyear");
						saveContentErrors.add(errorString);		
					}
				}else{
					contentletFormData.put("recurrenceDayOfMonth", "0");
				}
			}
		}
		
		if(!contentletFormData.get("recurrenceOccurs").toString().equals("never")) {
    		if(contentletFormData.get("noEndDate")==null || (contentletFormData.get("noEndDate")!=null && 
    				!Boolean.parseBoolean(contentletFormData.get("noEndDate").toString()))){
    			if(!UtilMethods.isSet((String)contentletFormData.get("recurrenceEnds"))){
    				String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.enddate");
    				saveContentErrors.add(errorString);	
    			}else{
    				try{
    					eventRecurrenceEndDateF.parse((String)contentletFormData.get("recurrenceEnds"));
    				}catch(Exception e){
    					String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.enddate");
    					saveContentErrors.add(errorString);	
    				}
    			}
    		}
		}
		
		//http://jira.dotmarketing.net/browse/DOTCMS-6327
		if(!contentletFormData.get("recurrenceOccurs").toString().equals("never")) {
    		if(contentletFormData.get("noEndDate")==null || 
    			!Boolean.parseBoolean(contentletFormData.get("noEndDate").toString())){
    			Integer interval = UtilMethods.isSet((String) contentletFormData.get("recurrenceInterval"))?
    					Integer.valueOf((String) contentletFormData.get("recurrenceInterval")):null;
    			Integer recurrenceWeekOfMonth =UtilMethods.isSet((String) contentletFormData.get("recurrenceWeekOfMonth"))?
    					Integer.valueOf((String) contentletFormData.get("recurrenceWeekOfMonth")):null;
    			Integer recurrenceDayOfWeek =UtilMethods.isSet((String) contentletFormData.get("recurrenceDayOfWeek"))?
    					Integer.valueOf((String) contentletFormData.get("recurrenceDayOfWeek")):null;
    			Integer recurrenceMonthOfYear =UtilMethods.isSet((String) contentletFormData.get("recurrenceMonthOfYear"))?
    					Integer.valueOf((String) contentletFormData.get("recurrenceMonthOfYear")):null;
    			Integer recurrenceDayOfMonth =UtilMethods.isSet((String) contentletFormData.get("recurrenceDayOfMonth"))?
    					Integer.valueOf((String) contentletFormData.get("recurrenceDayOfMonth")):null;
    		    Occurrency occurency = Occurrency.findOcurrency((String)contentletFormData.get("recurrenceOccurs"));
    
    		    if(occurency!=null){
    		    	Calendar firstOccurence = RecurrenceUtil.calculateFirstOccurence(eventStartDate, 
    		    			interval, 
    		    			occurency,
    		    			recurrenceDaysOfWeek,
    		    			recurrenceWeekOfMonth,
    		    			recurrenceDayOfWeek,
    		    			recurrenceMonthOfYear,
    		    			recurrenceDayOfMonth);         
    		    	if (recurrenceEndDate.before(firstOccurence.getTime())){    	
    		    		String errorString = LanguageUtil.get(user,"message.event.recurrence.before.occurence");
    		    		saveContentErrors.add(errorString);		
    		    	}
    		    }
    		}
		}
		
		try {
			if(cont &&(saveContentErrors==null || saveContentErrors.isEmpty())){
				newInode = contentletWebAPI.saveContent(contentletFormData,isAutoSave,isCheckin,user);
			}
		}		
		catch (DotContentletValidationException ve) {
			
			if(ve.hasRequiredErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED);
				for (Field field : reqs) {
					String errorString = LanguageUtil.get(user,"message.contentlet.required");
					errorString = errorString.replace("{0}", field.getFieldName());
					saveContentErrors.add(errorString);
				}
			}
			
			if(ve.hasLengthErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_MAXLENGTH);
				for (Field field : reqs) {
					String errorString = LanguageUtil.get(user,"message.contentlet.maxlength");
					errorString = errorString.replace("{0}", field.getFieldName());
					errorString = errorString.replace("{1}", "225");
					saveContentErrors.add(errorString);
				}
			}
			
			if(ve.hasPatternErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_PATTERN);
				for (Field field : reqs) {
					String errorString = LanguageUtil.get(user,"message.contentlet.format");
					errorString = errorString.replace("{0}", field.getFieldName());
					saveContentErrors.add(errorString);
				}
			}
			
			if(ve.hasRelationshipErrors()){
				StringBuffer sb = new StringBuffer("<br>");
				Map<String,Map<Relationship,List<Contentlet>>> notValidRelationships = ve.getNotValidRelationship();
				Set<String> auxKeys = notValidRelationships.keySet();
				for(String key : auxKeys)
				{
					String errorMessage = "";
					if(key.equals(DotContentletValidationException.VALIDATION_FAILED_REQUIRED_REL))
					{
						errorMessage = "<b>Required Relationship</b>";
					}
					else if(key.equals(DotContentletValidationException.VALIDATION_FAILED_INVALID_REL_CONTENT))
					{
						errorMessage = "<b>Invalid Relationship-Contentlet</b>";
					}
					else if(key.equals(DotContentletValidationException.VALIDATION_FAILED_BAD_REL))
					{
						errorMessage = "<b>Bad Relationship</b>";
					}

					sb.append(errorMessage + ":<br>");
					Map<Relationship,List<Contentlet>> relationshipContentlets = notValidRelationships.get(key);			
				
					for(Entry<Relationship,List<Contentlet>> relationship : relationshipContentlets.entrySet())
					{			
						sb.append(relationship.getKey().getRelationTypeValue() + ", ");
					}					
					sb.append("<br>");			
				}
				sb.append("<br>");

				//need to update message to support multiple relationship validation errors
				String errorString = LanguageUtil.get(user,"message.relationship.required_ext");
				errorString = errorString.replace("{0}", sb.toString());
				saveContentErrors.add(errorString);				
			}
			
			if(ve.hasUniqueErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_UNIQUE);
				for (Field field : reqs) {
					String errorString = LanguageUtil.get(user,"message.contentlet.unique");
					errorString = errorString.replace("{0}", field.getFieldName());
					saveContentErrors.add(errorString);
				}
			}
			
			if(ve.getMessage().contains("The content form submission data id different from the content which is trying to be edited")){
				String errorString = LanguageUtil.get(user,"message.contentlet.invalid.form");			
				saveContentErrors.add(errorString);
			}
			
		}
		
		catch(DotSecurityException dse){			
			String errorString = LanguageUtil.get(user,"message.insufficient.permissions.to.save");			
			saveContentErrors.add(errorString);
			
		}
		
		catch (Exception e) {			
			if(e.getMessage().equals(Constants.COMMON_ERROR)){			
				String errorString = LanguageUtil.get(user,"message.contentlet.save.error");			
				saveContentErrors.add(errorString);		
				SessionMessages.clear(req.getSession());
			}
			
		}
		
		finally{			
			if(!isAutoSave
				&&(saveContentErrors != null 
						&& saveContentErrors.size() > 0)){
					callbackData.put("saveContentErrors", saveContentErrors);
					SessionMessages.clear(req.getSession());				
			}				
		}
		
		if(InodeUtils.isSet(newInode))
			callbackData.put("contentletInode", newInode);		
		
		if(!isAutoSave  
				&&(saveContentErrors == null 
						|| saveContentErrors.size() == 0)){
			
			Logger.debug(this, "AFTER PUBLISH LANGUAGE=" + language);
			
			if (UtilMethods.isSet(language) && referer.indexOf("language") > -1) {
				Logger.debug(this, "Replacing referer language=" + referer);
				referer = referer.replaceAll("language=([0-9])*", com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE+"=" + language);
				Logger.debug(this, "Referer after being replaced=" + referer);				
			}
		}
		
		boolean savingRecurrence = false;
		callbackData.put("referer", referer);	
		HibernateUtil.commitTransaction();
		if(UtilMethods.isSet(newInode) && !savingRecurrence){
			if(!contAPI.isInodeIndexed(newInode)){
				Logger.error(this, "Timed out while waiting for index to return");
			}
		}
		return callbackData;
	}
	
	public Map<String, Object> disconnectEvent(String inode,String startDateStr, String endDateStr) throws DotRuntimeException, PortalException, SystemException, DotDataException, DotSecurityException, java.text.ParseException{

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		Map<String, Object> eventMap = new HashMap<String, Object>();
		List<String> disconnectEventErrors = new ArrayList<String>();

		String inodeStr = (InodeUtils.isSet(inode) ? inode : "");
		Contentlet contentlet = new Contentlet();
		if(InodeUtils.isSet(inodeStr)){			
			contentlet = contAPI.find(inodeStr, user, false);
		}
		if(InodeUtils.isSet(contentlet.getInode())) {
			Event ev = null;
			try{
				ev = eventAPI.find(contentlet.getIdentifier(), false, user, respectFrontendRoles);
			}catch(Exception e){
				disconnectEventErrors.add(e.getLocalizedMessage());
			}finally{
				if(disconnectEventErrors.size() > 0){
					eventMap.put("disconnectEventErrors", disconnectEventErrors);								
				}				
			}

			if(ev!=null){
				Date startDate = null;
				Date endDate = null;

				try{
					if(UtilMethods.isSet(startDateStr)){
						String date = dateFormat.format(dateFormat2.parse(startDateStr));
						startDate =  dateFormat.parse(date);
					}
					if(UtilMethods.isSet(endDateStr)){
						String date = dateFormat.format(dateFormat2.parse(endDateStr));
						endDate = dateFormat.parse(date);
					}

				}catch(java.text.ParseException pe){
					disconnectEventErrors.add(pe.getLocalizedMessage());
				}finally{
					if(disconnectEventErrors.size() > 0){
						eventMap.put("disconnectEventErrors", disconnectEventErrors);								
					}				
				}

				if(startDate!=null && endDate!=null){
					Calendar originalStartDate = Calendar.getInstance();
					Calendar originalEndDate = Calendar.getInstance();
					Calendar newStartDate = Calendar.getInstance();
					Calendar newEndDate = Calendar.getInstance();
					originalStartDate.setTime(ev.getStartDate());
					originalEndDate.setTime(ev.getEndDate());
					newStartDate.setTime(startDate);
					newEndDate.setTime(endDate);

					originalStartDate.set(Calendar.YEAR, newStartDate.get(Calendar.YEAR));
					originalStartDate.set(Calendar.MONTH, newStartDate.get(Calendar.MONTH));
					originalStartDate.set(Calendar.DAY_OF_MONTH, newStartDate.get(Calendar.DAY_OF_MONTH));

					originalEndDate.set(Calendar.YEAR, newEndDate.get(Calendar.YEAR));
					originalEndDate.set(Calendar.MONTH, newEndDate.get(Calendar.MONTH));
					originalEndDate.set(Calendar.DAY_OF_MONTH, newEndDate.get(Calendar.DAY_OF_MONTH));

					Event newEvent = null;

					try{
						boolean autoCom = false;
						try{
							autoCom =	 DbConnectionFactory.getConnection().getAutoCommit();
						}
						catch(Exception e){
							throw new DotDataException(e.getMessage());
						}
						if(autoCom){
							HibernateUtil.startTransaction();
						}
						newEvent = eventAPI.disconnectEvent(ev, user, originalStartDate.getTime(), originalEndDate.getTime());
						eventMap = newEvent.getMap();
						//Loading categories
						List<Map<String, Object>> categoryMaps = new ArrayList<Map<String,Object>>();
						List<Category> eventCategories =  categoryAPI.getParents(newEvent, user, respectFrontendRoles);
						for(Category cat : eventCategories) {
							categoryMaps.add(cat.getMap());
						}
						eventMap.put("categories", categoryMaps);
						eventMap.put("rating", RatingAPI.getAverageRating(newEvent.getIdentifier()));
						eventMap.put("votes", RatingAPI.getRatingVotesNumber(newEvent.getIdentifier()));
						eventMap.put("hasReadPermission", perAPI.doesUserHavePermission(newEvent, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles));
						eventMap.put("hasWritePermission", perAPI.doesUserHavePermission(newEvent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles));
						eventMap.put("hasPublishPermission", perAPI.doesUserHavePermission(newEvent, PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles));
						eventMap.put("readPermission", perAPI.doesUserHavePermission(newEvent, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles));
						eventMap.put("writePermission", perAPI.doesUserHavePermission(newEvent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles));
						eventMap.put("publishPermission", perAPI.doesUserHavePermission(newEvent, PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles));
						eventMap.put("isDisconnected", UtilMethods.isSet(newEvent.getDisconnectedFrom()));
						CommentsWebAPI cAPI = new CommentsWebAPI();
						cAPI.setUser(user);
						cAPI.setRespectFrontendRoles(respectFrontendRoles);
						eventMap.put("commentsCount", cAPI.getCommentsCount(newEvent.getInode()));
						HibernateUtil.commitTransaction();
					}catch(Exception e){
						HibernateUtil.rollbackTransaction();
						disconnectEventErrors.add(e.getLocalizedMessage());
					}finally{
						if(disconnectEventErrors.size() > 0){
							eventMap.put("disconnectEventErrors", disconnectEventErrors);								
						}				
					}
				}
			}
		}

		return eventMap;
	}
	

}
