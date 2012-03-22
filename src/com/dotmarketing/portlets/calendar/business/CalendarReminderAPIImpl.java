package com.dotmarketing.portlets.calendar.business;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.portlets.calendar.model.CalendarReminder;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

public class CalendarReminderAPIImpl implements CalendarReminderAPI {
	EventFactory EFI = FactoryLocator.getEventFactory();
	CalendarReminderFactory CRFI = FactoryLocator.getCalendarReminderFactory();

	/**
	 * Delete a specific CalendarReminder
	 * @param calendarReminder CalendarReminder to delete
	 * @throws DotDataException
	 */
	public void delete(CalendarReminder calendarReminder)
			throws DotDataException {
		CRFI.deleteCalendarReminder(calendarReminder);
	}

	/**
	 * Delete a List of Calendar Reminder
	 * @param calendarReminders List of CalendarReminder to delete
	 * @throws DotDataException
	 */
	public void delete(List<CalendarReminder> calendarReminders)
			throws DotDataException {
		CRFI.deleteCalendarReminders(calendarReminders);
	}

	/**
	 * Find an CalenderReminder, based on the user, event and date of the reminder
	 * @param userId userId of the CalendarReminder to retrieve
	 * @param eventId eventId of the CalendarReminder to retrieve
	 * @param date Date of the CalendarReminder to retrieve
	 * @return
	 * @throws DotDataException
	 */
	public CalendarReminder find(long userId, String eventId, Date date)
			throws DotDataException {
		return CRFI.getCalendarReminder(userId, eventId,date);
	}

	/**
	 * Return all the CalendarReminders of the system
	 * @return
	 * @throws DotDataException
	 */
	public List<CalendarReminder> findAll() throws DotDataException {
		return CRFI.getAll();
	}
	
	/**
	 * Creates a new CalendarReminder of an Event for an especific user, if the user doesn't exist it create a new user in the system
	 * @param emailAddress email address of the user
	 * @param firstName First name of the user
	 * @param lastName Last name of the user
	 * @param eventId EventId of the event for the CalendarReminder
	 * @param daysInAdvance How many days in advance the reminder will be sent
	 * @throws DotDataException
	 */
	public void create(String emailAddress,String firstName, String lastName,String eventId, int daysInAdvance)
			throws DotDataException {
		daysInAdvance = (daysInAdvance < 0 ? daysInAdvance : -daysInAdvance);
		// Create the user if it doesn't exist
		User user = null;
		if (UtilMethods.isSet(emailAddress)
				&& APILocator.getUserAPI().userExistsWithEmail(emailAddress)) {
			// The user exist, I have to retrieve it
			try {
				user = APILocator.getUserAPI().loadByUserByEmail(emailAddress, APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotDataException(e.getMessage(),e);
			}
		} else {
			// Create the user cause it doesn't exist
			try {
				user = createAccount(emailAddress,firstName,lastName);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotDataException(e.getMessage(),e);
			}
		}

		// Create the CalendarReminder
		String userId = user.getUserId();
		try {
			Event event = EFI.find(eventId, true, user, true);
			Date eventDate = event.getStartDate();
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(eventDate);
			gc.add(Calendar.DATE,daysInAdvance);
			Date calendarReminderDate = gc.getTime();

			CalendarReminder calendarReminder = new CalendarReminder();
			calendarReminder.setUserId(userId);
			calendarReminder.setEventId(eventId);
			calendarReminder.setSendDate(calendarReminderDate);
			CRFI.saveCalendarReminder(calendarReminder);
		} catch (Exception ex) {
			Logger.warn(CalendarReminderAPIImpl.class, ex.getMessage());
		}
	}
	
	/**
	 * Send all the CalendarReminder that has been set to be send before the date used as parameter
	 * @param date The date used to search the CalendarReminders
	 */
	public void sendCalendarRemainder(Date date) {
		// Get All the Reminders that have to be send today
		List<CalendarReminder> calendarReminders = CRFI.getCalendarReminderBefore(date);
		for (CalendarReminder calendarReminder : calendarReminders) {
			try {
				// Get the user information
				String userId = calendarReminder.getUserId();
				User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);

				// Get the event information
				String eventId = calendarReminder.getEventId();
				Event event = EFI.find(eventId, true, user, true);
				Map<String, Object> parameters = new HashMap<String, Object>();
				parameters.put("fullName", user.getFullName());
				parameters.put("eventTitle", event.getTitle());
				parameters.put("eventDate", UtilMethods.dateToHTMLDate(event.getStartDate(), "MM/dd/yyyy HH:mm a"));
				parameters.put("eventId", event.getIdentifier());
				
				//Template
				String reminderTemplate = Config.getStringProperty("CALENDAR_REMINDER_THREAD_TEMPLATE");
				reminderTemplate = (UtilMethods.isSet(reminderTemplate) ?  reminderTemplate : "/calendar/emailTemplate." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));
				parameters.put("emailTemplate",reminderTemplate);
				
				//From email
				String fromEmail = Config.getStringProperty("CALENDAR_REMINDER_THREAD_FROM_EMAIL");
				fromEmail = (UtilMethods.isSet(fromEmail) ?  fromEmail : "support@dotcms.org");
				parameters.put("from", fromEmail);
				
				//From Name
				String fromName = Config.getStringProperty("CALENDAR_REMINDER_THREAD_FROM_NAME");
				fromName = (UtilMethods.isSet(fromName) ?  fromName : "dotCMS System");				
				parameters.put("fromName",fromName);
				
				//To email
				parameters.put("to", user.getEmailAddress());

				String emailSubject = Config.getStringProperty("CALENDAR_REMINDER_THREAD_EMAIL_SUBJECT");
				emailSubject = (UtilMethods.isSet(emailSubject) ? emailSubject : "Event Reminder");			
				parameters.put("subject",emailSubject);

				// Send the email
				sendEmail(user, parameters);
			} catch (Exception ex) {
				Logger.debug(CalendarReminderAPIImpl.class, ex.toString());
			}
		}
		// Delete the remainders that have already be sent
		CRFI.deleteCalendarReminders(calendarReminders);
	}

	private User createAccount(String emailAddress,String firstName,String lastName) throws Exception {

		User user = APILocator.getUserAPI().loadByUserByEmail(emailAddress, APILocator.getUserAPI().getSystemUser(), false);
		User defaultUser = APILocator.getUserAPI().getDefaultUser();
		Date today = new Date();

		if (user.isNew() || (!user.isNew() && user.getLastLoginDate() == null)) {

			// ### CREATE USER ###
			Company company = PublicCompanyFactory.getDefaultCompany();
			user.setEmailAddress(emailAddress.trim().toLowerCase());
			user.setFirstName(firstName == null ? "" : firstName);
			user.setMiddleName("");
			user.setLastName(lastName == null ? "" : lastName);
			user.setNickName("");
			user.setCompanyId(company.getCompanyId());
			user.setPasswordEncrypted(true);
			user.setGreeting("Welcome, " + user.getFullName() + "!");

			// Set defaults values
			if (user.isNew()) {
				// if it's a new user we set random password
				String pass = PublicEncryptionFactory.getRandomPassword();
				user.setPassword(PublicEncryptionFactory.digestString(pass));
				user.setLanguageId(defaultUser.getLanguageId());
				user.setTimeZoneId(defaultUser.getTimeZoneId());
				user.setSkinId(defaultUser.getSkinId());
				user.setDottedSkins(defaultUser.isDottedSkins());
				user.setRoundedSkins(defaultUser.isRoundedSkins());
				user.setResolution(defaultUser.getResolution());
				user.setRefreshRate(defaultUser.getRefreshRate());
				user.setLayoutIds("");
				user.setActive(true);
				user.setCreateDate(today);
			}

			user.setActive(false);

			APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);
			// ### END CREATE USER ###

			// ### CREATE USER_PROXY ###
			UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user
					.getUserId(),APILocator.getUserAPI().getSystemUser(), false);
			userProxy.setPrefix("");
			userProxy.setTitle("");
			userProxy.setOrganization("");
			userProxy.setUserId(user.getUserId());
			com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);
			// ### END CRETE USER_PROXY ###

			Role defaultRole = com.dotmarketing.business.APILocator.getRoleAPI().loadRoleByKey(Config
					.getStringProperty("CMS_VIEWER_ROLE"));
			String roleId = defaultRole.getId();
			if (InodeUtils.isSet(roleId)) {
				com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(roleId, user);
			}
		}
		// ### END CREATE ADDRESS ###
		return user;
	}

	private void sendEmail(User user, Map<String, Object> parameters) throws DotDataException, DotSecurityException {
		
		HostAPI hostAPI = APILocator.getHostAPI();
		
		// Get the default host cause there is no request
		Host currentHost = hostAPI.findDefaultHost(user, false);
		
		// The parameters to be validated
		Set<String> toValidate = parameters.keySet();

		try {
			EmailFactory.sendParameterizedEmail(parameters,toValidate,currentHost, user);
		} catch (Exception ex) {
			Logger.debug(CalendarReminderAPIImpl.class, ex.toString());
		}
	}
}
