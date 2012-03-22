package com.dotmarketing.portlets.communications.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.net.URLDecoder;
import java.util.StringTokenizer;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.communications.factories.CommunicationsFactory;
import com.dotmarketing.portlets.communications.model.Communication;
import com.dotmarketing.portlets.communications.struts.CommunicationsForm;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.virtuallinks.factories.VirtualLinkFactory;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
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
 * Edit Communication objects
 * @author Oswaldo
 *
 */
public class EditCommunicationsAction extends DotPortletAction {
	
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
		
		Logger.debug(this, "EditCommunicationsAction cmd=" + cmd);
		
		HibernateUtil.startTransaction();
		
		User user = _getUser(req);
		
		try {
			_retrieveCommunication(req, res, config, form, user);
			
		} catch (ActionException ae) {
			_handleException(ae, req);
		}
		
		/*
		 * We are editing the Communication
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			try {
				_editCommunication(req, res, config, form, user);
				setForward(req,"portlet.ext.communications.edit_communication");
				
			} catch (ActionException ae) {
				_handleException(ae, req);
			}
		}
		
		/*
		 * If we are updating the Communication, copy the information
		 * from the struts bean to the hbm inode and run the
		 * update action and return to the list
		 */
		else if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {
				
				if (Validator.validate(req,form,mapping)) {
					_saveCommunication(req, res, config, form, user);
					_sendToReferral(req,res,referer);
				}
				
			} catch (ActionException ae) {
				_handleException(ae, req);
			}
			
		}
		/*
		 * If we are deleting the Communication,
		 * run the delete action and return to the list
		 *
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				_deleteCommunication(req, res, config, form, user);
			} catch (ActionException ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req,res,referer);
		}
		/*
		 * If we are deleting the Communications,
		 * run the delete action and return to the list
		 *
		 */
		else if ("deleteComms".equals(cmd)) {
        	try {
                if (_deleteSelectedCommunications(req)) {
            		SessionMessages.add(req, "message", "message.communications.deleted");
                } else {
                	SessionMessages.add(req, "message", "message.communications.not-deleted");
                }
        	} catch (Exception e) {
				_handleException(e,req);
			}
        	
        	_sendToReferral(req, res, referer);
        }

        HibernateUtil.commitTransaction();
		if(req.getAttribute(com.dotmarketing.util.WebKeys.COMMUNICATION_EDIT_FORM) == null){
			CommunicationsForm cform = (CommunicationsForm) form;
			if(!UtilMethods.isSet(cform.getModDate())){
				cform.setModDate(new java.util.Date());
				cform.setModifiedBy(user.getUserId());
			}
			req.setAttribute(com.dotmarketing.util.WebKeys.COMMUNICATION_EDIT_FORM, cform);
		}
		setForward(req,"portlet.ext.communications.edit_communication");
		
		
	}
	
	///// ************** ALL METHODS HERE *************************** ////////
	
	public void _retrieveCommunication(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {
		
		String inode = (req.getParameter("inode")!=null) ? req.getParameter("inode") : "";
		
		Communication c = null;
		c = CommunicationsFactory.getCommunication(inode);
		
		if(!InodeUtils.isSet(c.getInode())){
			c = CommunicationsFactory.newInstance();
			req.setAttribute(WebKeys.COMMUNICATION_EDIT_FORM_PERMISSION, true);
		}
		
		req.setAttribute(WebKeys.COMMUNICATION_EDIT, c);
		
	}
	
	public void _editCommunication(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {
		
		CommunicationsForm cfform = (CommunicationsForm) form;
		Communication c = (Communication) req.getAttribute(WebKeys.COMMUNICATION_EDIT);
		
		BeanUtils.copyProperties(cfform, c);
		
		if(cfform.getModDate() == null)
			cfform.setModDate(new java.util.Date());
			
		//add the html page to the Communication
		HTMLPage page = (HTMLPage) InodeFactory.getChildOfClass(c, HTMLPage.class);
		if (InodeUtils.isSet(page.getIdentifier())) {
			cfform.setHtmlPage(page.getIdentifier());
		}
		
		VirtualLink vl = (VirtualLink) InodeFactory.getChildOfClass(c, VirtualLink.class);
		if (InodeUtils.isSet(vl.getInode())) {
			cfform.setTrackBackLinkInode(vl.getInode());
		}
		req.setAttribute(WebKeys.COMMUNICATION_EDIT_FORM, cfform);
	}
	
	public void _saveCommunication(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {
		
		Communication c = (Communication) req.getAttribute(WebKeys.COMMUNICATION_EDIT);
		CommunicationsForm cfform = (CommunicationsForm) form;
		
		BeanUtils.copyProperties(req.getAttribute(WebKeys.COMMUNICATION_EDIT), cfform);
		
		if(cfform.getModDate() == null){
			c.setModDate(new java.util.Date());
		}
		
		c.setModifiedBy(user.getUserId());
		
		_checkUserPermissions(c, user, PERMISSION_WRITE);
		
		if (req.getParameter("typeContent").equals("HTMLPage") &&
				!cfform.getCommunicationType().equalsIgnoreCase("alert")) {
			c.setTextMessage("");
		}
		if (!cfform.getCommunicationType().equalsIgnoreCase("email")) {
			c.setFromEmail("");
			c.setFromName("");
			c.setEmailSubject("");
			c.setHtmlPage(null);
			cfform.setHtmlPage(null);
		}
		if (!cfform.getCommunicationType().equalsIgnoreCase("external")) {
			c.setTrackBackLinkInode(null);
			c.setExternalCommunicationIdentifier("");
			cfform.setTrackBackLinkInode(null);
		}

		try {

			// HibernateUtil.saveOrUpdate(c);
			HibernateUtil.saveOrUpdate(c);

		} catch (DotHibernateException dhe) {
			SessionMessages.add(req, "error", "error.communications.not-saved");
			//setForward(req,"portlet.ext.communications.edit_communication");
			throw new ActionException(dhe.getMessage());
		}

		
		// wipe the old HTML page entries
		HTMLPage page = (HTMLPage) InodeFactory.getChildOfClass(c, HTMLPage.class);
		if (InodeUtils.isSet(page.getInode()))
			c.deleteChild(page);
		
		if (req.getParameter("typeContent").equals("HTMLPage")) {
			//try to get the Communication's page
			if (InodeUtils.isSet(cfform.getHtmlPage())) {
				page = (HTMLPage) InodeFactory.getInode(String.valueOf(cfform.getHtmlPage()), HTMLPage.class);
				if (InodeUtils.isSet(page.getInode())) {
					c.addChild(page);
					c.setHtmlPage(page.getIdentifier());
				}
			}
		}
		
		// wipe the old VirtualLink entries
		VirtualLink vl = (VirtualLink) InodeFactory.getChildOfClass(c, VirtualLink.class);
		if (InodeUtils.isSet(vl.getInode()))
			c.deleteChild(vl);
		
		//try to get the Communication's virtual link
		if (InodeUtils.isSet(cfform.getTrackBackLinkInode())) {
			vl = VirtualLinkFactory.getVirtualLink(String.valueOf(cfform.getTrackBackLinkInode()));
			if (InodeUtils.isSet(vl.getInode())) {
				c.addChild(vl);
				c.setTrackBackLinkInode(vl.getInode());
			}
		}
				
		//add message
		SessionMessages.add(req, "message", "message.communications.saved");
		
	}
	
	
	public void _deleteCommunication(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {
		
		Communication c = (Communication) req.getAttribute(WebKeys.COMMUNICATION_EDIT);
		CommunicationsFactory.deleteCommunication(c, user.getUserId());
		SessionMessages.add(req, "message", "message.communications.deleted");
		
	}
	
    public boolean _deleteSelectedCommunications(ActionRequest req) {
    	try {
    		String communicationsStr = req.getParameter("comms");
	    	
	    	if ((communicationsStr == null) || (communicationsStr.trim().equals("")))
	    		return true;
	    	
	    	StringTokenizer tokens = new StringTokenizer(communicationsStr, ",");
	    	String token;
	    	
	    	for (; tokens.hasMoreTokens();) {
	    		if (!((token = tokens.nextToken().trim()).equals("")))
		    		CommunicationsFactory.deleteCommunication(token);
	    	}
	    	
	    	return true;
    	} catch (Exception e) {
    		return false;
    	}
    }
}
