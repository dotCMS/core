package com.dotmarketing.portlets.user.action;

import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.user.factories.UserCommentsFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Bob Johnson
 * @version $Revision: 1.5 $
 *
 */
public class ViewUserCommentsAction extends DotPortletAction {

	/* 
	 * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.portlet.PortletConfig, javax.portlet.RenderRequest, javax.portlet.RenderResponse)
	 */
	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

		String cmd = req.getParameter(Constants.CMD);
		
		try {
			//gets the user
			User user = _getUser(req);

			if(cmd!=null && cmd.equals(Constants.DELETE)) {
				String commentId = req.getParameter("commentId");
				UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
				UserCommentsFactory.deleteUserComment(userProxy.getInode(), commentId);
				//gets the session object for the messages
				SessionMessages.add(req, "message", "message.comment.delete");
			}
				
			_viewWebAssets(req, user);

			if (req.getWindowState().equals(WindowState.NORMAL)) {
				return mapping.findForward("portlet.ext.usercomments.view");
			}
			else {
				return mapping.findForward("portlet.ext.usercomments.view_user_comments");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
	
	//Needs to be implemented instead of using parent method because we use template to search for HTMLPages
	private void _viewWebAssets(RenderRequest req, User user) throws Exception {
		
		com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl)req;

		String userCommentId = user.getUserId();
		
		int pageNumber = 1;

		if (req.getParameter("pageNumber")!=null) {
			pageNumber = Integer.parseInt(req.getParameter("pageNumber")); 
		}
		int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
		int minIndex = (pageNumber - 1) * perPage;
		
		List l = new java.util.ArrayList();
		int numrows = 0;
		
		com.liferay.portal.model.User viewUser = null;
		try {
			viewUser = APILocator.getUserAPI().loadUserById(userCommentId,APILocator.getUserAPI().getSystemUser(),false);
		}catch(Exception e){
			Logger.error(ViewUserCommentsAction.class,e.getMessage());
			
		}
		req.setAttribute("viewUser",viewUser);
		
		try{
			numrows = UserCommentsFactory.countUserComments(userCommentId);
			
			l = UserCommentsFactory.getUserCommentsByUserId(userCommentId, minIndex, perPage);
			
		}	
		catch (Exception e) {
			Logger.error(ViewUserCommentsAction.class,e.getMessage());
			throw new ActionException (e.getMessage());

		}
		req.setAttribute("numrows",(new Integer(numrows)));
		req.setAttribute(WebKeys.USER_COMMENTS_VIEW, l);

	}
}
