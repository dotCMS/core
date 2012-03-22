package com.dotmarketing.quartz.job;

import java.util.Date;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.calendar.business.CalendarReminderAPI;
import com.dotmarketing.util.Logger;

/**
 * Job implementation to run calendar reminder process
 *
 * @author  Salvador Di Nardo
 */

public class CalendarReminderThread implements Job {
	public CalendarReminderThread() {
		
	}
	
	/**
	  * Thread main method to start the calendar reminder process
	  */
	@SuppressWarnings("unchecked")
	public void run() {
		Logger.debug(this, "Running Calendar Reminder Job");
		
		try {
		    HibernateUtil.startTransaction();
			CalendarReminderAPI CRAI = APILocator.getCalendarReminderAPI();
			Date now = new Date();
			CRAI.sendCalendarRemainder(now);
			Logger.debug(this,"The Calendar Reminder Job End successfully");
		} catch (Exception e) {
			Logger.warn(this, e.toString());
		}
		finally {
			try {
				HibernateUtil.commitTransaction();
			} catch (Exception e) {
				Logger.warn(this, e.toString());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#destroy()
	 */
	public void destroy() {
	}
	
	/**
	  * Job main method to start Calendar Reminder process, this method call run()
	  * @param		context JobExecutionContext.
	  * @exception	JobExecutionException.
	  */
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Logger.debug(this, "Running CalendarReminderThread - " + new Date());		
		try {
			run();
		} catch (Exception e) {
			Logger.warn(this, e.toString());
		}
	}
}
