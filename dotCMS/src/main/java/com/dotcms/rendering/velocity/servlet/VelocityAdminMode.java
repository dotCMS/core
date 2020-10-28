package com.dotcms.rendering.velocity.servlet;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * {@link VelocityModeHandler} to render a page into {@link com.dotmarketing.util.PageMode#ADMIN_MODE}
 */
public class VelocityAdminMode extends VelocityLiveMode {

    @Deprecated
    public VelocityAdminMode(final HttpServletRequest request, final HttpServletResponse response, final String uri, final Host host) {
        this(
                request,
                response,
                VelocityModeHandler.getHtmlPageFromURI(PageMode.get(request), request, uri, host),
                host
        );
    }

    protected VelocityAdminMode(final HttpServletRequest request, final HttpServletResponse response, final IHTMLPage htmlPage,
                             final Host host) {
        super(request, response, htmlPage, host);
    }

    @Override
    User getUser() {
        return APILocator.getLoginServiceAPI().getLoggedInUser();
    }
}
