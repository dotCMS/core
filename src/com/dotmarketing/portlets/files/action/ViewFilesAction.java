package com.dotmarketing.portlets.files.action;

import com.dotcms.repackage.portlet.javax.portlet.PortletConfig;
import com.dotcms.repackage.portlet.javax.portlet.RenderRequest;
import com.dotcms.repackage.portlet.javax.portlet.RenderResponse;
import javax.servlet.jsp.PageContext;

import com.dotcms.repackage.struts.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Maria Ahues
 * @version $Revision: 1.2 $
 *
 */
public class ViewFilesAction extends DotPortletAction {

	/*
	 * @see com.liferay.portal.struts.PortletAction#render(com.dotcms.repackage.struts.org.apache.struts.action.ActionMapping, com.dotcms.repackage.struts.org.apache.struts.action.ActionForm, com.dotcms.repackage.portlet.javax.portlet.PortletConfig, com.dotcms.repackage.portlet.javax.portlet.RenderRequest, com.dotcms.repackage.portlet.javax.portlet.RenderResponse)
	 */
	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

        Logger.debug(this, "Running ViewFilesAction!!!!");

		try {
			//gets the user
			User user = _getUser(req);

			_viewWebAssets(req, user, File.class, "file_asset", WebKeys.FILES_VIEW_COUNT, WebKeys.FILES_VIEW, WebKeys.FILE_QUERY, WebKeys.FILE_SHOW_DELETED, WebKeys.FILE_HOST_CHANGED);
			return mapping.findForward("portlet.ext.files.view_files");
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
}