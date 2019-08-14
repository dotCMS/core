package com.dotmarketing.portlets.structure.action;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
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
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Struts action that retrieves the required information to display the "Content
 * Types" portlet. Users can also order the data based on a specified criteria
 * and filter objects based on their names.
 * 
 * @author root
 * @version 1.1
 * @since Mar 22, 2012
 *
 */
public class ViewStructureAction extends DotPortletAction {

	private StructureAPI structureAPI;

	/**
	 * Default class constructor for Struts.
	 */
	public ViewStructureAction() {
        this(APILocator.getStructureAPI());
	}

	/**
	 * Class constructor for unit tests.
	 * 
	 * @param structureAPI
	 *            - The {@link StructureAPI} instance.
	 */
	@VisibleForTesting
	public ViewStructureAction(StructureAPI structureAPI) {
        this.structureAPI = structureAPI;
	}
	
	/**
	 * The main entry point that will handle all requests to the Struts action.
	 * 
	 * @param mapping
	 *            - Contains the mapping of a particular request to an instance
	 *            of a particular action class.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param req
	 *            - The HTTP Request wrapper.
	 * @param res
	 *            - The HTTP Response wrapper.
	 * @throws Exception
	 *             An error occurred when editing a field.
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
	 * Retrieves a list of {@link Structure} objects based on specific filtering
	 * parameters coming in the {@link RenderRequest} object. The resulting
	 * collection will be added as a request attribute that will be processed by
	 * the UI layer associated to this action.
	 * 
	 * @param req
	 *            - Portlet wrapper class for the HTTP request.
	 * @param user
	 *            - The {@link User} loading the portlet.
	 * @param countWebKey
	 *            - The parameter name indicating the
	 * @param viewWebKey
	 *            - The parameter name indicating the objects that will be
	 *            displayed on the page. In this case, Content Types.
	 * @param queryWebKey
	 *            - The parameter name indicating a specific search criterion
	 *            set by the user.
	 * @throws Exception
	 *             An error occurred when processing the request.
	 */
    protected void _loadStructures(RenderRequest req, User user, String countWebKey, String viewWebKey,
            String queryWebKey) throws Exception {
        com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
        // gets the session object for the messages
        HttpSession session = httpReq.getSession();

        Integer contentTypeBaseType =
                (Integer) session.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE);
        if (req.getParameter("structureType") != null)
            contentTypeBaseType = Integer.parseInt(req.getParameter("structureType"));
        if (contentTypeBaseType == null) {
            contentTypeBaseType = 0;
        } else {
            session.setAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE, contentTypeBaseType);
        }
        String query = req.getParameter("query");
        String resetQuery = req.getParameter("resetQuery");
        String showSystem = req.getParameter("system");
        List<Structure> contentTypes = new java.util.ArrayList<Structure>();
        APILocator.getPersonaAPI().createDefaultPersonaStructure();

        try {
            String orderby = req.getParameter("orderBy");
            String direction = req.getParameter("direction");
            if (!UtilMethods.isSet(orderby)) {
                orderby = "mod_date desc,upper(name)";
                direction = "asc";
            }
            if ("name".equals(orderby)) {
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
            if (((query != null) && (query.length() != 0)) || (contentTypeBaseType != null)) {

                if (query == null)
                    query = "";
                query = query.trim();
                if (UtilMethods.isSet(query)) {
                    queryCondition += "(lower(name) " + "like '%" + query.toLowerCase().replace("\'", "\\\'") + "%')";
                    if (UtilMethods.isLong(query)) {
                        queryCondition += " or inode=" + query + " ";
                    }
                }

                if (contentTypeBaseType != null && !"0".equals(contentTypeBaseType.toString())) {
                    if (!queryCondition.equals("")) {
                        queryCondition += " and structuretype = " + contentTypeBaseType + " ";
                    } else {
                        queryCondition += "structuretype = " + contentTypeBaseType + " ";
                    }
                }
                Logger.debug(this, "Getting Content Types based on condition = " + queryCondition);
            } else {
                Logger.debug(this, "Getting all Content Types");
            }

            if (UtilMethods.isSet(showSystem) && "1".equals(showSystem)) {
                if (!queryCondition.equals("")) {
                    queryCondition += " and system = " + DbConnectionFactory.getDBTrue() + " ";
                } else {
                    queryCondition += "system = " + DbConnectionFactory.getDBTrue() + " ";
                }
            }
            contentTypes = this.structureAPI.find(user, false, false, queryCondition, orderby, limit, offset, direction);

            if (contentTypes != null && !contentTypes.isEmpty()) {
            	count = APILocator.getContentTypeAPI(user).count(queryCondition);
            }

            req.setAttribute(countWebKey, new Integer(count));
            req.setAttribute(viewWebKey, contentTypes);

            Map<String, Long> entriesByContentTypes = APILocator.getContentTypeAPI(user, true).getEntriesByContentTypes();
            req.setAttribute(com.dotmarketing.util.WebKeys.Structure.ENTRIES_NUMBER, entriesByContentTypes);


            if (UtilMethods.isSet(req.getParameter("direction"))) {
                if ("asc".equals(req.getParameter("direction"))) {
                    req.setAttribute("direction", "desc");
                } else if ("desc".equals(req.getParameter("direction"))) {
                    req.setAttribute("direction", "asc");
                } else {
                    req.setAttribute("direction", "desc");
                }
            } else {
                req.setAttribute("direction", "desc");
            }
        } catch (Exception e) {
            req.setAttribute(viewWebKey, contentTypes);
			Logger.error(this,
					String.format("An error occurred when retrieving Content types: type=[%d], query=[%s] : ",
							contentTypeBaseType, query) + e.getMessage(), e);
            throw new Exception(e.getMessage(),e);
        }
    }

}
