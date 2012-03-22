package com.dotmarketing.portlets.campaigns.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.campaigns.factories.CampaignFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.campaigns.struts.CampaignForm;
import com.dotmarketing.portlets.communications.factories.CommunicationsFactory;
import com.dotmarketing.portlets.communications.model.Communication;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.portlets.scheduler.action.EditSchedulerAction;
import com.dotmarketing.portlets.scheduler.struts.SchedulerForm;
import com.dotmarketing.portlets.userfilter.factories.UserFilterFactory;
import com.dotmarketing.portlets.userfilter.model.UserFilter;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author Maria
 */

public class EditCampaignAction extends DotPortletAction {

	@SuppressWarnings("unchecked")
	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
	throws Exception {

		String cmd = (req.getParameter(Constants.CMD)!=null)? req.getParameter(Constants.CMD) : Constants.EDIT;
		String referer = req.getParameter("referer");

		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl)req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		if ((referer!=null) && (referer.length()!=0)) {
			referer = URLDecoder.decode(referer,"UTF-8");
		}

		Logger.debug(this, "EditCampaignAction cmd=" + cmd);

		HibernateUtil.startTransaction();

		User user = _getUser(req);

		try {
			_retrieveCampaign(req, res, config, form, user);

		} catch (ActionException ae) {
			_handleException(ae, req);
		}

		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);
		if(c.isLocked()){
			//add message
			SessionMessages.add(req, "message", "message.campaign.locked");
			setForward(req,"portlet.ext.campaigns.view_campaigns");
		}

		//getting the user roles
		boolean isCampaignManagerAdmin = false;
		String campaignManagerAdminRoleKey = "";
		try {
			Role campaignManagerAdminRole = APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CAMPAIGN_MANAGER_ADMIN"));
			campaignManagerAdminRoleKey = campaignManagerAdminRole.getRoleKey();
		}
		catch (Exception e) {}

		boolean isCampaignManagerEditor = false;
		String campaignManagerEditorRoleKey = "";
		try {
			Role campaignManagerEditorRole = APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CAMPAIGN_MANAGER_EDITOR"));
			campaignManagerEditorRoleKey = campaignManagerEditorRole.getRoleKey();
		}
		catch (Exception e) {}

		Role[] userRoles = (Role[])APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		for (int i = 0; i < userRoles.length; i++) {
			Role userrole = (Role) userRoles[i];
			if ((userrole.getRoleKey() != null) && userrole.getRoleKey().equals(campaignManagerAdminRoleKey)) {
				isCampaignManagerAdmin = true;
				if (isCampaignManagerEditor)
					break;
			}
			if ((userrole.getRoleKey() != null) && userrole.getRoleKey().equals(campaignManagerEditorRoleKey)) {
				isCampaignManagerEditor = true;
				if (isCampaignManagerAdmin)
					break;
			}
		}

		/*
		 * We are editing the campaign
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			try {
				_editCampaign(req, res, config, form, user);
				setForward(req,"portlet.ext.campaigns.edit_campaign");
				
				if ((!InodeUtils.isSet(c.getParentCampaign())) && UtilMethods.isSet(c.getCompletedDate())) {
					setForward(req,"portlet.ext.campaigns.view_report");
				}
				else{
					String parentCampaign = c.getParentCampaign();
					if (InodeUtils.isSet(parentCampaign)) {
						if (c.getWasSent())
							setForward(req,"portlet.ext.campaigns.view_report");
						else
							setForward(req,"portlet.ext.campaigns.edit_pending_campaign");
					}
				}
				if (c.getIsRecurrent() && c.isActive()) {
					if (UtilMethods.isSet(c.getExpirationDate()) && c.getExpirationDate().before(new Date())) {
						setForward(req,"portlet.ext.campaigns.view_report");
					}
					else {
						List<ScheduledTask> jobs = QuartzUtils.getStandardScheduledTask(String.valueOf(c.getInode()), "Recurrent Campaign");
						if (jobs.size() == 0) {
							setForward(req,"portlet.ext.campaigns.view_report");
						}
						else {
							Date endDate = jobs.get(0).getEndDate();
							if (endDate.before(new Date())) {
								setForward(req,"portlet.ext.campaigns.edit_campaign");
							}
						}
					}
				}
					
			} catch (ActionException ae) {
				_handleException(ae, req);
			}
		}

		/*
		 * We are editing the campaign
		 */
		if ((cmd != null) && cmd.equals("resend")) {
			try {
				_editCampaign(req, res, config, form, user);
				_resendCampaign(req, res, config, form, user);

				setForward(req,"portlet.ext.campaigns.edit_campaign");
			} catch (ActionException ae) {
				_handleException(ae, req);
			}
		}

		/*
		 * If we are updating the campaign, copy the information
		 * from the struts bean to the hbm inode and run the
		 * update action and return to the list
		 */
		else if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {
				String updateCampaignPermissionsOnly = req.getParameter(com.dotmarketing.util.Constants.UPDATE_CAMPAIGN_PERMISSIONS_ONLY);
				boolean updateAll = false;
				
				CampaignForm schedulerForm = (CampaignForm) form;
				boolean hasErrors = false;
				boolean displayRecurrence = false;
				
				if (schedulerForm.getTitle().trim().equals("")) {
					SessionMessages.add(req, "error", "message.campaign.error.titleRequired");
					hasErrors = true;
				}
				
				SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.DateFormats.DOTSCHEDULER_DATE2);
				
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
						displayRecurrence = true;
					} else if (endDate.before(startDate)) {
						SessionMessages.add(req, "error", "message.Scheduler.endDateBeforeStartDate");
						hasErrors = true;
						displayRecurrence = false;
					} else if (endDate.before(new Date())) {
						SessionMessages.add(req, "error", "message.Scheduler.endDateBeforeActualDate");
						hasErrors = true;
						displayRecurrence = true;
					}
				}

				if (((updateCampaignPermissionsOnly == null) || (updateCampaignPermissionsOnly.equalsIgnoreCase("false"))) && !hasErrors)
					updateAll = true;

				if ((updateCampaignPermissionsOnly != null) && (!updateAll) && !hasErrors) {
					try {
						int temp = Integer.parseInt(updateCampaignPermissionsOnly);
						if (temp < 1)
							updateAll = true;
					} catch (Exception e) {
					}
				}
				
				if (!hasErrors) {
					if (updateAll) {
						if (Validator.validate(req,form,mapping)) {
							hasErrors = !_saveCampaign(req, res, config, form, user);
						}
						else {
							setForward(req,"portlet.ext.campaigns.edit_campaign");
							return;
						}
					} else {
						hasErrors = !_saveCampaign(req, res, config, form, user);
					}
				}
				
				if (hasErrors) {
//					_editCampaign(req, res, config, form, user);
					loadEveryDayForm(form, req);
					schedulerForm = (CampaignForm) form;
					schedulerForm.setDisplayRecurrence(displayRecurrence);
					setForward(req,"portlet.ext.campaigns.edit_campaign");
					return;
				}
			} catch (ActionException ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req,res,referer);
		}
		/*
		 * If we are deleting the campaign,
		 * run the delete action and return to the list
		 *
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				_deleteCampaign(req, res, config, form, user);
			} catch (ActionException ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req,res,referer);
		}
		/*
		 * If we are copying the campaign,
		 * run the copy action and return to the list
		 *
		 */
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.COPY)) {
			try {
				_copyCampaign(req, res, config, form, user);
			} catch (ActionException ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req,res,referer);
		}
	}
	
	public void _resendCampaign(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {

		Campaign cToResend = CampaignFactory.getCampaign("0");

		CampaignForm cfform = (CampaignForm) form;

		Date now = new Date();
		SimpleDateFormat DATE_TO_PRETTY_HTML_DATE = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		cfform.setWebStartDate(DATE_TO_PRETTY_HTML_DATE.format(now));

		cfform.setExpirationDate(null);
		cfform.setWebExpirationDate("");

		cfform.setCompletedDate(null);
		cfform.setWebCompletedDate("");
		
		cfform.setInode(null);

		BeanUtils.copyProperties(cToResend, cfform);

		req.setAttribute("CampaignForm", cfform);
		req.setAttribute(WebKeys.CAMPAIGN_EDIT, cToResend);
	}

	private void loadEveryDayForm(ActionForm form, ActionRequest req) {
		String[] everyDay = req.getParameterValues("everyDay");
		
		CampaignForm schedulerForm = (CampaignForm) form;
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

	///// ************** ALL METHODS HERE *************************** ////////

	public void _retrieveCampaign(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {

		String inode = (req.getParameter("inode")!=null) ? req.getParameter("inode") : "0";

		Campaign c = null;
		c = CampaignFactory.getCampaign(inode);

		if(!InodeUtils.isSet(c.getInode())){
			c = CampaignFactory.newInstance();	
			c.setUserId(user.getUserId());
			c.setSendTo("mailingList");
		}

		req.setAttribute(WebKeys.CAMPAIGN_EDIT, c);

		List<Communication> list = CommunicationsFactory.getCommunications("","lower(title)"); 
		List<Communication> permitted = new ArrayList();
		for(Communication com : list){
			try {
				_checkUserPermissions(com, user, PERMISSION_READ);
				permitted.add(com);
			}catch(Exception e){}
		}
		req.setAttribute(WebKeys.COMMUNICATION_LIST_VIEW, permitted);

		// getting the user filter list
		List<UserFilter> userFilterList = UserFilterFactory.getAllUserFilter();
		List<UserFilter> userFilters = new ArrayList();
		for(UserFilter userFilter : userFilterList){
			try {
				_checkUserPermissions(userFilter, user, PERMISSION_READ);
				userFilters.add(userFilter);
			}catch(Exception e){}
		}
		req.setAttribute(WebKeys.USER_FILTER_LIST_VIEW_PORTLET, userFilters);

	}
	public void _editCampaign(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {

		CampaignForm cfform = (CampaignForm) form;
		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);

		BeanUtils.copyProperties(cfform, c);

		if (InodeUtils.isSet(c.getInode())) {
			//add the campaigns mailing list to the form
			MailingList ml = (MailingList) InodeFactory.getChildOfClass(c, MailingList.class);
			cfform.setMailingList(ml.getInode());

			//add the html page to the campaign
			HTMLPage page = (HTMLPage) InodeFactory.getChildOfClass(c, HTMLPage.class);
			if (InodeUtils.isSet(page.getInode())) {
				cfform.setHtmlPage(page.getInode());
				cfform.setSelectedHtmlPage(page.getTitle());
			}

			UserFilter uf = (UserFilter) InodeFactory.getChildOfClass(c, UserFilter.class);
			cfform.setUserFilterInode(uf.getInode());
		}
		else {
			cfform.setMailingList(null);
			cfform.setHtmlPage(null);
			cfform.setCommunicationInode(null);
			cfform.setUserFilterInode(null);
		}

		if (c.getIsRecurrent()) {
			// getting recurrency data
			List<ScheduledTask> jobs = QuartzUtils.getStandardScheduledTask(String.valueOf(c.getInode()), "Recurrent Campaign");

			if (jobs.size() > 0) {

				SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.DateFormats.DOTSCHEDULER_DATE2);

				CronScheduledTask job = (CronScheduledTask) jobs.get(0);
				
				String startDateStr = sdf.format(job.getStartDate());
				if (UtilMethods.isSet(startDateStr)) {
					cfform.setHaveStartDate(true);
					cfform.setStartDate(startDateStr);
				}
				else {
					cfform.setHaveStartDate(false);
					cfform.setStartDate(null);
				}

				String endDateStr = sdf.format(job.getEndDate());
				if (UtilMethods.isSet(endDateStr)) {
					cfform.setHaveEndDate(true);
					cfform.setEndDate(endDateStr);
				}
				else {
					cfform.setHaveEndDate(false);
					cfform.setEndDate(null);
				}

				String cronExpression = job.getCronExpression();
				
				if (UtilMethods.isSet(cronExpression)) {
					_populateForm(form, job);
				}
			} else {
//				c.setIsRecurrent(false);
				HibernateUtil.saveOrUpdate(c);
			}
		}
	}

	public boolean _saveCampaign(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {

		boolean result = true;
		
		HibernateUtil.startTransaction();
		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);

		String updateCampaignPermissionsOnly = req.getParameter(com.dotmarketing.util.Constants.UPDATE_CAMPAIGN_PERMISSIONS_ONLY);
		boolean updateAll = false;

		if ((updateCampaignPermissionsOnly == null) || (updateCampaignPermissionsOnly.equalsIgnoreCase("false")))
			updateAll = true;

		if ((updateCampaignPermissionsOnly != null) && (!updateAll)) {
			try {
				int temp = Integer.parseInt(updateCampaignPermissionsOnly);
				if (temp < 1)
					updateAll = true;
			} catch (Exception e) {
			}
		}

		if (updateAll) {
			CampaignForm cfform = (CampaignForm) form;

			boolean isWasRecurrent = c.getIsRecurrent();

			BeanUtils.copyProperties(req.getAttribute(WebKeys.CAMPAIGN_EDIT), cfform);
			HibernateUtil.saveOrUpdate(c);

			// wipe the old mailing list that was the child
			MailingList ml = (MailingList) InodeFactory.getChildOfClass(c, MailingList.class);
			c.deleteChild(ml);

			//try to get the campaign's new mailing list
			ml = (MailingList) InodeFactory.getInode(String.valueOf(cfform.getMailingList()), MailingList.class);
			if (InodeUtils.isSet(ml.getInode())) {
				c.addChild(ml);
			}

			// wipe the old communication that was the child
			Communication comm = (Communication) InodeFactory.getChildOfClass(c, Communication.class);
			c.deleteChild(comm);

			//try to get the campaign's new communication
			comm = (Communication) InodeFactory.getInode(String.valueOf(cfform.getCommunicationInode()), Communication.class);
			if (InodeUtils.isSet(comm.getInode())) {
				c.addChild(comm);
			}

			// wipe the old user filter that was the child
			UserFilter userfilter = (UserFilter) InodeFactory.getChildOfClass(c, UserFilter.class);
			c.deleteChild(userfilter);

			//try to get the campaign's new communication
			userfilter = (UserFilter) InodeFactory.getInode(String.valueOf(cfform.getUserFilterInode()), UserFilter.class);
			if (InodeUtils.isSet(userfilter.getInode())) {
				c.addChild(userfilter);
			}

			c.setUserId(user.getUserId());

			if (c.getIsRecurrent()) {
				SchedulerForm schedulerForm = new SchedulerForm();
				BeanUtils.copyProperties(schedulerForm, cfform);
				schedulerForm.setJavaClass("com.dotmarketing.quartz.job.DeliverCampaignThread");

				//schedulerForm.setJobName(c.getTitle());
				schedulerForm.setJobName(String.valueOf(c.getInode()));

				HashMap hashMap = new HashMap<String, String>();
				hashMap.put("inode", c.getInode());
				schedulerForm.setMap(hashMap);
				schedulerForm.setJobDescription(c.getTitle());

				//schedulerForm.setJobGroup(String.valueOf(c.getInode()));
				schedulerForm.setJobGroup("Recurrent Campaign");

				// create/edit quartz job
				result = EditSchedulerAction._saveScheduler(req, res, config, schedulerForm, user);
			}
			else if (isWasRecurrent) {
				// ending quartz job
				QuartzUtils.removeJob(c.getInode(), "Recurrent Campaign");
			}
			
			if (result)
				HibernateUtil.saveOrUpdate(c);

		}

		if (result) {
			//add message
			SessionMessages.add(req, "message", "message.campaign.saved");
		} else {
			SessionMessages.clear(req);
			SessionMessages.add(req, "error", "message.Scheduler.invalidJobSettings");
		}

		return result;
	}

	public void _copyCampaign(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {

		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);
		MailingList ml = (MailingList) InodeFactory.getChildOfClass(c, MailingList.class);
		HTMLPage page = (HTMLPage) InodeFactory.getChildOfClass(c, HTMLPage.class);
		Communication comm = (Communication) InodeFactory.getChildOfClass(c, Communication.class);
		UserFilter userfilter = (UserFilter) InodeFactory.getChildOfClass(c, UserFilter.class);

		Campaign copy = CampaignFactory.newInstance();

		copy.setTitle( c.getTitle() + " (copy)");
		copy.setFromEmail(c.getFromEmail());
		copy.setFromName(c.getFromName());
		copy.setCStartDate(c.getCStartDate());
		copy.setSubject(c.getSubject());
		copy.setMessage(c.getMessage());
		copy.setOwner(c.getOwner());
		copy.setUserId(c.getUserId());
		copy.setCommunicationInode(c.getCommunicationInode());
		copy.setUserFilterInode(c.getUserFilterInode());

		//no sure if this is needed
		HibernateUtil.saveOrUpdate(copy);

		if(InodeUtils.isSet(ml.getInode())){
			copy.addChild(ml);
		}
		if(InodeUtils.isSet(page.getInode())){
			copy.addChild(page);
		}
		if (InodeUtils.isSet(comm.getInode())) {
			copy.addChild(comm);
		}
		if (InodeUtils.isSet(userfilter.getInode())) {
			copy.addChild(userfilter);
		}
		HibernateUtil.saveOrUpdate(copy);

		//add message
		if (c.isSendEmail())
			SessionMessages.add(req, "message", "message.campaign.copied");
		else
			SessionMessages.add(req, "message", "message.campaign.copied.no.resend");
	}

	public void _deleteCampaign(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);
		boolean UserHasWriteCampPerms = perAPI.doesUserHavePermission(c,PERMISSION_WRITE,user);
		if(c.getUserId().equalsIgnoreCase(user.getUserId()) || UserHasWriteCampPerms)
		{
			//Ending Quartz Job
			if ((c.getIsRecurrent() && !InodeUtils.isSet(c.getParentCampaign())) && c.getUserId().equalsIgnoreCase(user.getUserId()) || UserHasWriteCampPerms) 
			{
				// removing the recurrent campaign occurrences
				List childCampaigns = CampaignFactory.getChildCampaignsByParent(String.valueOf(c.getInode()));
				if (childCampaigns.size() > 0) {
					Iterator childCampIter = childCampaigns.iterator();
					
					//### LOOP THE CHILD CAMPAIGNS ###
					while (childCampIter.hasNext()) {
						//Obtain the campaign
						Campaign childCampaign = (Campaign) childCampIter.next();
						CampaignFactory.deleteCampaign(childCampaign, user.getUserId());
					}
				}

				QuartzUtils.removeJob(String.valueOf(c.getInode()), "Recurrent Campaign");
			}
			CampaignFactory.deleteCampaign(c, user.getUserId());
			SessionMessages.add(req, "message", "message.campaign.deleted");
		}
		else
		{
			SessionMessages.add(req, "message", "message.campaign.deleted.error");
		}

	}
	
	private void _populateForm(ActionForm form, CronScheduledTask scheduler) {
		try {
			BeanUtils.copyProperties(form, scheduler);
			CampaignForm schedulerForm = ((CampaignForm) form);
			
			if (scheduler.getStartDate() != null) {
				schedulerForm.setHaveStartDate(true);
			} else {
				schedulerForm.setHaveStartDate(false);
			}
			
			if (scheduler.getEndDate() != null) {
				schedulerForm.setHaveEndDate(true);
			} else {
				schedulerForm.setHaveEndDate(false);
			}
			
			if (UtilMethods.isSet(scheduler.getCronExpression())) {
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
				if (scheduler.getCronExpression().endsWith("0 0 * * * ? *")) {
					schedulerForm.setEveryInfo(true);
					schedulerForm.setEvery("isDate");
				}
			}
		} catch (Exception e) {
			Logger.warn(this, e.getMessage());
		}
	}
}