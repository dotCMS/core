package com.dotcms.rendering.velocity.servlet;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * {@link VelocityModeHandler} to render a page into {@link com.dotmarketing.util.PageMode#ADMIN_MODE}
 */
public class VelocityAdminMode extends VelocityLiveMode {

    public VelocityAdminMode(final HttpServletRequest request, final HttpServletResponse response, final String uri,
                             final Host host, final String personaTagToIncludeContent) {
        super(request, response, uri, host, personaTagToIncludeContent);
    }

    @Override
    User getUser() {
        return APILocator.getLoginServiceAPI().getLoggedInUser();
    }
}
