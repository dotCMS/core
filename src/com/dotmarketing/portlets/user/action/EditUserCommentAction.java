package com.dotmarketing.portlets.user.action;

import java.util.Date;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.user.factories.UserCommentsFactory;
import com.dotmarketing.portlets.user.model.UserComment;
import com.dotmarketing.portlets.user.struts.UserCommentsForm;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author Maria Ahues 
 */

public class EditUserCommentAction extends DotPortletAction {
	
	public static boolean debug = false;
	
	/*public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req,
			RenderResponse res) throws Exception {
		
		ActionForward myfoward = mapping.findForward("portlet.admin.list_users");
		return myfoward;
	}*/
	
	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
	throws Exception 
	{
		ActionRequestImpl reqImpl = (ActionRequestImpl)req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		UserCommentsForm userCommentForm = (UserCommentsForm) form;
		
		String referer = req.getParameter("referer");
		
		String cmd = req.getParameter(Constants.CMD);		
		User user = _getUser(req);
		
		
		new HibernateUtil().startTransaction();
		try
		{
			if(cmd.equals(Constants.DELETE))
			{
				_deleteWebAsset(req,res,config,userCommentForm,user);
			}
			else if (cmd.equals(Constants.SAVE))
			{
				_saveWebAsset(req,res,config,userCommentForm,user);
			}
			new HibernateUtil().commitTransaction();
		}
		catch(Exception ex)
		{
			Logger.warn(this,ex.toString());
			new HibernateUtil().rollbackTransaction();
		}
		referer += "&layer=comments";
		_sendToReferral(req,res,referer);
		SessionMessages.add(req,"comments");
	}
	
	public void _deleteWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) throws Exception 
	{
		try
		{
			UserCommentsForm userCommentForm = (UserCommentsForm) form;
			String userCommentInodeString = req.getParameter("commentId");
			//long userCommentInode = Long.parseLong(userCommentInodeString);
			UserComment userComment = UserCommentsFactory.getComment(userCommentInodeString);
			String userProxyInode = userCommentForm.getUserProxy();
			UserCommentsFactory.deleteUserComment(userProxyInode,userComment);
		}
		catch(Exception ex)
		{
			Logger.debug(this,ex.toString());
		}
	}
	
	public void _saveWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) throws Exception 
	{
		UserCommentsForm userCommentForm = (UserCommentsForm) form;
		UserComment userComment = new UserComment();
		BeanUtils.copyProperties(userComment,userCommentForm);
		
		//Copy additional fields
		userComment.setCommentUserId(user.getUserId());
		
		Date now = new Date();
		userComment.setDate(now);
		
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userCommentForm.getUserProxy(),APILocator.getUserAPI().getSystemUser(), false);
		userComment.setUserId(userProxy.getUserId());
		
		userComment.setMethod(userCommentForm.getMethod());
		userComment.setTypeComment(userCommentForm.getTypeComment());
		userComment.setSubject(userCommentForm.getSubject());
		userComment.setComment(userCommentForm.getComment());

		UserCommentsFactory.saveUserComment(userProxy.getInode(),userComment);
		
	}
}