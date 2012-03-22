/*
 * Created on Sep 23, 2004
 *
 */
package com.dotmarketing.portlets.languagesmanager.action;

import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;

/**
 * @author alex
 *
 */
public class ViewLanguageAction extends PortletAction {

	private LanguageAPI langAPI = APILocator.getLanguageAPI();

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

        Logger.debug(this, "Running ViewLanguageAction!!!!");
        Logger.debug(this, "req.getContextPath()"+req.getContextPath());
		
		try {
			if (req.getWindowState().equals(WindowState.NORMAL)) {
				//get their lists
				List list = langAPI.getLanguages();
				req.setAttribute(WebKeys.LANGUAGE_MANAGER_LIST, list);
				
		        Logger.debug(this, "Going to: portlet.ext.languagesmanager.view");
				return mapping.findForward("portlet.ext.languagesmanager.view");
				
			}
			else {
				//get their lists
				/*List list = new ArrayList();
				if (InodeUtils.isSet(req.getParameter("inode"))) {
					HTMLPage htmlPage = (HTMLPage) InodeFactory.getInode(req.getParameter("inode"),HTMLPage.class);
					Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);
					list = VirtualLinkFactory.getVirtualLinks(identifier.getURI());
				}
				else {
					list = VirtualLinkFactory.getVirtualLinks();
				}

				req.setAttribute(WebKeys.VIRTUAL_LINK_LIST_VIEW, list);*/
				List list = langAPI.getLanguages();
				req.setAttribute(WebKeys.LANGUAGE_MANAGER_LIST, list);
		        Logger.debug(this, "Going to: portlet.ext.languagesmanager.view_languages");
				return mapping.findForward("portlet.ext.languagesmanager.view_languages");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
}
