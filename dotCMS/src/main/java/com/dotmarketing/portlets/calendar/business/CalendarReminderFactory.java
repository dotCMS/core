package com.dotmarketing.portlets.calendar.business;

import java.util.Date;
import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.calendar.model.CalendarReminder;

public interface CalendarReminderFactory {
	
	/**
	 * Find an CalenderReminder, based on the user, event and date of the reminder
	 * @param userId userId of the CalendarReminder to retrieve
	 * @param eventId eventId of the CalendarReminder to retrieve
	 * @param sendDate Date of the CalendarReminder to retrieve
	 * @return
	 */
	public CalendarReminder getCalendarReminder(long userId, String eventId,Date sendDate);
	
	/**
	 * Retrieve all the CalendarReminders before the specified date
	 * @param sendDate The Date used to retrieve the CalendarReminders
	 * @return
	 */
	public List<CalendarReminder> getCalendarReminderBefore(Date sendDate);
	
	/**
	 * Return all the CalendarReminders of the system
	 * @return
	 * @throws DotDataException
	 */
	public List<CalendarReminder> getAll();
	
	//Saves
	/**
	 * Save a CalendarReminder to the DB
	 */
	public void saveCalendarReminder(CalendarReminder calendarReminder);
		
	//Delete	
	/**
	 * Delete a List of Calendar Reminder
	 * @param calendarReminders List of CalendarReminder to delete
	 * @throws DotDataException
	 */
	public boolean deleteCalendarReminders(List<CalendarReminder> calendarReminders);
	
	/**
	 * Delete a specific CalendarReminder
	 * @param calendarReminder CalendarReminder to delete
	 * @throws DotDataException
	 */
	public void deleteCalendarReminder(CalendarReminder calendarReminder);
}
