package com.dotmarketing.portlets.form.action;

import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

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
		orderBy = (UtilMethods.isSet(orderBy) ? orderBy : "name");
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
		String resetQuery = req.getParameter("resetQuery");

		List<Structure> structures = new java.util.ArrayList<Structure>();

		try {
			String orderby = req.getParameter("orderBy");
			if (!UtilMethods.isSet(orderby)) {
				orderby = "upper(name)";
			}
			String direction = req.getParameter("direction");
			if (!UtilMethods.isSet(direction)) {
				direction = "asc";
			}

			int pageNumber = 1;

			if (UtilMethods.isSet(req.getParameter("pageNumber"))) {
				pageNumber = Integer.parseInt(req.getParameter("pageNumber"));
			}

			int limit = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");

			int offset = (pageNumber - 1) * limit;

			if ((query == null) && (resetQuery == null)) {
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
					queryCondition += "(lower(name) " + "like '%" + query.toLowerCase().replace("\'","\\\'") + "%' or inode='"+query+"')";
				}							


			} else {
				Logger.debug(this, "Getting all Forms Structures");
			}

			if(UtilMethods.isSet(queryCondition)) {
				queryCondition += " and structuretype="+Structure.STRUCTURE_TYPE_FORM; 
			}else{
				queryCondition += " structuretype="+Structure.STRUCTURE_TYPE_FORM;	
			}

			structures = StructureFactory.getStructuresByUser(user,queryCondition, orderby, limit, offset, direction);
			count = (int) ((PaginatedArrayList<Structure>)structures).getTotalResults();
			req.setAttribute(countWebKey, new Integer(count));
			req.setAttribute(viewWebKey, structures);
		} catch (Exception e) {
			req.setAttribute(viewWebKey, structures);
			Logger.error(this, "Exception e =" + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}

	}



}
