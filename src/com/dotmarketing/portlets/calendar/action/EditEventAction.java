package com.dotmarketing.portlets.calendar.action;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.calendar.struts.EventForm;
import com.dotmarketing.portlets.contentlet.action.EditContentletAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;

/**
 * Struts controller that handle portlet actions to save/publish/un-publish/archive and delete
 * events
 * @author David Torres
 */

public class EditEventAction extends EditContentletAction {

	EventAPI eventAPI;
	ContentletAPI conAPI;


	/**
	 * Main method called by the portlet passing the cmd parameter 
	 * to discriminate what action to execute
	 */
	@SuppressWarnings("unchecked")
	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
	throws Exception {

		eventAPI = APILocator.getEventAPI();
		conAPI = APILocator.getContentletAPI();

		//The super handles all the operations to save/edit/publish/delete/... contentlet
		super.processAction(mapping, form, config, req, res);


		User user = _getUser(req);

		/**
		 * Specific commands handlers for custom recurrence 
		 */
		String cmd = req.getParameter(Constants.CMD);

		retrieveEvent(mapping, form, config, req, res, cmd, user);

		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			Logger.debug(this, "Calling Edit Method");
			editEvent(mapping, form, config, req, res, user);
		}

		if ((cmd != null) && cmd.equals("edit_recurrent")) {
			Logger.debug(this, "Calling Edit Recurrent Event Method");
			editEvent(mapping, form, config, req, res, user);
		}

		if ((cmd != null) && cmd.equals(Constants.ADD)) { 
			ActionErrors errors = (ActionErrors) req.getAttribute(Globals.ERROR_KEY);

			if(errors == null || errors.size() == 0)
				saveEvent(mapping, form, config, req, res, user);

		}

	}

	/**
	 * Retrieves the event object from storage
	 * @param mapping
	 * @param form
	 * @param config
	 * @param req
	 * @param res
	 * @param user
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws ParseException 
	 * @throws NumberFormatException 
	 */
	private void retrieveEvent (ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res, String command, User user) throws DotDataException, DotSecurityException, ParseException  {
		Contentlet contentlet = (Contentlet) req.getAttribute(WebKeys.CONTENTLET_EDIT);
		if(InodeUtils.isSet(contentlet.getInode())) {
			Event ev = eventAPI.find(contentlet.getIdentifier(), false, user, false);
			req.setAttribute(WebKeys.EVENT_EDIT, ev);

		} else {
			EventForm eventForm = (EventForm) form;
			eventForm.setRecurrenceEndsDate(new Date ());
			eventForm.setRecurrenceOccurs("never");
			eventForm.setRecurrenceDayOfMonth("");
			eventForm.setNoEndDate(false);
			eventForm.setRecurrenceInterval(1);
			eventForm.setRecurrenceIntervalDaily(1);
			eventForm.setRecurrenceIntervalWeekly(1);
			eventForm.setRecurrenceIntervalMonthly(1);
			eventForm.setRecurrenceIntervalYearly(1);
			String[] daysOfWeekRecurrence = { String.valueOf(Calendar.MONDAY), String.valueOf(Calendar.TUESDAY), 
					String.valueOf(Calendar.WEDNESDAY),	String.valueOf(Calendar.THURSDAY), String.valueOf(Calendar.FRIDAY), 
					String.valueOf(Calendar.SATURDAY),  String.valueOf(Calendar.SUNDAY) };
			eventForm.setRecurrenceDaysOfWeek(daysOfWeekRecurrence);
			eventForm.setRecurrenceDayOfWeek(1);
			eventForm.setRecurrenceMonthOfYear(1);
			eventForm.setRecurrenceWeekOfMonth(1);
			eventForm.setSpecificDayOfMonthRecY("1");
			eventForm.setSpecificMonthOfYearRecY("1");
			eventForm.setSpecificDate(false);
			req.setAttribute(WebKeys.EVENT_EDIT, new Event());
		}
	}

	/**
	 * Retrieves the event object from storage
	 * @param mapping
	 * @param form
	 * @param config
	 * @param req
	 * @param res
	 * @param user
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws NumberFormatException 
	 */
	private void editEvent (ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res, User user)  {
		EventForm eventForm = (EventForm) form;
		Event event = (Event) req.getAttribute(WebKeys.EVENT_EDIT);
		if (event.getRecurs()) {
			eventForm.setRecurrenceStartsDate(event.getRecurrenceStart());
			eventForm.setRecurrenceDayOfMonth(String.valueOf(event.getRecurrenceDayOfMonth()));
			eventForm.setRecurrenceDayOfWeek(event.getRecurrenceDayOfWeek());

			if(UtilMethods.isSet(event.getRecurrenceEnd())){
				eventForm.setRecurrenceEndsDate(event.getRecurrenceEnd());
				eventForm.setNoEndDate(false);
			}else{
				eventForm.setNoEndDate(true);	
			}
			String daysOfWeek = event.getRecurrenceDaysOfWeek();
			if(daysOfWeek == null)
				daysOfWeek = "";
			List<String> daysList = new ArrayList<String>();
			for(String day : daysOfWeek.split(",")) {
				if(UtilMethods.isSet(day)) {
					daysList.add(day);
				}
			}
			String[] daysListArr = new String[daysList.size()];
			for(int i = 0 ; i < daysList.size(); i++)
				daysListArr[i] = daysList.get(i);
			eventForm.setRecurrenceDaysOfWeek(daysListArr);
			eventForm.setRecurrenceInterval((int) event.getRecurrenceInterval());
			if(event.getOccursEnum() == Event.Occurrency.DAILY){
				eventForm.setRecurrenceOccurs("daily");
			}else if(event.getOccursEnum() == Event.Occurrency.WEEKLY){
				eventForm.setRecurrenceOccurs("weekly");
			 }else if(event.getOccursEnum() == Event.Occurrency.MONTHLY){
					eventForm.setRecurrenceOccurs("monthly");
					if(UtilMethods.isSet(event.getRecurrenceDayOfMonth()) && event.getRecurrenceDayOfMonth() >0){
						eventForm.setRecurrenceDayOfMonth(String.valueOf(event.getRecurrenceDayOfMonth()));		
						eventForm.setSpecificDate(true);
					}else{
						eventForm.setSpecificDate(false);
						eventForm.setRecurrenceMonthOfYear(event.getRecurrenceMonthOfYear());
						eventForm.setRecurrenceWeekOfMonth(event.getRecurrenceWeekOfMonth());
						eventForm.setRecurrenceDayOfMonth("");
					}
		        }else if(event.getOccursEnum() == Event.Occurrency.ANNUALLY){
	                if(UtilMethods.isSet(event.getRecurrenceDayOfMonth()) && event.getRecurrenceDayOfMonth() >0){
	                	eventForm.setSpecificDayOfMonthRecY(String.valueOf(event.getRecurrenceDayOfMonth()));	
	                	eventForm.setSpecificMonthOfYearRecY(String.valueOf(event.getRecurrenceMonthOfYear()));
	                	eventForm.setSpecificDate(true);
					}else{
						eventForm.setSpecificDate(false);
						eventForm.setSpecificDayOfMonthRecY("");
						eventForm.setSpecificMonthOfYearRecY("");
						eventForm.setRecurrenceMonthOfYear(event.getRecurrenceMonthOfYear());
						eventForm.setRecurrenceWeekOfMonth(event.getRecurrenceWeekOfMonth());
					}
					eventForm.setRecurrenceOccurs("annually");
		        }

		} else {
			eventForm.setRecurrenceStartsDate(new Date());
			eventForm.setRecurrenceEndsDate(new Date());
			eventForm.setNoEndDate(false);
			eventForm.setRecurrenceDayOfMonth("");
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(new Date());
			eventForm.setRecurrenceWeekOfMonth(cal.get(Calendar.WEEK_OF_MONTH));
			eventForm.setRecurrenceDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
			eventForm.setRecurrenceMonthOfYear(cal.get(Calendar.MONTH)+1);

			String[] daysOfWeekRecurrence = { String.valueOf(Calendar.MONDAY), String.valueOf(Calendar.TUESDAY), 
					String.valueOf(Calendar.WEDNESDAY),	String.valueOf(Calendar.THURSDAY), String.valueOf(Calendar.FRIDAY),
					String.valueOf(Calendar.SATURDAY),  String.valueOf(Calendar.SUNDAY) };

			eventForm.setRecurrenceDaysOfWeek(daysOfWeekRecurrence);
			eventForm.setRecurrenceInterval(1);
			eventForm.setRecurrenceOccurs("never");
		}
	}


	/**
	 * Copies the event changes from the form to the event object and saves it
	 * @param mapping
	 * @param form
	 * @param config
	 * @param req
	 * @param res
	 * @param user
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	private void saveEvent (ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res, User user) throws DotDataException, DotSecurityException {
		HibernateUtil.startTransaction();
		HttpServletRequest request = ((ActionRequestImpl) req).getHttpServletRequest();

		EventForm eventForm = (EventForm) form;
		Event event = (Event) req.getAttribute(WebKeys.EVENT_EDIT);
	}
}