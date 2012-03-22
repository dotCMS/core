package com.dotmarketing.portlets.calendar.viewtools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * 
 * @author david
 *
 */
public class CalendarWebAPI implements ViewTool {
	
	private HttpServletRequest request;
	Context ctx;


    private EventAPI eventAPI =  APILocator.getEventAPI();
    private UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
    private CategoryAPI categoryAPI = APILocator.getCategoryAPI();

	/**
	 * 
	 */
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
	}
	
	/**
	 * 
	 * @param parentEvent
	 * @param fromDate
	 * @param toDate
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws PortalException
	 * @throws SystemException
	 */
	public List<Event> findRelatedEvents (String parentEvent, Date fromDate, Date toDate) 
		throws DotDataException, DotSecurityException, PortalException, SystemException {
		
		Logger.debug(this, "findRelatedEvents: parentEvent = " + parentEvent + ", fromDate: " + fromDate + ", toDate = " + toDate);
		
		//Retrieving the current user
		User user = PortalUtil.getUser(request);
		boolean respectCMSAnon = false;
		if(user == null) {
			//Assuming is a front-end access
			respectCMSAnon = true;
			user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
		}
		
		List<Event> eventsList = new ArrayList<Event>();
		Event parentEv = eventAPI.find(parentEvent, true, user, respectCMSAnon);
		List<Event> events = eventAPI.findRelatedEvents(parentEv, fromDate, toDate, true, user, respectCMSAnon);
		for(Event ev : events) {
			eventsList.add(ev);
		}
		return eventsList;
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotSecurityException
	 * @throws PortalException
	 * @throws SystemException
	 */
	
	public Event find(String id)throws DotDataException, DotSecurityException, 
	                  DotSecurityException, PortalException, SystemException {
		User user = PortalUtil.getUser(request);
		boolean respectCMSAnon = false;
		if(user == null) {
			//Assuming is a front-end access
			respectCMSAnon = true;
			user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
		}
		Event event = eventAPI.find(id, true, user, respectCMSAnon);
		return event;	
	}
	
	public List<Event> findEvents (String hostId, Date fromDate, Date toDate, String tag, 
			String keyword, String categoryInode,final String sortBy, int offset, int limit) 
			throws DotDataException, DotSecurityException, PortalException, SystemException {
		User user = PortalUtil.getUser(request);
		boolean respectCMSAnon = false;
		if(user == null) {
			//Assuming is a front-end access
			respectCMSAnon = true;
			user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
		}
		List<Category> categories = new ArrayList<Category>();
		if(UtilMethods.isSet(categoryInode)) {
				Category cat = categoryAPI.find(categoryInode, user, respectCMSAnon);
				if(cat != null)
					categories.add(cat);
		}
		String[] tags = null;
		if(UtilMethods.isSet(tag)){
			tags = new String[1];
			tags[0] = tag;
		}
		String[] keywords = null;
		if(UtilMethods.isSet(keyword)){
			keywords = new String[1];
			keywords[0] = keyword;
		}
		List<Event> toReturn = eventAPI.find(hostId, fromDate, toDate, tags, keywords, categories, true, false, offset, limit, user, respectCMSAnon);
		if(UtilMethods.isSet(sortBy)){
			final boolean isDesc = sortBy.contains("desc");
			Collections.sort(toReturn, new Comparator<Event>(){
				public int compare(Event event1, Event event2) {
					if(sortBy.contains("startDate")){
						if(isDesc){
							return event2.getStartDate().compareTo(event1.getStartDate());
						}
						return event1.getStartDate().compareTo(event2.getStartDate());
					}else if(sortBy.contains("endDate")){
						if(isDesc){
							return event2.getEndDate().compareTo(event2.getEndDate());
						}
						return event1.getEndDate().compareTo(event2.getEndDate());
					}
					return 0;
				}
			});
		}
        return toReturn;		
	}
	
		
}