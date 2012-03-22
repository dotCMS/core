package com.dotmarketing.portlets.calendar.action;

import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

/**
 * 
 * Action that let you download an event from the frontend in vcalendar format
 * check the struts-cms.xml to see how this action is mapped to struts
 * 
 * @author Salvador Di Nardo
 * 
 */
public class SendVCalendarDetail extends DispatchAction {
	
	EventAPI eventAPI = null;
	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	
	public SendVCalendarDetail() 
	{
		eventAPI = APILocator.getEventAPI();
	}	
	
	/**
	 * This is the default method. Get the email info and the event inode to create the body and send the email.
	 * @param	mapping ActionMapping.
	 * @param	lf ActionForm.
	 * @param	request HttpServletRequest.
	 * @param	response HttpServletResponse.
	 * @return	ActionForward.
	 * @exception	Exception.
	 */
	public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response) throws Exception 
	{
		HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
		try
		{
			String inode = request.getParameter("id");
			String recurrenceStartDate = request.getParameter("recurrenceStartDate");
			String recurrenceEndDate = request.getParameter("recurrenceEndDate");
			Date startDate = null;
			Date endDate = null;
			
			if(UtilMethods.isSet(recurrenceStartDate)){
				startDate = dateFormat.parse(recurrenceStartDate);
			}
			
			if(UtilMethods.isSet(recurrenceEndDate)){
			
				endDate = dateFormat.parse(recurrenceEndDate);
			}
			User user = PortalUtil.getUser(request);
			boolean respectCMSAnon = false;
			if(user == null) 
			{
				//Assuming is a front-end access
				respectCMSAnon = true;
				user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
			}
			Event event = eventAPI.find(inode, true, user, respectCMSAnon);
			Host host = hostWebAPI.getCurrentHost(request);
			String iCalEvent = eventAPI.createVCalendarInfo(event, startDate, endDate, host);
						
			//response.setContentType("application/octet-stream");
			response.setContentType("text/calendar");			
			response.setHeader("Content-Disposition", "attachment; filename=\"" + event.getTitle() + ".ics\"");
			OutputStreamWriter out = new OutputStreamWriter(response.getOutputStream(), response.getCharacterEncoding());
			out.write(iCalEvent);
		
			out.flush();
			out.close();
		} catch (Exception e) {
			Logger.warn(this, e.toString());
		}		
		ActionForward af = null;
		return af;
	}	
}