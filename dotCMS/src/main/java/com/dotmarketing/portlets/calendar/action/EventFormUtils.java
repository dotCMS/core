package com.dotmarketing.portlets.calendar.action;

import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.contentlet.struts.EventAwareContentletForm;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Struts controller that handle portlet actions to save/publish/un-publish/archive and delete
 * events
 * @author David Torres
 */

public class EventFormUtils {


	private static String[] daysOfWeekRecurrence = { String.valueOf(Calendar.MONDAY), String.valueOf(Calendar.TUESDAY),
			String.valueOf(Calendar.WEDNESDAY),	String.valueOf(Calendar.THURSDAY), String.valueOf(Calendar.FRIDAY),
			String.valueOf(Calendar.SATURDAY),  String.valueOf(Calendar.SUNDAY) };

	/**
	 *
	 * @param eventForm
	 * @return
	 */
    public static EventAwareContentletForm setEventDefaults(final EventAwareContentletForm eventForm){

		eventForm.setRecurrenceStartsDate(new Date());
		eventForm.setRecurrenceEndsDate(new Date());
		eventForm.setNoEndDate(false);
		eventForm.setRecurrenceDayOfMonth("");
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(new Date());
		eventForm.setRecurrenceWeekOfMonth(cal.get(Calendar.WEEK_OF_MONTH));
		eventForm.setRecurrenceDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
		eventForm.setRecurrenceMonthOfYear(cal.get(Calendar.MONTH)+1);
		eventForm.setRecurrenceDaysOfWeek(daysOfWeekRecurrence);
		eventForm.setRecurrenceInterval(1);
		eventForm.setRecurrenceIntervalDaily(1);
		eventForm.setRecurrenceIntervalWeekly(1);
		eventForm.setRecurrenceIntervalMonthly(1);
		eventForm.setRecurrenceIntervalYearly(1);
		eventForm.setSpecificDayOfMonthRecY("1");
		eventForm.setSpecificMonthOfYearRecY("1");
		eventForm.setRecurrenceOccurs("never");

        return eventForm;
    }

	/**
	 *
	 * @param event
	 * @param eventForm
	 * @return
	 */
	public static EventAwareContentletForm editEvent(final Event event, final EventAwareContentletForm eventForm)  {
		if(!UtilMethods.isSet(event.getInode()))
			return eventForm;

		eventForm.getMap().put("startDate",event.getStartDate());
		eventForm.getMap().put("endDate",event.getEndDate());

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
			List<String> daysList = new ArrayList<>();
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
				if(event.getRecurrenceDayOfMonth() >0){
					eventForm.setRecurrenceDayOfMonth(String.valueOf(event.getRecurrenceDayOfMonth()));
					eventForm.setSpecificDate(true);
				}else{
					eventForm.setSpecificDate(false);
					eventForm.setRecurrenceMonthOfYear(event.getRecurrenceMonthOfYear());
					eventForm.setRecurrenceWeekOfMonth(event.getRecurrenceWeekOfMonth());
					eventForm.setRecurrenceDayOfMonth("");
				}
			}else if(event.getOccursEnum() == Event.Occurrency.ANNUALLY){
				if(event.getRecurrenceDayOfMonth() >0){
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
		    /*
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
			*/
			setEventDefaults(eventForm);
		}
		return eventForm;
	}


}