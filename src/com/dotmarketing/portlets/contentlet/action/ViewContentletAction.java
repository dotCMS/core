package com.dotmarketing.portlets.contentlet.action;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.RenderRequestImpl;

/**
 * Struts action that retrieves the required information to display the
 * "Content Search" portlet.
 * 
 * @author root
 * @version 1.1
 * @since Mar 22, 2012
 *
 */
public class ViewContentletAction extends DotPortletAction {

	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	private StructureAPI structureAPI = APILocator.getStructureAPI();

	/**
	 * 
	 */
	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req,
			RenderResponse res) throws Exception {

		Logger.debug(ViewContentletAction.class, "Running ViewContentletsAction!!!!");

		try {
			// gets the user
			User user = _getUser(req);
			_viewContentlets(req, user);
			return mapping.findForward("portlet.ext.contentlet.view_contentlets");

		} catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}

	/**
	 * Retrieves a list of {@link Structure} objects based on specific filtering
	 * parameters coming in the {@link RenderRequest} object. The resulting
	 * collection will be added as a request attribute that will be processed by
	 * the JSP associated to this action.
	 * 
	 * @param req
	 *            - Portlet wrapper class for the HTTP request.
	 * @param user
	 *            - The {@link User} loading the portlet.
	 * @throws Exception
	 *             An error occurred when processing this request.
	 */
	@SuppressWarnings("unchecked")
	protected void _viewContentlets(RenderRequest req, User user) throws Exception {
		
		//GIT-2816
		RenderRequestImpl reqImpl = (RenderRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		HttpSession ses = httpReq.getSession();
		ContentletAPI conAPI = APILocator.getContentletAPI();

		List<String> tempBinaryImageInodes = (List<String>) ses.getAttribute(Contentlet.TEMP_BINARY_IMAGE_INODES_LIST);		
		if(UtilMethods.isSet(tempBinaryImageInodes) && tempBinaryImageInodes.size() > 0){
			for(String inode : tempBinaryImageInodes){
				conAPI.delete(conAPI.find(inode, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false, true);
			}
			tempBinaryImageInodes.clear();
		}
		
		if (req.getParameter("popup") != null)
		{
			if (req.getParameter("container_inode") != null)
			{
				Container cont = (Container) InodeFactory.getInode(req.getParameter("container_inode"), Container.class);

				List<Structure> structures = APILocator.getContainerAPI().getStructuresInContainer(cont);
				req.setAttribute(WebKeys.Structure.STRUCTURES, structures);
			}
			else if (req.getParameter("structure_id") != null)
			{
				Structure st = (Structure) InodeFactory.getInode(req.getParameter("structure_id"), Structure.class);
				req.setAttribute(WebKeys.Structure.STRUCTURE, st);
			}
		}
		else
		{
			if(req.getParameter("structure_id") != null){
				Structure st = (Structure) InodeFactory.getInode(req.getParameter("structure_id"), Structure.class);
				if(st.getStructureType()==Structure.STRUCTURE_TYPE_FORM){
					List<Structure> structures =StructureFactory.getStructuresByUser(user,"structuretype="+st.getStructureType(), "upper(name)", 0, 0, "asc");
					req.setAttribute(WebKeys.Structure.STRUCTURES, structures);
					req.setAttribute("SHOW_FORMS_ONLY", true);
				}else{
					List<Structure> structures = this.structureAPI.find(user, false, true);
					req.setAttribute(WebKeys.Structure.STRUCTURES, structures);
				}

			}else{
				List<Structure> structures = this.structureAPI.find(user, false, true);
				req.setAttribute(WebKeys.Structure.STRUCTURES, structures);
			}
		}

		if(req.getParameter("selected_lang") != null){
			((RenderRequestImpl)req).getHttpServletRequest().getSession().setAttribute(WebKeys.LANGUAGE_SEARCHED, req.getParameter("selected_lang"));
		}
		List<Language> languages = langAPI.getLanguages();
		req.setAttribute(WebKeys.LANGUAGES, languages);

	}

}
