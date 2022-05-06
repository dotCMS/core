package com.dotcms.spring.portlet;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.Portlet;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletException;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletURLUtil;


public class PortletController implements Portlet {


	public String renderPortlet(String portletId, HttpServletRequest request) {

		//TODO include user validation
		
		Logger.debug(PortletController.class, "PortletController.renderPortlet");
		return "redirect:/" + PortletURLUtil.URL_ADMIN_PREFIX + "/?id=" + portletId;
	}

	@Override
	public void init(PortletConfig portletConfig) throws PortletException {
		Logger.debug(PortletController.class, "PortletController.init");
	}

	@Override
	public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) throws PortletException, IOException {
		Logger.debug(PortletController.class, "PortletController.processAction");
	}

	@Override
	public void render(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException, IOException {
		Logger.debug(PortletController.class, "PortletController.render");
	}

	@Override
	public void destroy() {
		Logger.debug(PortletController.class, "PortletController.destroy");
	}
}
