package com.dotmarketing.portlets.mailinglists.action;

import java.util.StringTokenizer;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import com.liferay.portlet.ActionRequestImpl;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.portlets.mailinglists.struts.MailingListForm;
import com.dotmarketing.portlets.userfilter.factories.UserFilterFactory;
import com.dotmarketing.portlets.userfilter.model.UserFilter;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.util.ParamUtil;
import com.liferay.util.servlet.SessionMessages;
/**
 * @author Maria
 */

public class EditMailingListAction extends PortletAction {

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {

        // Getting the http request
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
	
		String cmd = req.getParameter(Constants.CMD);

		//get the user
		User user = null;
		try {
			user = com.liferay.portal.util.PortalUtil.getUser(req);
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			setForward(req, Constants.COMMON_ERROR);
		}

		/*
		 *  get the mainglist object, stick it in request
		 *  
		 */
		try {
			_retrieveMailingList(req, res, config, form);
		}
		catch (Exception ae) {
			Logger.error(this, "Error retrieving the list", ae);
			req.setAttribute(PageContext.EXCEPTION, ae);
			setForward(req, Constants.COMMON_ERROR);
		}

		/*
		 * If this isn't our list and it isn't the Do Not send list
		 * 
		 */
		MailingList ml = (MailingList) req.getAttribute(WebKeys.MAILING_LIST_EDIT);
		boolean isMailingListAdmin = MailingListFactory.isMailingListAdmin(user);
		if (InodeUtils.isSet(ml.getInode())
			&& !ml.getUserId().equals(user.getUserId())
			&& !ml.getInode().equalsIgnoreCase(MailingListFactory.getUnsubscribersMailingList().getInode())
			&& !isMailingListAdmin) {
			
			SessionMessages.add(req, "message", "message.mailinglist.cannotEdit");
			setForward(req, "portlet.ext.mailinglists.view_mailinglists");
		}

		/*
		 *  if we are saving form 
		 *  
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {

				if (Validator.validate(req,form,mapping)) {
					_saveMailingList(req, res, config, form, user);
					if (UtilMethods.isSet(ParamUtil.getString(req, "redirect")))
						res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
				}
			}
			catch (Exception ae) {
				Logger.error(this, "Error Saving Maling List", ae);
				req.setAttribute(PageContext.EXCEPTION, ae);
				setForward(req, Constants.COMMON_ERROR);
			}
		} else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.DELETE_LIST)) {
			try {
				_deleteSelectedLists(req, res, config, form, user);
			}
			catch (Exception ae) {
		        Logger.error(this, ae.toString(), ae);
				req.setAttribute(PageContext.EXCEPTION, ae);
				setForward(req, Constants.COMMON_ERROR);
			}
			Logger.debug(this, "Returning to view mailing lists page");
			res.sendRedirect(SecurityUtils.stripReferer(httpReq, ParamUtil.getString(req, "redirect")));
		}

		/*
		 * Copy copy props from the db to the form bean 
		 * 
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			BeanUtils.copyProperties(form, req.getAttribute(WebKeys.MAILING_LIST_EDIT));
		}
		
		/*
		 * return to edit page
		 *  
		 */
		setForward(req, "portlet.ext.mailinglists.edit_mailinglist");
	}

	/**
	 * @param form
	 * @param config
	 * @param res
	 * @param req
	 * 
	 */
	private void _retrieveMailingList(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form)
	throws Exception {
        MailingList ml = (MailingList) InodeFactory.getInode(req.getParameter("inode"),MailingList.class);
        req.setAttribute(WebKeys.MAILING_LIST_EDIT, ml);
	}

	private void _saveMailingList(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {

		BeanUtils.copyProperties(req.getAttribute(WebKeys.MAILING_LIST_EDIT),form);

		MailingList ml = (MailingList) req.getAttribute(WebKeys.MAILING_LIST_EDIT);


		/*
		 * if this is  the users list and 
		 * not the "Do Not Send List"
		 * 
		 */
		
		if(!InodeUtils.isSet(ml.getInode()) || 
				ml.getUserId().equals(user.getUserId()) ||
				MailingListFactory.isMailingListAdmin(user)){
			ml.setUserId(user.getUserId());
			HibernateUtil.saveOrUpdate(ml);
			SessionMessages.add(req,"message", "message.mailinglist.save");
		}
		else{
			SessionMessages.add(req, "message", "message.mailinglist.cannotEdit");
		}
		HibernateUtil.closeSession();

		HibernateUtil.saveOrUpdate(ml);
		SessionMessages.add(req, "message", "message.mailinglist.save");

		//wipe the subscriber list form bean
		BeanUtils.copyProperties(form,req.getAttribute(WebKeys.MAILING_LIST_EDIT));
		((MailingListForm)form).setNewSubscribers("");
	}

	/**
	 * Delete one or more mailing lists
	 * 
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws DotHibernateException 
	 */
	private void _deleteSelectedLists(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws PortalException, SystemException, DotHibernateException {
		String mailingListIds = req.getParameter("mailinglists");
    	
    	if ((mailingListIds == null) || (mailingListIds.trim().equals(""))) {
    		return;
    	}
    	
    	StringTokenizer ids = new StringTokenizer(mailingListIds, ",");
    	String mailingListId;
    	MailingList mailingList;
    	UserFilter userFilter;
    	
    	while(ids.hasMoreTokens()) {
    		mailingListId = ids.nextToken();
    		mailingList = MailingListFactory.getMailingListsByInode(mailingListId);
    		
    		if (UtilMethods.isSet(mailingList.getInode())) {
	    		if (mailingList.getUserId().equals(user.getUserId()) || MailingListFactory.isMailingListAdmin(user)) {
					InodeFactory.deleteInode(mailingList);
					SessionMessages.add(req, "message", "message.mailinglist.delete");
				} else {
					SessionMessages.add(req, "message", "message.mailinglist.cannotEdit");
				}
    		} else {
    			userFilter = UserFilterFactory.getUserFilter(mailingListId);
    			if (userFilter.getOwner().equals(user.getUserId()) || MailingListFactory.isMailingListAdmin(user)) {
					InodeFactory.deleteInode(userFilter);
					SessionMessages.add(req, "message", "message.mailinglist.delete");
				} else {
					SessionMessages.add(req, "message", "message.mailinglist.cannotEdit");
				}
    		}
    	}
	}
}
