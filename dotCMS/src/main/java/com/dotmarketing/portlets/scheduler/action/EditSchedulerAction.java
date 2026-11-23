package com.dotmarketing.portlets.scheduler.action;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.scheduler.struts.SchedulerForm;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.beanutils.BeanUtils;

/**
 * @author Maria
 */

public class EditSchedulerAction extends DotPortletAction {

	/** Request attribute with the name of the user a job runs as, shown in the edit form. */
	static final String SCHEDULED_BY_ATTRIBUTE = "scheduledByUserName";

	/**
	 * Deletes a job ({@code cmd=delete}) or loads one into the edit form ({@code cmd=edit}), then shows
	 * the edit form. Saving a job is done by {@link com.dotcms.rest.api.v1.scheduler.SchedulerJobResource}.
	 *
	 * @param mapping the Struts mapping
	 * @param form    the {@link SchedulerForm} naming the job
	 * @param config  the portlet configuration
	 * @param req     the request
	 * @param res     the response
	 * @throws Exception if the job cannot be read or deleted
	 */
	@Override
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
		 * deleting the list, return to listing page. Jobs are saved through
		 * SchedulerJobResource (/api/v1/scheduler/jobs), not through this action.
		 */
		if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
		        Logger.debug(this, "I'm deleting the scheduler");
				_deleteScheduler(req, res, config, form,user);

			}
			catch (Exception ae) {
				try{
					_forceTodDeleteScheduler(req, res, config, form,user);
				}catch(Exception a){
					_handleException(a, req);
				}
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
				schedulerForm.setMap(withoutScheduledBy(scheduler.getProperties()));
				setScheduledByAttribute(req, scheduler);
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

	/**
	 * Returns a copy of the job properties without {@link ScheduledTask#SCHEDULED_BY_USER_ID}, so the
	 * edit form only lists the parameters people set. The value is recorded again when the job is saved.
	 *
	 * @param properties the job properties
	 * @return the properties to show in the form, or {@code null} when there are none
	 */
	private static Map<String, Object> withoutScheduledBy(final Map<String, Object> properties) {
		if (properties == null) {
			return null;
		}
		final Map<String, Object> visible = new HashMap<>(properties);
		visible.remove(ScheduledTask.SCHEDULED_BY_USER_ID);
		return visible;
	}

	/**
	 * Puts the name of the user the job runs as, recorded under {@link ScheduledTask#SCHEDULED_BY_USER_ID},
	 * in the {@link #SCHEDULED_BY_ATTRIBUTE} request attribute so the edit form can show it. The user id
	 * is shown when the user cannot be loaded, and nothing is set when the job has no user recorded.
	 *
	 * @param req       the request
	 * @param scheduler the job being edited
	 */
	private static void setScheduledByAttribute(final ActionRequest req, final CronScheduledTask scheduler) {
		final Object userId = scheduler.getProperties() == null ? null
				: scheduler.getProperties().get(ScheduledTask.SCHEDULED_BY_USER_ID);
		if (!UtilMethods.isSet(userId)) {
			return;
		}
		String name = userId.toString();
		try {
			name = APILocator.getUserAPI().loadUserById(userId.toString()).getFullName();
		} catch (final Exception e) {
			Logger.debug(EditSchedulerAction.class, () -> "Could not load the user " + userId + ": " + e.getMessage());
		}
		req.setAttribute(SCHEDULED_BY_ATTRIBUTE, name);
	}

	private void _deleteScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form , User user) throws Exception {
		SchedulerForm schedulerForm = (SchedulerForm) form;
		
		if (UtilMethods.isSet(schedulerForm.getJobGroup()))
			QuartzUtils.removeJob(schedulerForm.getJobName(), schedulerForm.getJobGroup());
		else
			QuartzUtils.removeJob(req.getParameter("name"), req.getParameter("group"));
		
		SessionMessages.add(req, "message", "message.Scheduler.delete");
	}
	
	/**
	 * This method force the job deletion when the job class is not found
	 */
	private String update_class_to_delete = "update qrtz_excl_job_details set job_class_name=? where job_name=? and job_group=?";
	private void _forceTodDeleteScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form , User user) throws Exception {
		SchedulerForm schedulerForm = (SchedulerForm) form;
		if (UtilMethods.isSet(schedulerForm.getJobGroup())){
			DotConnect dc = new DotConnect();
			dc.setSQL(update_class_to_delete);
			dc.addParam("com.dotmarketing.quartz.job.TimeMachineJob");
			dc.addParam(schedulerForm.getJobName());
			dc.addParam(schedulerForm.getJobGroup());
			dc.loadResult();
			
			QuartzUtils.removeJob(schedulerForm.getJobName(), schedulerForm.getJobGroup());
		}else{
			DotConnect dc = new DotConnect();
			dc.setSQL(update_class_to_delete);
			dc.addParam("com.dotmarketing.quartz.job.TimeMachineJob");
			dc.addParam(req.getParameter("name"));
			dc.addParam(req.getParameter("group"));
			dc.loadResult();
			
			QuartzUtils.removeJob(req.getParameter("name"), req.getParameter("group"));
		}
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
			
			schedulerForm.setCronExpression(scheduler.getCronExpression());
			
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