package com.dotcms.spring.portlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.Portlet;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletException;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotmarketing.util.Logger;

/**
 * @author Jonathan Gamba
 *         5/18/16
 */
@Controller
@RequestMapping("/portlet")
public class PortletController implements Portlet {

	@RequestMapping(value = "/{portletId}", method = RequestMethod.GET)
	public String renderPortlet(@PathVariable String portletId, HttpServletRequest request) {

		//TODO include user validation
		
		Logger.debug(PortletController.class, "PortletController.renderPortlet");
		return "redirect:/html/ng/?id=" + portletId;
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
