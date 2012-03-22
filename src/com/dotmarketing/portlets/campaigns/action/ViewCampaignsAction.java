package com.dotmarketing.portlets.campaigns.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.quartz.SchedulerException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.campaigns.factories.CampaignFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.communications.model.Communication;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Maria Ahues
 * @version $Revision: 1.3 $
 *
 */
public class ViewCampaignsAction extends DotPortletAction {

	private PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	/**
	 * @param permissionAPI the permissionAPI to set
	 */
	public void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	/* 
	 * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.portlet.PortletConfig, javax.portlet.RenderRequest, javax.portlet.RenderResponse)
	 */
	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

		try {
			//gets the user
			User user = _getUser(req);
			
			String cmd = (req.getParameter(Constants.CMD)!=null)? req.getParameter(Constants.CMD) : Constants.EDIT;

			if ((cmd != null) && cmd.equals(Constants.DELETE)) {
				_deleteSelectedCampaigns(req, user);
			}

			_viewCampaigns(req, user);

			if (req.getWindowState().equals(WindowState.NORMAL)) {
				return mapping.findForward("portlet.ext.campaigns.view");
			}
			else {
				return mapping.findForward("portlet.ext.campaigns.view_campaigns");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	private void _viewCampaigns(RenderRequest req, User user) throws PortalException, SystemException {

		DateFormat modDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, user.getLocale());
		boolean isCampaignManagerAdmin = CampaignFactory.isCampaignManagerAdmin(user);

		//get their lists
		String orderby = req.getParameter("orderby");
		String condition = req.getParameter("query");
		String direction = req.getParameter("direction");
		if(UtilMethods.isSet(direction) && UtilMethods.isSet(orderby))
			orderby = orderby+" "+direction;

		// getting all the campaigns
		List<Campaign> list = (List<Campaign>) CampaignFactory.getCampaigns(condition,orderby);
		int totalCampaignToDisplay = list.size();

		// obtaining quantity of the subsequent occurrences of every recurrent campaign 
		HashMap occurrencesRecurrentCampaign = new HashMap<String, String>();
		for(Campaign c : list){
			String parentCampaign = c.getParentCampaign();
			if (InodeUtils.isSet(parentCampaign)) {			// it's an occurrence of a recurrent campaign
				totalCampaignToDisplay--;
				String occurrencesStr = (String) occurrencesRecurrentCampaign.get(String.valueOf(parentCampaign));
				int occurrences = 0;
				if (com.dotmarketing.util.UtilMethods.isSet(occurrencesStr)) {
					occurrences = Integer.parseInt(String.valueOf(occurrencesStr));
					occurrences++;
				}
				else {
					occurrences = 1;
				}
				occurrencesRecurrentCampaign.put(String.valueOf(parentCampaign), String.valueOf(occurrences));
			}
		}

		// initializing the hash map object where to save the info of every occurrence of every recurrent campaign 
		Iterator keys = occurrencesRecurrentCampaign.keySet().iterator();
		while (keys.hasNext()) {
			String keyParentInode = (String) keys.next();
			int valueOccurrence = Integer.parseInt((String) occurrencesRecurrentCampaign.get(keyParentInode));
			
			String[][] occurrencesCampaigns = new String[valueOccurrence][8];
			occurrencesRecurrentCampaign.put(keyParentInode, occurrencesCampaigns);
		}

		// getting the campaign info for non recurrent campaigns and recurrent campaign, only the campaign that defines the recurrence
		int i = 0;
		String[][] listArray = new String[totalCampaignToDisplay][8];
		for(Campaign c : list){
			try {
				String parentCampaign = c.getParentCampaign();
				if (!InodeUtils.isSet(parentCampaign)) {
					listArray[i] = _getCampaignInfo(c, user, modDateFormat, isCampaignManagerAdmin);
					i++;
				}
			}catch(Exception e){}
		}

		// getting the campaign info for every occurrence of every recurrent campaign 
		HashMap occurrencesRecurrentCampaignAdded = new HashMap<String, String>();
		for(Campaign c : list){
			try {
				String parentCampaign = c.getParentCampaign();
				if (InodeUtils.isSet(parentCampaign)) {
					String[] occurrenceCampaign = _getCampaignInfo(c, user, modDateFormat, isCampaignManagerAdmin);

					String[][] occurrencesCampaigns = (String[][]) occurrencesRecurrentCampaign.get(String.valueOf(parentCampaign));

					String occurrencesAdded = (String) occurrencesRecurrentCampaignAdded.get(String.valueOf(parentCampaign));
					int currentPosition = 0;
					if (com.dotmarketing.util.UtilMethods.isSet(occurrencesAdded)) {
						currentPosition = Integer.parseInt(String.valueOf(occurrencesAdded));
					}

					occurrencesCampaigns[currentPosition] = occurrenceCampaign;

					occurrencesRecurrentCampaignAdded.put(String.valueOf(parentCampaign), String.valueOf(++currentPosition));
					occurrencesRecurrentCampaign.put(String.valueOf(parentCampaign), occurrencesCampaigns);
				}
			}catch(Exception e){}
		}
		
		req.setAttribute(WebKeys.CAMPAIGN_LIST, listArray);
		req.setAttribute(WebKeys.CAMPAIGN_RECURRENT_OCURRENCES, occurrencesRecurrentCampaign);
	}

	private String[] _getCampaignInfo(Campaign c, User user, DateFormat modDateFormat, boolean isCampaignManagerAdmin) throws LanguageException, DotDataException, SchedulerException {
		String[] listArray = new String[9];
		listArray[0] = String.valueOf(c.getInode());				// campaign inode
		listArray[1] = c.getTitle();								// campaign title
		listArray[2] = modDateFormat.format(c.getCStartDate());	// campaign start date
		listArray[3] = String.valueOf(c.getIsRecurrent());		// campaign recurrency

		String status = "";
		if(c.isActive()){
			if((c.getCompletedDate() != null) && (!c.getIsRecurrent())){
				status =  LanguageUtil.get(user, "campaign.status_done")+": " + modDateFormat.format(c.getCompletedDate());
			}
			else {
				if(c.isLocked()) {
					status = LanguageUtil.get(user, "campaign.status_Running");
				}
				else {
					if (c.getIsRecurrent()) {
						List<ScheduledTask> jobs = QuartzUtils.getStandardScheduledTask(String.valueOf(c.getInode()), "Recurrent Campaign");
						
						if (jobs.size() > 0) {
							
							CronScheduledTask scheduler = (CronScheduledTask) jobs.get(0);
							
							SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.DateFormats.DOTSCHEDULER_DATE2);

							String schedulerEndDateString = sdf.format(scheduler.getEndDate());
							if (!UtilMethods.isSet(schedulerEndDateString) && UtilMethods.isSet(c.getCompletedDate())) {

								Date now = new Date();
								if (now.before(c.getCompletedDate())) {
									status = LanguageUtil.get(user, "campaign.status_Pending")+": " + modDateFormat.format(c.getCompletedDate());
								}
								else {
									status = LanguageUtil.get(user, "campaign.status_Expired");
								}
							}
							else {
								String cronExpression = scheduler.getCronExpression();
								if (!UtilMethods.isSet(cronExpression)) {
									status = LanguageUtil.get(user, "campaign.status_Expired");
								}
								else {
									if (UtilMethods.isSet(c.getCompletedDate()))
										status = LanguageUtil.get(user, "campaign.status_Pending")+": " + modDateFormat.format(c.getCompletedDate());
									else
										status = LanguageUtil.get(user, "campaign.status_Pending");
								}
							}
							
							listArray[2] = modDateFormat.format(c.getCStartDate());	// campaign start date
							
						} else {
							HibernateUtil.saveOrUpdate(c);
							
							listArray[3] = "false";
							if (UtilMethods.isSet(c.getCompletedDate()))
								status = LanguageUtil.get(user, "campaign.status_done")+": " + modDateFormat.format(c.getCompletedDate());
							else
								status = LanguageUtil.get(user, "campaign.status_done");
						}
					}
					else {
						status = LanguageUtil.get(user, "campaign.status_Pending");
					}
				}
			}
		}
		else {
			status = LanguageUtil.get(user, "campaign.status_Inactive");
		}
		listArray[4] = status;									// campaign status

		if (!isCampaignManagerAdmin) {
			// adding read permission
			try {
				_checkUserPermissions(c, user, PERMISSION_READ);
				listArray[5] = "true";
			} catch (ActionException ae) {
				listArray[5] = "false";
			}

			// adding write permission
			try {
				_checkUserPermissions(c, user, PERMISSION_WRITE);
				listArray[6] = "true";
			} catch (ActionException ae) {
				listArray[6] = "false";
			}
		}
		else {
			// if the user is Campaign Manager Admin, has total permission
			listArray[5] = "true";
			listArray[6] = "true";
		}

		listArray[7] = String.valueOf(c.getParentCampaign());

		listArray[8] = "false";
		Communication comm = (Communication) InodeFactory.getChildOfClass(c, Communication.class);
		if (comm.getCommunicationType().equalsIgnoreCase("alert")) {
			listArray[8] = "true";
		}

		return listArray;
	}

	private boolean _deleteSelectedCampaigns(RenderRequest req, User user) {

		try {
    		String campaignsStr = req.getParameter("campaigns");
	    	
	    	if ((campaignsStr == null) || (campaignsStr.trim().equals("")))
	    		return true;
	    	
	    	StringTokenizer tokens = new StringTokenizer(campaignsStr, ",");
	    	String token;

	    	for (; tokens.hasMoreTokens();) {
	    		if (!((token = tokens.nextToken().trim()).equals(""))) {

	    			Campaign c = CampaignFactory.getCampaign(token);

	    			if(c.getUserId().equalsIgnoreCase(user.getUserId()) 
	    					|| permissionAPI.doesUserHavePermission(c,PERMISSION_WRITE,user)) {
	    				//Ending Quartz Job
	    				if ((c.getIsRecurrent() && !InodeUtils.isSet(c.getParentCampaign()))) {

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
	    			}
	    		}
	    	}
	    	
			SessionMessages.add(req, "message", "message.campaigns.deleted");

			return true;
    	} catch (Exception e) {
    		SessionMessages.add(req, "message", "message.campaigns.deleted.error");
    		return false;
    	}
    }

}