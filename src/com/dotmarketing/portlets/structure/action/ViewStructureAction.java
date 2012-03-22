package com.dotmarketing.portlets.structure.action;

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

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class ViewStructureAction extends DotPortletAction {


	public ActionForward render(ActionMapping mapping, ActionForm form,
			PortletConfig config, RenderRequest req, RenderResponse res)
	throws Exception {

		String orderBy = req.getParameter("orderBy");
		User user = _getUser(req);
		orderBy = (UtilMethods.isSet(orderBy) ? orderBy : "name");
		_loadStructures(req, user, WebKeys.STRUCTURES_VIEW_COUNT, WebKeys.Structure.STRUCTURES, WebKeys.STRUCTURE_QUERY);
		if (req.getWindowState().equals(WindowState.NORMAL)) {
			return mapping.findForward("portlet.ext.structure.view");
		} else {			
			return mapping.findForward("portlet.ext.structure.view_structure");

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
		if (structureType == null){
			structureType=1;
		}
		else{
			session.setAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE, structureType);
		}
		String query = req.getParameter("query");
		String resetQuery = req.getParameter("resetQuery");
		String showSystem = req.getParameter("system");

		List<Structure> structures = new java.util.ArrayList<Structure>();
		String tableName = "structure";

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
					queryCondition += "(lower(name) " + "like '%" + query.toLowerCase().replace("\'","\\\'") + "%')";
					if(UtilMethods.isLong(query)){
						queryCondition += " or inode="+query+" ";
					}
				}
				
				
				if(structureType != null && !structureType.toString().equals("0")){
					if(!queryCondition.equals("")){
						queryCondition += " and structuretype = "+structureType+" "; 
					}else{
						queryCondition += "structuretype = "+structureType+" ";  
					}
				}
				Logger.debug(this, "Getting Structures based on condition = " + queryCondition );


			} else {
				Logger.debug(this, "Getting all Structures");
			}
			
			if(UtilMethods.isSet(showSystem) && showSystem.equals("1")){
				
				if(!queryCondition.equals("")){
					queryCondition += " and system = "+DbConnectionFactory.getDBTrue()+" "; 
				}else{
					queryCondition += "system = "+DbConnectionFactory.getDBTrue()+" ";  
				}

			}
			/*
			if(!queryCondition.equals("")){
				queryCondition += " and structuretype not in("+Structure.STRUCTURE_TYPE_FORM+")"; 
			}else{
				queryCondition += "structuretype not in("+Structure.STRUCTURE_TYPE_FORM+") ";  
			}
			*/
			structures = StructureFactory.getStructures(queryCondition, orderby, limit, offset, direction);
			count = StructureFactory.getStructuresCount(queryCondition);
			req.setAttribute(countWebKey, new Integer(count));
			req.setAttribute(viewWebKey, structures);
		} catch (Exception e) {
			req.setAttribute(viewWebKey, structures);
			Logger.error(this, "Exception e =" + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}

	}
}
