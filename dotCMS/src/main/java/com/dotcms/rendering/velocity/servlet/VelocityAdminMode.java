package com.dotcms.rendering.velocity.servlet;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Velocity renderer for PageMode#ADMIN_MODE
 */
public class VelocityAdminMode extends VelocityLiveMode {

    public VelocityAdminMode(HttpServletRequest request, HttpServletResponse response, String uri, Host host) {
        super(request, response, uri, host);
    }

    User getUser() {
        return APILocator.getLoginServiceAPI().getLoggedInUser();
    }
}
