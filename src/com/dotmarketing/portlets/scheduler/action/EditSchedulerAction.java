package com.dotmarketing.portlets.scheduler.action;

import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.quartz.Trigger;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.scheduler.struts.SchedulerForm;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;
/**
 * @author Maria
 */

public class EditSchedulerAction extends DotPortletAction {

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res)
		throws Exception {
	
		String cmd = req.getParameter(Constants.CMD);
        Logger.debug(this, "Inside EditSchedulerAction cmd=" + cmd);

		//get the user
		User user = _getUser(req);
		
		CronScheduledTask scheduler = null;
		
		try {
	        Logger.debug(this, "I'm retrieving the schedule");
	        scheduler = _retrieveScheduledJob(req, res, config, form);
		}
		catch (Exception ae) {
			_handleException(ae, req);
		}

		/*
		 *  if we are saving, 
		 *  
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {
				
				SchedulerForm schedulerForm = (SchedulerForm) form;
				boolean hasErrors = false;
				
				if (!UtilMethods.isSet(schedulerForm.getJobName())) {
					SessionMessages.add(req, "error", "message.Scheduler.invalidJobName");
					hasErrors = true;
				} else if (!schedulerForm.isEditMode() && (scheduler != null)) {
					SessionMessages.add(req, "error", "message.Scheduler.jobAlreadyExists");
					hasErrors = true;
				}
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				
				Date endDate = null;
				if (schedulerForm.isHaveEndDate()) {
					try {
						endDate = sdf.parse(schedulerForm.getEndDate());
					} catch (Exception e) {
					}
				}
				
				if ((endDate != null) && !hasErrors) {
					Date startDate = null;
					if (schedulerForm.isHaveStartDate()) {
						try {
							startDate = sdf.parse(schedulerForm.getStartDate());
						} catch (Exception e) {
						}
					}
					
					if (startDate == null) {
						SessionMessages.add(req, "error", "message.Scheduler.startDateNeeded");
						hasErrors = true;
					} else if (endDate.before(startDate)) {
						SessionMessages.add(req, "error", "message.Scheduler.endDateBeforeStartDate");
						hasErrors = true;
					} else if (endDate.before(new Date())) {
						SessionMessages.add(req, "error", "message.Scheduler.endDateBeforeActualDate");
						hasErrors = true;
					}
				}
				
				if ((!UtilMethods.isSet(schedulerForm.getJavaClass())) && !hasErrors) {
					SessionMessages.add(req, "error", "message.Scheduler.invalidJavaClass");
					hasErrors = true;
				}
				
				if (Validator.validate(req,form,mapping) && !hasErrors) {
			        Logger.debug(this, "I'm Saving the scheduler");
					if (_saveScheduler(req, res, config, form, user)) {
						scheduler = _retrieveScheduledJob(req, res, config, form);
						
						if (scheduler != null) {
							_populateForm(form, scheduler);
							schedulerForm.setMap(scheduler.getProperties());
						}
						
						String redirect = req.getParameter("referrer");
						if (UtilMethods.isSet(redirect)) {
							redirect = URLDecoder.decode(redirect, "UTF-8") + "&group=" + scheduler.getJobGroup();
							_sendToReferral(req, res, redirect);
							return;
						}
					} else {
						SessionMessages.clear(req);
						SessionMessages.add(req, "error", "message.Scheduler.invalidJobSettings");
						schedulerForm.setMap(getSchedulerProperties(req, schedulerForm));
						loadEveryDayForm(form, req);
					}
				} else {
					schedulerForm.setMap(getSchedulerProperties(req, schedulerForm));
					loadEveryDayForm(form, req);
				}
			}
			catch (Exception ae) {
				if (!ae.getMessage().equals(WebKeys.UNIQUE_SCHEDULER_EXCEPTION)){
					_handleException(ae, req);
				}
			}
		}

		/*
		 * deleting the list, return to listing page
		 *  
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
		        Logger.debug(this, "I'm deleting the scheduler");
				_deleteScheduler(req, res, config, form,user);

			}
			catch (Exception ae) {
				_handleException(ae, req);
			}
			
			String redirect = req.getParameter("referrer");
			if (UtilMethods.isSet(redirect)) {
				redirect = URLDecoder.decode(redirect, "UTF-8");
				_sendToReferral(req, res, redirect);
				return;
			}
		}

		/*
		 * Copy copy props from the db to the form bean 
		 * 
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			if (scheduler != null) {
				_populateForm(form, scheduler);
				SchedulerForm schedulerForm = (SchedulerForm) form;
				schedulerForm.setMap(scheduler.getProperties());
				schedulerForm.setEditMode(true);
				if (!UtilMethods.isSet(scheduler.getCronExpression())) {
					SessionMessages.add(req, "message", "message.Scheduler.jobExpired");
				}
			}
		}
		
		/*
		 * return to edit page
		 *  
		 */
		setForward(req, "portlet.ext.scheduler.edit_scheduler");
	}
	
	private void loadEveryDayForm(ActionForm form, ActionRequest req) {
		String[] everyDay = req.getParameterValues("everyDay");
		
		SchedulerForm schedulerForm = (SchedulerForm) form;
		if (UtilMethods.isSet(everyDay) && schedulerForm.isEveryInfo()) {
			for (String dayOfWeek: everyDay) {
				if (dayOfWeek.equals("MON"))
					schedulerForm.setMonday(true);
				else if (dayOfWeek.equals("TUE"))
					schedulerForm.setTuesday(true);
				else if (dayOfWeek.equals("WED"))
					schedulerForm.setWednesday(true);
				else if (dayOfWeek.equals("THU"))
					schedulerForm.setThusday(true);
				else if (dayOfWeek.equals("FRI"))
					schedulerForm.setFriday(true);
				else if (dayOfWeek.equals("SAT"))
					schedulerForm.setSaturday(true);
				else if (dayOfWeek.equals("SUN"))
					schedulerForm.setSunday(true);
			}
			
			schedulerForm.setEveryInfo(true);
			schedulerForm.setEvery("isDays");
		} else {
			schedulerForm.setEvery("");
			schedulerForm.setMonday(false);
			schedulerForm.setTuesday(false);
			schedulerForm.setWednesday(false);
			schedulerForm.setThusday(false);
			schedulerForm.setFriday(false);
			schedulerForm.setSaturday(false);
			schedulerForm.setSunday(false);
		}
	}
	
	private Map<String, String> getSchedulerProperties(ActionRequest req, SchedulerForm schedulerForm) {
		Map<String, String> properties = new HashMap<String, String>(5);
		Enumeration<String> propertiesNames = req.getParameterNames();

		if (UtilMethods.isSet(schedulerForm.getMap())) {
			properties = schedulerForm.getMap();
		}
		else {
			String propertyName;
			String propertyValue;
			
			for (; propertiesNames.hasMoreElements();) {
				propertyName = propertiesNames.nextElement();
				if (propertyName.startsWith("propertyName")) {
					propertyValue = req.getParameter("propertyValue" + propertyName.substring(12));
					
					if (UtilMethods.isSet(req.getParameter(propertyName)) && UtilMethods.isSet(propertyValue))
						properties.put(req.getParameter(propertyName), propertyValue);
				}
			}
		}
		
		return properties;
	}
	
	private CronScheduledTask _retrieveScheduledJob(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
		SchedulerForm schedulerForm = (SchedulerForm) form;		
		try{
			if (UtilMethods.isSet(schedulerForm.getJobGroup())){
				if(!QuartzUtils.getStandardScheduledTask(schedulerForm.getJobName(), schedulerForm.getJobGroup()).isEmpty()){
					return (CronScheduledTask) QuartzUtils.getStandardScheduledTask(schedulerForm.getJobName(), schedulerForm.getJobGroup()).get(0);
				}
			}
			else{
				if(!QuartzUtils.getStandardScheduledTask(req.getParameter("name"), req.getParameter("group")).isEmpty()){
					return (CronScheduledTask) QuartzUtils.getStandardScheduledTask(req.getParameter("name"), req.getParameter("group")).get(0);
				}
			}
		}catch(ArrayIndexOutOfBoundsException e){
			return null;
		}
		return null;
		
	}

	public static boolean _saveScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) throws Exception {
		boolean result = false;
		SchedulerForm schedulerForm = (SchedulerForm) form;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		Date startDate = null;
		if (schedulerForm.isHaveStartDate()) {
			try {
				startDate = sdf.parse(schedulerForm.getStartDate());
			} catch (Exception e) {
			}
		}
		
		Date endDate = null;
		if (schedulerForm.isHaveEndDate()) {
			try {
				endDate = sdf.parse(schedulerForm.getEndDate());
			} catch (Exception e) {
			}
		}
		/*
		String type = req.getParameter("type");
		
		boolean schedulerEditable = true;
		if (req.getParameter("schedulerEditable").equals("false"))
			schedulerEditable = false;
		*/
		Map<String, Object> properties = new HashMap<String, Object>(5);
		Enumeration<String> propertiesNames = req.getParameterNames();

		if (UtilMethods.isSet(schedulerForm.getMap())) {
			properties = schedulerForm.getMap();
		}
		else {
			String propertyName;
			String propertyValue;
			
			for (; propertiesNames.hasMoreElements();) {
				propertyName = propertiesNames.nextElement();
				if (propertyName.startsWith("propertyName")) {
					propertyValue = req.getParameter("propertyValue" + propertyName.substring(12));
					
					if (UtilMethods.isSet(req.getParameter(propertyName)) && UtilMethods.isSet(propertyValue))
						properties.put(req.getParameter(propertyName), propertyValue);
				}
			}
		}
		
		String cronSecondsField = "0";
		String cronMinutesField = "0";
		String cronHoursField = "*";
		String cronDaysOfMonthField = "*";
		String cronMonthsField = "*";
		String cronDaysOfWeekField = "?";
		String cronYearsField = "*";

			if (UtilMethods.isSet(req.getParameter("at")) && req.getParameter("at").equals("isTime")) {
				String atTime = req.getParameter("atTime");
				if(UtilMethods.isSet(atTime)){
					String[] hms =  atTime.split(":");
					cronSecondsField = hms[2];
					cronMinutesField = hms[1];
					cronHoursField = hms[0].replace("T", "");
					schedulerForm.setAtTimeHour(Integer.valueOf(hms[0].replace("T", "")));
					schedulerForm.setAtTimeMinute(Integer.valueOf(hms[1]));
					schedulerForm.setAtTimeSecond(Integer.valueOf(hms[2]));
				}else{//Git-219 Campaigns
					if(UtilMethods.isSet(req.getParameter("atTimeSecond")))
						cronSecondsField = req.getParameter("atTimeSecond");
					if(UtilMethods.isSet(req.getParameter("atTimeMinute")))
						cronMinutesField = req.getParameter("atTimeMinute");
					if(UtilMethods.isSet(req.getParameter("atTimeHour")))
						cronHoursField = req.getParameter("atTimeHour");
				}
			}
			
			if (UtilMethods.isSet(req.getParameter("at")) && req.getParameter("at").equals("isBetween")) {
				//cronSecondsField = req.getParameter("betweenFromSecond") + "-" + req.getParameter("betweenToSecond");
				//cronMinutesField = req.getParameter("betweenFromMinute") + "-" + req.getParameter("betweenToMinute");
				cronHoursField = req.getParameter("betweenFromHour") + "-" + req.getParameter("betweenToHour");
			}

		
		if (schedulerForm.isEveryInfo()) {
			if (UtilMethods.isSet(req.getParameter("every")) && req.getParameter("every").equals("isDate")) {
				cronDaysOfMonthField = req.getParameter("everyDateDay");
				
				try {
					cronMonthsField = "" + (Integer.parseInt(req.getParameter("everyDateMonth")) + 1);
				} catch (Exception e) {
//					cronMonthsField = "";
				}
				
				cronYearsField = req.getParameter("everyDateYear");
			}
			
			if (UtilMethods.isSet(req.getParameter("every")) && req.getParameter("every").equals("isDays")) {
				cronDaysOfMonthField = "?";
				
				String[] daysOfWeek = req.getParameterValues("everyDay");
				
				cronDaysOfWeekField = "";
				for(String day: daysOfWeek) {
					if (cronDaysOfWeekField.length() == 0) {
						cronDaysOfWeekField = day;
					} else {
						cronDaysOfWeekField = cronDaysOfWeekField + "," + day;
					}
				}
			}
		}
		

			if (UtilMethods.isSet(req.getParameter("eachHours"))) {
				try {
					int eachHours = Integer.parseInt(req.getParameter("eachHours"));
					cronHoursField = cronHoursField + "/" + eachHours;
				} catch (Exception e) {
				}
			}
			
			if (UtilMethods.isSet(req.getParameter("eachMinutes"))) {
				try {
					int eachMinutes = Integer.parseInt(req.getParameter("eachMinutes"));
					cronMinutesField = cronMinutesField + "/" + eachMinutes;
				} catch (Exception e) {
				}
			}

		
		String cronExpression = cronSecondsField + " " + cronMinutesField + " " + cronHoursField + " " + cronDaysOfMonthField + " " + cronMonthsField + " " + cronDaysOfWeekField + " " + cronYearsField;

		CronScheduledTask job = new CronScheduledTask();
		job.setJobName(schedulerForm.getJobName());
		job.setJobGroup(schedulerForm.getJobGroup());
		job.setJobDescription(schedulerForm.getJobDescription());
		job.setJavaClassName(schedulerForm.getJavaClass());
		job.setProperties(properties);
		job.setStartDate(startDate);
		job.setEndDate(endDate);
		job.setCronExpression(cronExpression);
		
		try {// DOTCMS - 3897
			QuartzUtils.scheduleTask(job);
		} catch (Exception e) {			
			//e.printStackTrace();
			Logger.debug(EditSchedulerAction.class, "Based on configured schedule, the given trigger will never fire.");
			return false;
		}
		
		SessionMessages.add(req, "message", "message.Scheduler.saved");
		
		return true;
	}

	private void _deleteScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form , User user) throws Exception {
		SchedulerForm schedulerForm = (SchedulerForm) form;
		
		if (UtilMethods.isSet(schedulerForm.getJobGroup()))
			QuartzUtils.removeJob(schedulerForm.getJobName(), schedulerForm.getJobGroup());
		else
			QuartzUtils.removeJob(req.getParameter("name"), req.getParameter("group"));
		
		SessionMessages.add(req, "message", "message.Scheduler.delete");
	}
	
	private void _populateForm(ActionForm form, CronScheduledTask scheduler) {
		try {
			BeanUtils.copyProperties(form, scheduler);
			SchedulerForm schedulerForm = ((SchedulerForm) form);
			
			if (UtilMethods.isSet(scheduler.getJavaClassName())) {
				schedulerForm.setJavaClass(scheduler.getJavaClassName());
			}
			
			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			if (scheduler.getStartDate() != null) {
				schedulerForm.setHaveStartDate(true);
				schedulerForm.setStartDate(sdf2.format(scheduler.getStartDate()));
			} else {
				schedulerForm.setHaveStartDate(false);
			}
			
			if (scheduler.getEndDate() != null) {
				schedulerForm.setHaveEndDate(true);
				schedulerForm.setEndDate(sdf2.format(scheduler.getEndDate()));
			} else {
				schedulerForm.setHaveEndDate(false);
			}
			
			StringTokenizer cronExpressionTokens = new StringTokenizer(scheduler.getCronExpression());
			String token;
			String[] intervalTokens;
			String[] rangeTokens;
			
			// Seconds Cron Expression
			if (cronExpressionTokens.hasMoreTokens()) {
				token = cronExpressionTokens.nextToken();
				
				if (token.equals("*")) {
					schedulerForm.setAtInfo(false);
					schedulerForm.setAt(null);
					schedulerForm.setAtTimeSecond(0);
				} else {
					intervalTokens = token.split("/");
					rangeTokens = intervalTokens[0].split("-");
					
					if (rangeTokens.length == 2) {
						schedulerForm.setAtInfo(true);
						schedulerForm.setAt("isBetween");
						try {
							schedulerForm.setBetweenFromSecond(Integer.parseInt(rangeTokens[0]));
							schedulerForm.setBetweenToSecond(Integer.parseInt(rangeTokens[1]));
						} catch (Exception e) {
							schedulerForm.setBetweenFromSecond(0);
							schedulerForm.setBetweenToSecond(0);
						}
					} else {
						schedulerForm.setAtInfo(true);
						schedulerForm.setAt("isTime");
						try {
							schedulerForm.setAtTimeSecond(Integer.parseInt(intervalTokens[0]));
						} catch (Exception e) {
							schedulerForm.setAtTimeSecond(0);
						}
					}
					
//					if (intervalTokens.length == 2) {
//						;
//					}
				}
			}
			
			schedulerForm.setEachInfo(false);
			
			// Minutes Cron Expression
			if (cronExpressionTokens.hasMoreTokens()) {
				token = cronExpressionTokens.nextToken();
				
				if (token.equals("*")) {
					schedulerForm.setAtInfo(false);
					schedulerForm.setAt(null);
					schedulerForm.setAtTimeMinute(0);
				} else {
					intervalTokens = token.split("/");
					rangeTokens = intervalTokens[0].split("-");
					
					if (rangeTokens.length == 2) {
						schedulerForm.setAtInfo(true);
						schedulerForm.setAt("isBetween");
						try {
							schedulerForm.setBetweenFromMinute(Integer.parseInt(rangeTokens[0]));
							schedulerForm.setBetweenToMinute(Integer.parseInt(rangeTokens[1]));
						} catch (Exception e) {
							schedulerForm.setBetweenFromMinute(0);
							schedulerForm.setBetweenToMinute(0);
						}
					} else {
						schedulerForm.setAtInfo(true);
						schedulerForm.setAt("isTime");
						try {
							schedulerForm.setAtTimeMinute(Integer.parseInt(intervalTokens[0]));
						} catch (Exception e) {
							schedulerForm.setAtTimeMinute(0);
						}
					}
					
					if (intervalTokens.length == 2) {
						try {
							schedulerForm.setEachMinutes(Integer.parseInt(intervalTokens[1]));
							schedulerForm.setEachInfo(true);
						} catch (Exception e) {
							schedulerForm.setEachMinutes(0);
						}
					}
				}
			}
			
			// Hours Cron Expression
			if (cronExpressionTokens.hasMoreTokens()) {
				token = cronExpressionTokens.nextToken();
				
				if (token.equals("*")) {
					schedulerForm.setAtInfo(false);
					schedulerForm.setAt(null);
					schedulerForm.setAtTimeHour(0);
				} else {
					intervalTokens = token.split("/");
					rangeTokens = intervalTokens[0].split("-");
					
					if (rangeTokens.length == 2) {
						schedulerForm.setAtInfo(true);
						schedulerForm.setAt("isBetween");
						try {
							schedulerForm.setBetweenFromHour(Integer.parseInt(rangeTokens[0]));
							schedulerForm.setBetweenToHour(Integer.parseInt(rangeTokens[1]));
						} catch (Exception e) {
							schedulerForm.setBetweenFromHour(0);
							schedulerForm.setBetweenToHour(0);
						}
					} else {
						schedulerForm.setAtInfo(true);
						schedulerForm.setAt("isTime");
						try {
							schedulerForm.setAtTimeHour(Integer.parseInt(intervalTokens[0]));
						} catch (Exception e) {
							schedulerForm.setAtTimeHour(0);
						}
					}
					
					if (intervalTokens.length == 2) {
						try {
							schedulerForm.setEachHours(Integer.parseInt(intervalTokens[1]));
							schedulerForm.setEachInfo(true);
						} catch (Exception e) {
							schedulerForm.setEachHours(0);
						}
					}
				}
			}
			
			schedulerForm.setEveryInfo(false);
			schedulerForm.setEvery(null);
			
			// Days of Month Cron Expression
			if (cronExpressionTokens.hasMoreTokens()) {
				token = cronExpressionTokens.nextToken();
				
				if (token.equals("*") || token.equals("?")) {
					schedulerForm.setEveryDateDay(-1);
				} else {
					try {
						schedulerForm.setEveryDateDay(Integer.parseInt(token));
						schedulerForm.setEveryInfo(true);
						schedulerForm.setEvery("isDate");
					} catch (Exception e) {
						schedulerForm.setEveryDateDay(-1);
					}
				}
			}
			
			// Months Cron Expression
			if (cronExpressionTokens.hasMoreTokens()) {
				token = cronExpressionTokens.nextToken();
				
				if (token.equals("*")) {
					schedulerForm.setEveryDateMonth(-1);
				} else {
					try {
						schedulerForm.setEveryDateMonth(Integer.parseInt(token));
						schedulerForm.setEveryInfo(true);
						schedulerForm.setEvery("isDate");
					} catch (Exception e) {
						schedulerForm.setEveryDateMonth(-1);
					}
				}
			}
			
			// Days of Week Cron Expression
			if (cronExpressionTokens.hasMoreTokens()) {
				token = cronExpressionTokens.nextToken();
				
				if ((!token.equals("*")) && (!token.equals("?"))) {
					StringTokenizer daysOfWeek = new StringTokenizer(token, ",");
					String dayOfWeek;
					
					for (; daysOfWeek.hasMoreTokens();) {
						dayOfWeek = daysOfWeek.nextToken();
						
						if (dayOfWeek.equals("MON"))
							schedulerForm.setMonday(true);
						else if (dayOfWeek.equals("TUE"))
							schedulerForm.setTuesday(true);
						else if (dayOfWeek.equals("WED"))
							schedulerForm.setWednesday(true);
						else if (dayOfWeek.equals("THU"))
							schedulerForm.setThusday(true);
						else if (dayOfWeek.equals("FRI"))
							schedulerForm.setFriday(true);
						else if (dayOfWeek.equals("SAT"))
							schedulerForm.setSaturday(true);
						else if (dayOfWeek.equals("SUN"))
							schedulerForm.setSunday(true);
					}
					
					schedulerForm.setEveryInfo(true);
					schedulerForm.setEvery("isDays");
				}
			}
			
			// Years Cron Expression
			if (cronExpressionTokens.hasMoreTokens()) {
				token = cronExpressionTokens.nextToken();
				
				if (token.equals("*")) {
					schedulerForm.setEveryDateYear(-1);
				} else {
					try {
						schedulerForm.setEveryDateYear(Integer.parseInt(token));
						schedulerForm.setEveryInfo(true);
						schedulerForm.setEvery("isDate");
					} catch (Exception e) {
						schedulerForm.setEveryDateYear(-1);
					}
				}
			}
			schedulerForm.setEveryDate(schedulerForm.getEveryDateYear()+"-"+(schedulerForm.getEveryDateMonth()<10?"0"+schedulerForm.getEveryDateMonth():schedulerForm.getEveryDateMonth())+"-"+schedulerForm.getEveryDateDay());
			
		} catch (Exception e) {
			Logger.warn(this, e.getMessage());
		}
	}
}