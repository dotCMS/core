package com.dotmarketing.portlets.browser.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;

import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;

import java.util.List;

import javax.servlet.jsp.PageContext;

/**
 *
 * @author  David Torres
 *
 */
public class ViewBrowserAction extends PortletAction {

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {
	    
		try {

			
			List<Language> languages = APILocator.getLanguageAPI().getLanguages();
			req.setAttribute(WebKeys.LANGUAGES, languages);
			
			return mapping.findForward("portlet.ext.browser.view_browser");
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}

}