package com.dotmarketing.portlets.calendar.business;

import java.util.Date;
import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.calendar.model.CalendarReminder;

public interface CalendarReminderAPI
{
	/**
	 * Delete a specific CalendarReminder
	 * @param calendarReminder CalendarReminder to delete
	 * @throws DotDataException
	 */
	public void delete(CalendarReminder calendarReminder) throws DotDataException;
	
	/**
	 * Delete a List of Calendar Reminder
	 * @param calendarReminders List of CalendarReminder to delete
	 * @throws DotDataException
	 */
	public void delete(List<CalendarReminder> calendarReminders) throws DotDataException ;

	/**
	 * Find an CalenderReminder, based on the user, event and date of the reminder
	 * @param userId userId of the CalendarReminder to retrieve
	 * @param eventId eventId of the CalendarReminder to retrieve
	 * @param date Date of the CalendarReminder to retrieve
	 * @return
	 * @throws DotDataException
	 */
	public CalendarReminder find(long userId,String eventId,Date date) throws DotDataException;

	/**
	 * Return all the CalendarReminders of the system
	 * @return
	 * @throws DotDataException
	 */
	public List<CalendarReminder> findAll() throws DotDataException;
	
	/**
	 * Creates a new CalendarReminder of an Event for an especific user, if the user doesn't exist it create a new user in the system
	 * @param emailAddress email address of the user
	 * @param firstName First name of the user
	 * @param lastName Last name of the user
	 * @param eventId EventId of the event for the CalendarReminder
	 * @param daysInAdvance How many days in advance the reminder will be sent
	 * @throws DotDataException
	 */
	public void create(String emailAddress,String firstName, String lastName, String eventId, int daysInAdvance) throws DotDataException;
	
	/**
	 * Send all the CalendarReminder that has been set to be send before the date used as parameter
	 * @param date The date used to search the CalendarReminders
	 */
	public void sendCalendarRemainder(Date date);
	
}
