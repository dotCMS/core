package com.dotmarketing.portlets.calendar.cms.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.calendar.business.CalendarReminderAPI;
import com.dotmarketing.portlets.calendar.struts.CalendarReminderForm;
import com.dotmarketing.util.UtilMethods;

public class CalendarReminderAction extends DispatchAction {
	
	CalendarReminderAPI CRAI = APILocator.getCalendarReminderAPI();
	
	public ActionForward unspecified(ActionMapping rMapping, ActionForm form,HttpServletRequest request, HttpServletResponse response) throws Exception 
	{
		CalendarReminderForm calendarReminderForm = (CalendarReminderForm) form;
		
		String emailAddress = calendarReminderForm.getEmailAddress();
		String firstName = calendarReminderForm.getFirstName();
		String lastName = calendarReminderForm.getLastName();
		String eventId = calendarReminderForm.getEventId();
		int daysInAdvance = calendarReminderForm.getDaysInAdvance();
		
		CRAI.create(emailAddress,firstName,lastName,eventId,daysInAdvance);
		
		if(UtilMethods.isSet(request.getParameter("returnURL")))
		{
			ActionForward af = new ActionForward(request.getParameter("returnURL"));
			af.setRedirect(true);
			return af;
		}
		else
		{
			ActionForward af = rMapping.findForward("thankYouPage");
			return af;
		}
	}
}
