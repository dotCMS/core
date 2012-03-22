package com.dotmarketing.portlets.userclicks.action;

import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.userclicks.factories.UserClickFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Maria Ahues
 * @version $Revision: 1.2 $
 *
 */
public class ViewUserClicksAction extends DotPortletAction {

	/* 
	 * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.portlet.PortletConfig, javax.portlet.RenderRequest, javax.portlet.RenderResponse)
	 */
	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

		Logger.debug(this, "Running ViewUserClicksAction!!!!");

		try {
			//gets the user
			User user = _getUser(req);

			if (req.getWindowState().equals(WindowState.NORMAL)) {
				return mapping.findForward("portlet.ext.userclicks.view");
			}
			else {
				if(req.getParameter("clickstreamId")==null){
					/** @see com.dotmarketing.portal.struts.DotPortletAction._viewWebAssets **/
					_viewWebAssets(req, user);		
					return mapping.findForward("portlet.ext.userclicks.view_user_clicks");

				}else{
					_detailWebAsset(req, user);		
					return mapping.findForward("portlet.ext.userclicks.detail_user_clicks");
					
				}
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
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		//gets the session object for the messages
		HttpSession session = httpReq.getSession();

		Logger.debug(this, "########## req.getParameter(\"user_click_id\") " + req.getParameter("user_click_id"));

		String userClickId = req.getParameter("user_click_id");
		
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
			viewUser = APILocator.getUserAPI().loadUserById(userClickId,APILocator.getUserAPI().getSystemUser(),false);
		}catch(Exception e){
	        Logger.warn(this, e.toString(), e);
		}
		req.setAttribute("viewUser",viewUser);
		
		Logger.debug(this, "Inside ViewUserClicksAction user_id=" + req.getParameter("user_click_id"));

		try{
			numrows = UserClickFactory.countUserClicks(userClickId);
			
			l = UserClickFactory.getUserClicks(userClickId, minIndex, perPage);
			
		}	
		catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new ActionException (e.getMessage());

		}
		req.setAttribute("numrows",(new Integer(numrows)));
		req.setAttribute(WebKeys.USER_CLICKS_VIEW, l);

		Logger.debug(this, "Done with ViewHTMLPageViewsAction");
		
	}

	private void _detailWebAsset(RenderRequest req, User user) throws Exception {
		
		com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl)req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		//gets the session object for the messages
		HttpSession session = httpReq.getSession();
		
		Logger.debug(this, "########## req.getParameter(\"user_click_id\") " + req.getParameter("user_click_id"));

		String clickstreamId = req.getParameter("clickstreamId");
		String userClickId = req.getParameter("user_click_id");
		
		
		com.liferay.portal.model.User viewUser = null;
		try {
			viewUser = APILocator.getUserAPI().loadUserById(userClickId,APILocator.getUserAPI().getSystemUser(),false);
		}catch(Exception e){
	        Logger.warn(this, e.toString(), e);
		}
		req.setAttribute("viewUser",viewUser);
		Clickstream clickstream = null;

		Logger.debug(this, "Inside _detail ViewUserClicksAction user_id=" + req.getParameter("user_click_id"));

		try{
			clickstream = (Clickstream) ClickstreamFactory.getClickstream(clickstreamId);
			
		}	
		catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new ActionException (e.getMessage());
		}
		req.setAttribute("clickstream",clickstream);

		Logger.debug(this, "Done with _detail ViewHTMLPageViewsAction");
		
	}

}
