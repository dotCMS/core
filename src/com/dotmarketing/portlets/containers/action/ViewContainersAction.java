package com.dotmarketing.portlets.containers.action;

import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Maria Ahues
 * @version $Revision: 1.2 $
 * 
 */
public class ViewContainersAction extends DotPortletAction {

	/*
	 * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping,
	 *      org.apache.struts.action.ActionForm, javax.portlet.PortletConfig,
	 *      javax.portlet.RenderRequest, javax.portlet.RenderResponse)
	 */
	public ActionForward render(ActionMapping mapping, ActionForm form,
			PortletConfig config, RenderRequest req, RenderResponse res)
			throws Exception {

		Logger.debug(this, "Running ViewContainersAction!!!!");

		try {
			// gets the user
			User user = _getUser(req);

			com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl) req;
			HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
			// gets the session object for the messages
			HttpSession session = httpReq.getSession();
			String structureId = (String) session.getAttribute(WebKeys.SEARCH_STRUCTURE_ID);
			if (req.getParameter("structure_id") != null)
				structureId = req.getParameter("structure_id");
			if (structureId != null)
				session.setAttribute(WebKeys.SEARCH_STRUCTURE_ID, structureId);
			if (!InodeUtils.isSet(structureId)) {
				structureId = "";
			}

			//Checking if the user can add templates to a host
			PermissionAPI perAPI = APILocator.getPermissionAPI();
			List<Host> hosts = APILocator.getHostAPI().findAll(user, false);
			hosts.remove(APILocator.getHostAPI().findSystemHost(user, false));
			hosts = perAPI.filterCollection(hosts, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, false, user);
			if(hosts.size() == 0) {
				req.setAttribute(WebKeys.CONTAINER_CAN_ADD, false);
			} else {
				req.setAttribute(WebKeys.CONTAINER_CAN_ADD, true);
			}
			
			_viewWebAssets(req, user, Container.class, "container", WebKeys.CONTAINERS_VIEW_COUNT, WebKeys.CONTAINERS_VIEW, WebKeys.CONTAINER_QUERY, WebKeys.CONTAINER_SHOW_DELETED, WebKeys.CONTAINER_HOST_CHANGED, structureId);	
			return mapping.findForward("portlet.ext.containers.view_containers");
		} catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}

}