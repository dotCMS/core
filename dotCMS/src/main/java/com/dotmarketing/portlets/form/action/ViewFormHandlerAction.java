package com.dotmarketing.portlets.form.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Get the existing forms structures
 * @author Oswaldo
 *
 */
public class ViewFormHandlerAction extends DotPortletAction {


	public ActionForward render(ActionMapping mapping, ActionForm form,
			PortletConfig config, RenderRequest req, RenderResponse res)
	throws Exception {

		String orderBy = req.getParameter("orderBy");
		User user = _getUser(req);
		orderBy = (UtilMethods.isSet(orderBy) ? orderBy : "mod_date desc");
		_loadStructures(req, user, WebKeys.STRUCTURES_VIEW_COUNT, WebKeys.Structure.STRUCTURES, WebKeys.STRUCTURE_QUERY);
		if (req.getWindowState().equals(WindowState.NORMAL)) {
			return mapping.findForward("portlet.ext.formhandler.view");
		} else {			
			return mapping.findForward("portlet.ext.formhandler.view_form");

		}
	}


	protected void _loadStructures(RenderRequest req, User user, String countWebKey, String viewWebKey,
			String queryWebKey) throws Exception {

		com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		// gets the session object for the messages
		HttpSession session = httpReq.getSession();

		Integer structureType = (Integer) session.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE);
		if (req.getParameter("structureType") != null)
			structureType = Integer.parseInt(req.getParameter("structureType"));
		if (structureType != null)
			session.setAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE, structureType);
		String query = req.getParameter("query");
		if(UtilMethods.isSet(query)){
    		Pattern p = Pattern.compile("^[a-zA-Z0-9]*");
    		Matcher m = p.matcher(query);
    		if(m.find()){
    		    query=m.group();
    		}else{
    		    query=null;
    		}
		}
		boolean resetQuery = req.getParameter("resetQuery")!=null;
		
		List<Structure> structures = new java.util.ArrayList<Structure>();

		try {
			String orderby = req.getParameter("orderBy");
			if (!UtilMethods.isSet(orderby)) {
				orderby = "mod_date";
			}
			else{
			    orderby=SQLUtil.sanitizeSortBy(orderby);
			}
			String direction = req.getParameter("direction");
			if (!UtilMethods.isSet(direction) || "desc".equalsIgnoreCase(direction)) {
				direction = "desc";
			}else{
			    direction = "asc";
			}

			int pageNumber = 1;

			if (UtilMethods.isSet(req.getParameter("pageNumber"))) {
				pageNumber = Integer.parseInt(req.getParameter("pageNumber"));
			}

			int limit = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");

			int offset = (pageNumber - 1) * limit;

			if ((query == null) && (!resetQuery)) {
				query = (String) session.getAttribute(queryWebKey);
			}
			session.setAttribute(queryWebKey, query);

			int count = 0;
			String queryCondition ="";

			if (((query != null) && (query.length() != 0)) || (structureType != null)) {

				if (query == null)
					query = "";
				query = query.trim();
				if (UtilMethods.isSet(query)) {
					queryCondition += "(lower(name) " + "like '%" + query.toLowerCase().replace("\'","\\\'") + "%' or inode.inode='"+query+"')";
				}							


			} else {
				Logger.debug(this, "Getting all Forms Structures");
			}

			if(UtilMethods.isSet(queryCondition)) {
				queryCondition += " and structuretype="+Structure.STRUCTURE_TYPE_FORM; 
			}else{
				queryCondition += " structuretype="+Structure.STRUCTURE_TYPE_FORM;	
			}
            structures = APILocator.getStructureAPI().find(user, false, false, queryCondition, orderby, limit, offset, direction);

			count = APILocator.getContentTypeAPI(user).count(queryCondition);
			req.setAttribute(countWebKey, new Integer(count));
			req.setAttribute(viewWebKey, structures);
		} catch (Exception e) {
			req.setAttribute(viewWebKey, structures);
			Logger.error(this, "Exception e =" + e.getMessage(), e);
			throw new Exception(e.getMessage(),e);
		}

	}



}
