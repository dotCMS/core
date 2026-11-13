package com.dotmarketing.portlets.calendar.ajax;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rendering.velocity.viewtools.CommentsWebAPI;
import com.dotcms.rendering.velocity.viewtools.DateViewWebAPI;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

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
		List<Map<String, Object>> categoryMaps = new ArrayList<>();
		List<Category> eventCategories =  categoryAPI.getParents(ev, user, respectFrontendRoles);
		for(Category cat : eventCategories) {
			categoryMaps.add(cat.getMap());
		}
		eventMap.put("categories", categoryMaps);

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

		List<Map<String, Object>> retList = new ArrayList<>();
		List<Category> categories = new ArrayList<>();
		if(categoriesInodes != null) {
			for (String categoryInode : categoriesInodes) {
				Category cat = categoryAPI.find(categoryInode, user, respectFrontendRoles);
				if(cat != null)
					categories.add(cat);
			}
		}
		List<Event> events = eventAPI.find(hostId, fromDate, toDate, tags, keywords, categories, live, includeArchived, offset, limit, user, respectFrontendRoles);
		for(Event ev : events) {
			ev.setTags();
			Map<String, Object> eventMap = ev.getMap();

			//Loading categories
			List<Map<String, Object>> categoryMaps = new ArrayList<>();
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
			eventMap.put("inode", ev.getInode());
			eventMap.put("recurr", ev.isRecurrent());
			final String eventTags = ev.getTags();
			eventMap.put("tags", UtilMethods.isSet(eventTags) ? eventTags : StringPool.BLANK);
			ev.setIdentifier(origIdent);
			
			eventMap.put("categories", categoryMaps);

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
		
		List<Map<String, Object>> retList = new ArrayList<>();
		Event parentEv = eventAPI.find(parentEvent, live, user, respectFrontendRoles);
		List<Event> events = eventAPI.findRelatedEvents(parentEv, fromDate, toDate, live, user, respectFrontendRoles);
		for(Event ev : events) {
			retList.add(ev.getMap());
		}
		return retList;
	}

	@WrapInTransaction
	public void publishEvent (final String inode) throws PortalException, SystemException, DotDataException, DotSecurityException {

		final WebContext ctx = WebContextFactory.get();
		final HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		final User user = userAPI.getLoggedInUser(request);
		final boolean respectFrontendRoles = true;
		final Event ev  = eventAPI.findbyInode(inode, user, respectFrontendRoles);

		try {

			ev.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());
			contAPI.publish(ev, user, respectFrontendRoles);
		} catch(Exception e) {
			Logger.error(this, e.getMessage());
		}
	}

	@WrapInTransaction
	public Map<String,Object> unpublishEvent (final String inode) throws PortalException, SystemException, DotDataException, DotSecurityException {

		final Map<String,Object> callbackData   = new HashMap<>();//DOTCMS-5199
		final List<String> eventUnpublishErrors = new ArrayList<>();
		final WebContext ctx 					= WebContextFactory.get();
		final HttpServletRequest request 		= ctx.getHttpServletRequest();

		//Retrieving the current user
		final User user = userAPI.getLoggedInUser(request);
		final boolean respectFrontendRoles = true;
		final Event ev = eventAPI.findbyInode(inode, user, respectFrontendRoles);

		try {

			ev.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());
			contAPI.unpublish(ev, user, respectFrontendRoles);  
		} catch (DotSecurityException | DotDataException | DotContentletStateException e) {
			eventUnpublishErrors.add(e.getLocalizedMessage());
		} finally{
			if(eventUnpublishErrors.size() > 0) {
				callbackData.put("eventUnpublishErrors", eventUnpublishErrors);								
			}				
		}

		return callbackData;
	}

	@WrapInTransaction
	public void archiveEvent (final String inode) throws PortalException, SystemException, DotDataException, DotSecurityException {

		final WebContext ctx = WebContextFactory.get();
		final HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		final User user = userAPI.getLoggedInUser(request);
		final boolean respectFrontendRoles = true;

		final Event ev = eventAPI.findbyInode(inode, user, respectFrontendRoles);
		try {
			ev.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());
			contAPI.archive(ev, user, respectFrontendRoles);
		}catch(Exception e){
			Logger.error(this, e.getMessage());
		}
	}

	@WrapInTransaction
	public void archiveDisconnectedEvent (final String inode, final boolean putBack) throws PortalException, SystemException, DotDataException, DotSecurityException {
		final WebContext ctx = WebContextFactory.get();
		final HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		final User user = userAPI.getLoggedInUser(request);
		final boolean respectFrontendRoles = true;

		final Event ev = eventAPI.findbyInode(inode, user, respectFrontendRoles);
		ev.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());

		if(putBack) {

			Event baseEvent = null;
			try {
			   baseEvent =eventAPI.find(ev.getDisconnectedFrom(), false, user, respectFrontendRoles);
			} catch(Exception e){
				Logger.error(this, "Base event not found");
			}

			if(baseEvent!=null) {
				try {
				  final Date originalStartDate = ev.getOriginalStartDate();
				  baseEvent.deleteDateToIgnore(originalStartDate);
				  APILocator.getContentletAPI().checkin(baseEvent, categoryAPI.getParents(baseEvent, user, true), perAPI.getPermissions(baseEvent), user, false);
				} catch(Exception e){
					Logger.error(this, "Could not put back event in recurrence");
				}
			}
		}

		contAPI.archive(ev, user, respectFrontendRoles);
	}

	
	
	
    @WrapInTransaction
	public void unarchiveEvent (final String inode) throws PortalException, SystemException, DotDataException, DotSecurityException {
		final WebContext ctx = WebContextFactory.get();
		final HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		final User user = userAPI.getLoggedInUser(request);
		final boolean respectFrontendRoles = true;

		final Event ev = eventAPI.findbyInode(inode, user, respectFrontendRoles);
		try{

			ev.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());

			if(UtilMethods.isSet(ev.getDisconnectedFrom())) {
				Event baseEvent = null;
				try {
				   baseEvent =eventAPI.find(ev.getDisconnectedFrom(), false, user, respectFrontendRoles);
				} catch(Exception e){
					Logger.error(this, "Base event not found");
				}

				if(baseEvent!=null) {
					try {
						final Date originalStartDate = ev.getOriginalStartDate();
						baseEvent.addDateToIgnore(originalStartDate);
						APILocator.getContentletAPI().checkin(baseEvent, categoryAPI.getParents(baseEvent, user, true), perAPI.getPermissions(baseEvent), user, false);
					} catch(Exception e){
						Logger.error(this, "Could not delete event from recurrence");
					}
				}
			}

			contAPI.unarchive(ev, user, respectFrontendRoles);
			
		} catch(Exception e){
			Logger.error(this, e.getMessage());
		}
	}



	public Map<String,Object> deleteEvent (String identifier) throws PortalException, SystemException, DotDataException, DotSecurityException {
		HibernateUtil.startTransaction();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		List<String> eventDeleteErrors = new ArrayList<>();
		Map<String,Object> callbackData = new HashMap<>();

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
		   HibernateUtil.closeAndCommitTransaction();
		}

        //At this point we already deleted the content from the index on the delete call
		/*if(!contAPI.isInodeIndexed(ev.getInode())){
			Logger.error(this, "Timed out while waiting for index to return");
		}*/
		
		return callbackData;
	}		
	
	
	public Map<String, Object> disconnectEvent(String inode,String startDateStr, String endDateStr) throws DotRuntimeException, PortalException, SystemException, DotDataException, DotSecurityException, java.text.ParseException{

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = true;

		Map<String, Object> eventMap = new HashMap<>();
		List<String> disconnectEventErrors = new ArrayList<>();

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
							throw new DotDataException(e.getMessage(),e);
						}
						if(autoCom){
							HibernateUtil.startTransaction();
						}
						newEvent = eventAPI.disconnectEvent(ev, user, originalStartDate.getTime(), originalEndDate.getTime());
						eventMap = newEvent.getMap();
						//Loading categories
						List<Map<String, Object>> categoryMaps = new ArrayList<>();
						List<Category> eventCategories =  categoryAPI.getParents(newEvent, user, respectFrontendRoles);
						for(Category cat : eventCategories) {
							categoryMaps.add(cat.getMap());
						}
						eventMap.put("categories", categoryMaps);
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
						HibernateUtil.closeAndCommitTransaction();
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
