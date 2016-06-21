package com.dotcms.spring.portlet;

import com.dotcms.repackage.javax.portlet.*;
import com.liferay.portal.util.CookieKeys;
import com.liferay.util.CookieUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author Jonathan Gamba
 *         5/18/16
 */
@Controller
@RequestMapping("/portlet")
public class PortletController implements Portlet {

    @RequestMapping(value = "/{portletId}", method = RequestMethod.GET)
    public String renderPortlet(@PathVariable String portletId, HttpServletRequest request) {

        //If the token does not exist or is not valid an exception will be thrown
        
        return "redirect:/html/ng/?id=" + portletId;
    }

    @Override
    public void init(PortletConfig portletConfig) throws PortletException {
        System.out.println("PortletController.init");
    }

    @Override
    public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) throws PortletException, IOException {
        System.out.println("PortletController.processAction");
    }

    @Override
    public void render(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException, IOException {
        System.out.println("PortletController.render");
    }

    @Override
    public void destroy() {
        System.out.println("PortletController.destroy");
    }
}
