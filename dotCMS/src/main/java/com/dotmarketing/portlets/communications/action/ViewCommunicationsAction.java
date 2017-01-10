package com.dotmarketing.portlets.communications.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import javax.servlet.jsp.PageContext;

import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.communications.factories.CommunicationsFactory;
import com.dotmarketing.portlets.communications.model.Communication;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;

/**
 * List the communications objects
 * @author Oswaldo
 *
 */
public class ViewCommunicationsAction extends DotPortletAction {
	
	/* 
	 * @see com.liferay.portal.struts.PortletAction#render(com.dotcms.repackage.org.apache.struts.action.ActionMapping, com.dotcms.repackage.org.apache.struts.action.ActionForm, com.dotcms.repackage.javax.portlet.PortletConfig, com.dotcms.repackage.javax.portlet.RenderRequest, com.dotcms.repackage.javax.portlet.RenderResponse)
	 */
	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
	throws Exception {
		
		try {
			//gets the user
			User user = _getUser(req);
			
			_viewCommunications(req, user);
			
			if (req.getWindowState().equals(WindowState.NORMAL)) {
				return mapping.findForward("portlet.ext.communications.view");
			}
			else {
				return mapping.findForward("portlet.ext.communications.view_communications");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void _viewCommunications(RenderRequest req, User user) throws PortalException, SystemException {
		
		//get their lists
		List<Communication> list = null;
		String orderby = req.getParameter("orderby");
		String query1 =req.getParameter("query1");
		String query2 =req.getParameter("query2");
		String query3 =req.getParameter("query3");
		
		if(UtilMethods.isSet(query1)){
			query1 = " lower(title)like '%"+query1.toLowerCase()+"%'";
		} else {
			query1 ="";
		}
		
		if(UtilMethods.isSet(query2)){
			query2 = " lower(from_name) like '%"+query2.toLowerCase()+"%'";
			
			if(UtilMethods.isSet(query1))
				query2 = " and "+query2;
		} else {
			query2 ="";
		}
	    if(UtilMethods.isSet(query3)&& query3.equalsIgnoreCase("All")){
			query3 = "";
		}
		if(UtilMethods.isSet(query3)){
			query3 = " lower(communication_type) like '%"+query3.toLowerCase()+"%'";
			
			if(UtilMethods.isSet(query1) || UtilMethods.isSet(query2))
				query3 = " and "+query3;
		} else {
			query3 ="";
		}
		
		String condition = query1 + query2 + query3;
		String direction = req.getParameter("direction");
		if(UtilMethods.isSet(direction))
			orderby = orderby+" "+direction;
		
		list = CommunicationsFactory.getCommunications(condition,orderby); 
		
		List<Communication> permitted = new ArrayList();
		for(Communication com : list){
			try {
				_checkUserPermissions(com, user, PERMISSION_READ);
				permitted.add(com);
			}catch(Exception e){
			}
		}
		
		req.setAttribute(WebKeys.COMMUNICATIONS_LIST, permitted);
	}
	
}
