package com.dotmarketing.portlets.campaigns.action;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.campaigns.factories.CampaignFactory;
import com.dotmarketing.portlets.campaigns.factories.RecipientFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.campaigns.model.Click;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionResponseImpl;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Maria Ahues
 * @version $Revision: 1.5 $
 *
 */
public class ViewReportAction extends DotPortletAction {

	/* 
	 * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.portlet.PortletConfig, javax.portlet.RenderRequest, javax.portlet.RenderResponse)
	 */
	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

		//wraps request to get session object
		ActionResponseImpl resImpl = (ActionResponseImpl)res;
		HttpServletResponse httpRes = resImpl.getHttpServletResponse();

		String cmd = (req.getParameter(Constants.CMD)!=null) ? req.getParameter(Constants.CMD) : Constants.VIEW; 

		Logger.debug(this, "ViewReportAction :: cmd=" + cmd);

		User user = _getUser(req);

		//get the campaign	
		try {

			Logger.debug(this, "I runned <sic> the RetrieveCampaignAction");
			_retrieveCampaign(req, res, form, user);

		}
		catch (Exception ae) {
        	_handleException(ae, req);
		}

		if (cmd.equals(Constants.VIEW)) {

			try {
				_viewCampaignReport(req,res,form, user);
			}
			catch (Exception ae) {
	        	_handleException(ae, req);
			}
			setForward(req,"portlet.ext.campaigns.view_report");

		}

		if (cmd.equals("view_all")) {

			try {
				_viewCampaignAllReport(req,res,form, user);
			}
			catch (Exception ae) {
	        	_handleException(ae, req);
			}
			if (req.getParameter("csv") == null) {
				setForward(req,"portlet.ext.campaigns.view_report_detailed");
			}
			else {
				_writeCSV(httpRes,req,res,form,user);
				return;
			}

		}

		if (cmd.equals("view_opened")) {

			try {
				_viewCampaignReportOpened(req,res,form, user);
			}
			catch (Exception ae) {
	        	_handleException(ae, req);
			}
			if (req.getParameter("csv") == null) {
				setForward(req,"portlet.ext.campaigns.view_report_detailed");
			}
			else {
				_writeCSV(httpRes,req,res,form,user);
				return;
			}

		}

		if (cmd.equals("view_unopened")) {

			try {
				_viewCampaignReportUnOpened(req,res,form, user);
			}
			catch (Exception ae) {
	        	_handleException(ae, req);
			}
			if (req.getParameter("csv") == null) {
				setForward(req,"portlet.ext.campaigns.view_report_detailed");
			}
			else {
				_writeCSV(httpRes,req,res,form,user);
				return;
			}
		}
		
		if (cmd.equals("view_errors")) {

			try {
				_viewCampaignReportErrors(req,res,form, user);
			}
			catch (Exception ae) {
	        	_handleException(ae, req);
			}
			if (req.getParameter("csv") == null) {
				setForward(req,"portlet.ext.campaigns.view_report_detailed");
			}
			else {
				_writeCSV(httpRes,req,res,form,user);
				return;
			}
		}
		
		if (cmd.equals("view_link")) {

			try {
				_viewCampaignReportLink(req,res,form, user);
			}
			catch (Exception ae) {
	        	_handleException(ae, req);
			}
			if (req.getParameter("csv") == null) {
				setForward(req,"portlet.ext.campaigns.view_report_detailed");
			}
			else {
				_writeCSV(httpRes,req,res,form,user);
				return;
			}
		}
		if (!UtilMethods.isSet(getForward(req))) {
			Logger.debug(this, "I'm the ViewReportAction and I got nothing");
			setForward(req,"portlet.ext.campaigns.view_report");
		}

	}
	private void _retrieveCampaign(ActionRequest req, ActionResponse res,ActionForm form,User user) throws Exception  {

		String inode = (req.getParameter("inode")!=null) ? req.getParameter("inode") : "";
		
		Campaign c = null;
		c = CampaignFactory.getCampaign(inode);
		
		if(!InodeUtils.isSet(c.getInode())){
			c = CampaignFactory.newInstance();	
			c.setUserId(user.getUserId());
		}
        req.setAttribute(WebKeys.CAMPAIGN_EDIT, c);
		
	}

	private void _viewCampaignReportLink(ActionRequest req, ActionResponse res,ActionForm form,User user) throws Exception  {
		
		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);
		Click click = (Click) InodeFactory.getInode(req.getParameter("clickId"), Click.class);
		req.setAttribute(WebKeys.RECIPIENT_LIST, RecipientFactory.getRecipientsByCampaignAndClick(c, click));
		req.setAttribute(WebKeys.RECIPIENT_LIST_TITLE, "Users who clicked:" +click.getLink());
        req.setAttribute(WebKeys.CLICK_EDIT, click);
		
	}

	private void _viewCampaignReportUnOpened(ActionRequest req, ActionResponse res,ActionForm form,User user) throws Exception  {
		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);
		req.setAttribute(WebKeys.RECIPIENT_LIST, RecipientFactory.getUnopenedRecipientsByCampaign(c));
		req.setAttribute(WebKeys.RECIPIENT_LIST_TITLE, "Unopened");
	}
	private void _viewCampaignReportOpened(ActionRequest req, ActionResponse res,ActionForm form,User user) throws Exception  {
		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);
		req.setAttribute(WebKeys.RECIPIENT_LIST, RecipientFactory.getOpenedRecipientsByCampaign(c));
		req.setAttribute(WebKeys.RECIPIENT_LIST_TITLE, "Opened");
	}
	private void _viewCampaignReportErrors(ActionRequest req, ActionResponse res,ActionForm form,User user) throws Exception  {
		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);
		req.setAttribute(WebKeys.RECIPIENT_LIST, RecipientFactory.getRecipientsWithErrorsByCampaign(c));
		req.setAttribute(WebKeys.RECIPIENT_LIST_TITLE, "Errors/Bounces");
	}
	private void _viewCampaignAllReport(ActionRequest req, ActionResponse res,ActionForm form,User user) throws Exception  {
		Campaign c = (Campaign) req.getAttribute(WebKeys.CAMPAIGN_EDIT);
		req.setAttribute(WebKeys.RECIPIENT_LIST, RecipientFactory.getAllRecipientsByCampaign(c));
		req.setAttribute(WebKeys.RECIPIENT_LIST_TITLE, "All");
	}
	private void _viewCampaignReport(ActionRequest req, ActionResponse res,ActionForm form,User user) throws Exception {
	}

	private void _writeCSV(HttpServletResponse httpRes,ActionRequest req, ActionResponse res,ActionForm form,User user) throws Exception {

		ServletOutputStream out = httpRes.getOutputStream();
	
		boolean isCampaignManagerViewer = false;
		String campaignManagerViewerRoleKey = "";
		try {
			Role campaignManagerViewerRole = APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CAMPAIGN_MANAGER_VIEWER"));
			campaignManagerViewerRoleKey = campaignManagerViewerRole.getRoleKey();
		}
		catch (Exception e) {}

		try {
			Role[] userRoles = (Role[])APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
			for (int i = 0; i < userRoles.length; i++) {
				Role userRole = (Role) userRoles[i];
				if (userRole.getRoleKey().equals(campaignManagerViewerRoleKey)) {
					isCampaignManagerViewer = true;
				}
			}
		}
		catch (Exception e) {
			
		}
		
		httpRes.setContentType("application/octet-stream");
		httpRes.setHeader("Content-Disposition", "attachment; filename=\"report" + System.currentTimeMillis() +".csv\"");

		//httpRes.setContentType("text/csv");
		//httpRes.setHeader("Content-Disposition", "attachment; filename=\"report" + System.currentTimeMillis() +".csv\"");
	 
		//print the header
		com.dotmarketing.portlets.campaigns.model.Campaign camp = (com.dotmarketing.portlets.campaigns.model.Campaign)  req.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_EDIT);
		java.util.List allClicks =  com.dotmarketing.portlets.campaigns.factories.ClickFactory.getClicksByParentOrderByCount(camp);
		java.util.List recipients = (java.util.List) req.getAttribute(com.dotmarketing.util.WebKeys.RECIPIENT_LIST);

//		if (!isCampaignManagerViewer) {
			out.print("Name, eMail, Sent Date, Opened Date");
//		}
//		else {
//			out.print("Name, Sent Date, Opened Date");
//		}
		
		java.util.Iterator i = allClicks.iterator();
		int x = 1;
		while(i.hasNext()){
		 	com.dotmarketing.portlets.campaigns.model.Click c = (com.dotmarketing.portlets.campaigns.model.Click) i.next();
			out.print(", Clicks on : " + c.getLink());
		}
		out.print("\r\n");

		java.util.Iterator iter =  recipients.iterator();

		while(iter.hasNext()){

		 	com.dotmarketing.portlets.campaigns.model.Recipient r = (com.dotmarketing.portlets.campaigns.model.Recipient) iter.next();
			String name = r.getName();
			if(UtilMethods.isSet(r.getLastname())){
				name = name+" "+r.getLastname();
			}
		 	out.print("\"" + com.dotmarketing.util.UtilMethods.webifyString(name) +"\",");
//			if (!isCampaignManagerViewer) {
				out.print("\"" + com.dotmarketing.util.UtilMethods.webifyString(r.getEmail()) +"\",");
//			}
			out.print("\"" + com.dotmarketing.util.UtilMethods.webifyString(com.dotmarketing.util.UtilMethods.dateToHTMLDate(r.getSent()))+"\",");
			out.print("\"" + com.dotmarketing.util.UtilMethods.webifyString(com.dotmarketing.util.UtilMethods.dateToHTMLDate(r.getOpened()))+"\",");
			java.util.List clicks = com.dotmarketing.factories.InodeFactory.getChildrenClass(r, com.dotmarketing.portlets.campaigns.model.Click.class);
			i = allClicks.iterator();
			while(i.hasNext()){
				int cCount = 0;
			 	com.dotmarketing.portlets.campaigns.model.Click c = (com.dotmarketing.portlets.campaigns.model.Click) i.next();
				java.util.Iterator i2 =  clicks.iterator();
				boolean printClick = false;
				while(i2.hasNext()){
				 	com.dotmarketing.portlets.campaigns.model.Click c2 = (com.dotmarketing.portlets.campaigns.model.Click) i2.next();
					if(c.getLink() != null && c2.getLink() != null && c.getLink().equals(c2.getLink())){
						cCount = c.getClickCount();
					}
				}
				out.print("\"" + cCount +"\",");
		
			}
			out.print("\r\n");
		}
		out.flush();
		out.close();
		
	}

}