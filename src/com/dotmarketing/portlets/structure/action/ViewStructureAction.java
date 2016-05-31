package com.dotmarketing.portlets.structure.action;

import java.util.List;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 * Struts action that retrieves the required information to display the
 * "Content Types" portlet.
 * 
 * @author root
 * @version 1.1
 * @since Mar 22, 2012
 *
 */
public class ViewStructureAction extends DotPortletAction {

	private StructureAPI structureAPI = APILocator.getStructureAPI();

	/**
	 * 
	 */
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

    /**
     * Retrieves a list of {@link Structure} objects based on specific filtering parameters coming in the
     * {@link RenderRequest} object. The resulting collection will be added as a request attribute that will be
     * processed by the JSP associated to this action.
     * 
     * @param req - Portlet wrapper class for the HTTP request.
     * @param user - The {@link User} loading the portlet.
     * @param countWebKey -
     * @param viewWebKey -
     * @param queryWebKey -
     * @throws Exception An error occurred when processing the request.
     */
    protected void _loadStructures(RenderRequest req, User user, String countWebKey, String viewWebKey,
            String queryWebKey) throws Exception {
        com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
        // gets the session object for the messages
        HttpSession session = httpReq.getSession();

        Integer structureType =
                (Integer) session.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE);
        if (req.getParameter("structureType") != null)
            structureType = Integer.parseInt(req.getParameter("structureType"));
        if (structureType == null) {
            structureType = 0;
        } else {
            session.setAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE, structureType);
        }
        String query = req.getParameter("query");
        String resetQuery = req.getParameter("resetQuery");
        String showSystem = req.getParameter("system");

        List<Structure> structures = new java.util.ArrayList<Structure>();
        APILocator.getPersonaAPI().createDefaultPersonaStructure();

        try {
            String orderby = req.getParameter("orderBy");
            String direction = req.getParameter("direction");
            if (!UtilMethods.isSet(orderby)) {
                orderby = "mod_date desc,upper(name)";
                direction = "asc";
            }
            if (orderby.equals("name")) {
                orderby = "upper(name)";
            }

            if (!UtilMethods.isSet(direction)) {
                direction = "asc";
            }

            int pageNumber = 1;

            if (UtilMethods.isSet(req.getParameter("pageNumber"))) {
                pageNumber = Integer.parseInt(req.getParameter("pageNumber"));
            }

            int limit = com.dotmarketing.util.Config.getIntProperty("PER_PAGE", 40);

            int offset = (pageNumber - 1) * limit;

            if ((query == null) && (resetQuery == null)) {
                query = (String) session.getAttribute(queryWebKey);
            }
            session.setAttribute(queryWebKey, query);

            int count = 0;
            String queryCondition = "";

            if (((query != null) && (query.length() != 0)) || (structureType != null)) {

                if (query == null)
                    query = "";
                query = query.trim();
                if (UtilMethods.isSet(query)) {
                    queryCondition += "(lower(name) " + "like '%" + query.toLowerCase().replace("\'", "\\\'") + "%')";
                    if (UtilMethods.isLong(query)) {
                        queryCondition += " or inode=" + query + " ";
                    }
                }

                if (structureType != null && !structureType.toString().equals("0")) {
                    if (!queryCondition.equals("")) {
                        queryCondition += " and structuretype = " + structureType + " ";
                    } else {
                        queryCondition += "structuretype = " + structureType + " ";
                    }
                }
                Logger.debug(this, "Getting Structures based on condition = " + queryCondition);
            } else {
                Logger.debug(this, "Getting all Structures");
            }

            if (UtilMethods.isSet(showSystem) && showSystem.equals("1")) {

                if (!queryCondition.equals("")) {
                    queryCondition += " and system = " + DbConnectionFactory.getDBTrue() + " ";
                } else {
                    queryCondition += "system = " + DbConnectionFactory.getDBTrue() + " ";
                }

            }
            structures = this.structureAPI.find(user, false, false, queryCondition, orderby, limit, offset, direction);

            if (structures != null && !structures.isEmpty()) {
                count = this.structureAPI.countStructures(queryCondition);
            }

            req.setAttribute(countWebKey, new Integer(count));
            req.setAttribute(viewWebKey, structures);
            if (UtilMethods.isSet(req.getParameter("direction"))) {
                if (req.getParameter("direction").equals("asc"))
                    req.setAttribute("direction", "desc");
                else if (req.getParameter("direction").equals("desc"))
                    req.setAttribute("direction", "asc");
                else
                    req.setAttribute("direction", "desc");
            } else {
                req.setAttribute("direction", "desc");
            }
        } catch (Exception e) {
            req.setAttribute(viewWebKey, structures);
            Logger.error(this, "Exception e =" + e.getMessage(), e);
            throw new Exception(e.getMessage());
        }
    }

}
