package com.dotmarketing.portlets.calendar.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.calendar.model.CalendarReminder;
import com.dotmarketing.util.Logger;

public class CalendarReminderFactoryImpl implements CalendarReminderFactory 
{
	//Gets
	public CalendarReminder getCalendarReminder(long userId, String eventId,Date sendDate)
	{
		
		/**
		 * Find an CalenderReminder, based on the user, event and date of the reminder
		 * @param userId userId of the CalendarReminder to retrieve
		 * @param eventId eventId of the CalendarReminder to retrieve
		 * @param sendDate Date of the CalendarReminder to retrieve
		 * @return
		 */
		CalendarReminder returnValue = new CalendarReminder();
		try {
			String query = "from calendar_reminder in class com.dotmarketing.portlets.calendar.model.CalendarReminder where user_id = ? and event_id = ? and send_date = ?";			
			HibernateUtil dh = new HibernateUtil(CalendarReminder.class);
			dh.setQuery(query);
			dh.setParam(userId);
			dh.setParam(eventId);
			dh.setParam(eventId);
			dh.setParam(sendDate);
			returnValue = (CalendarReminder) dh.load();			
		} catch (Exception e) {
			Logger.warn(CalendarReminderFactoryImpl.class, "getCalendarReminder failed:" + e.getMessage(), e);
		}
		finally
		{
			return returnValue;
		}
	}
	
	/**
	 * Retrieve all the CalendarReminders before the specified date
	 * @param sendDate The Date used to retrieve the CalendarReminders
	 * @return
	 */
	public List<CalendarReminder> getCalendarReminderBefore(Date sendDate)
	{
		List<CalendarReminder> returnValue = new ArrayList<CalendarReminder>();
		try
		{			
			String query = "from calendar_reminder in class com.dotmarketing.portlets.calendar.model.CalendarReminder where send_date <= ?";
			HibernateUtil dh = new HibernateUtil(CalendarReminder.class);
			dh.setQuery(query);
			dh.setParam(sendDate);			
			returnValue = (List<CalendarReminder>) dh.list();
		}
		catch(Exception ex)
		{
			Logger.warn(CalendarReminderFactoryImpl.class, "getCalendareReminder failed:" + ex.getMessage(), ex);
		}
		finally
		{
			return returnValue;
		}		
	}
	
	/**
	 * Return all the CalendarReminders of the system
	 * @return
	 * @throws DotDataException
	 */
	public List<CalendarReminder> getAll()
	{
		List<CalendarReminder> returnValue = new ArrayList<CalendarReminder>();
		try
		{
			String query = "from calendar_reminder in class com.dotmarketing.portlets.calendar.model.CalendarReminder";
			HibernateUtil dh = new HibernateUtil(CalendarReminder.class);
			dh.setQuery(query);
			returnValue = (List<CalendarReminder>) dh.list();			
		}
		catch(Exception ex)
		{
			
		}
		finally
		{
			return returnValue;
		}
	}
	
	
	//Saves
	/**
	 * Save a CalendarReminder to the DB
	 */
	public void saveCalendarReminder(CalendarReminder calendarReminder)
	{	
		try {
			HibernateUtil.save(calendarReminder);
		} catch (DotHibernateException e) {
			Logger.error(CalendarReminderFactoryImpl.class, "saveCalendarReminder failed:" + e, e);
		}
	}
	
	//Delete
	/**
	 * Delete a List of Calendar Reminder
	 * @param calendarReminders List of CalendarReminder to delete
	 * @throws DotDataException
	 */
	public boolean deleteCalendarReminders(List<CalendarReminder> calendarReminders)
	{
		boolean errors = false;
		for(CalendarReminder calendarReminder : calendarReminders)
		{
			try
			{
				HibernateUtil.delete(calendarReminder);
			}
			catch(Exception ex)
			{
				Logger.warn(CalendarReminderFactoryImpl.class,ex.getMessage());
				errors = true;
			}
		}
		return errors;
	}
	
	/**
	 * Delete a specific CalendarReminder
	 * @param calendarReminder CalendarReminder to delete
	 * @throws DotDataException
	 */
	public void deleteCalendarReminder(CalendarReminder calendarReminder)
	{
		try {
			HibernateUtil.delete(calendarReminder);
		} catch (DotHibernateException e) {
			Logger.warn(CalendarReminderFactoryImpl.class, "deleteCalendarReminder failed:" + e, e);
		}
	}
	
	
}
