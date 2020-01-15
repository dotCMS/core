package com.dotcms.rendering.velocity.servlet;

import com.dotcms.rendering.velocity.events.PreviewEditParseErrorException;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class VelocityEditMode extends VelocityModeHandler {

    private final User user;
    private final static String REORDER_MENU_URL="/c/portal/layout?p_l_id={0}&p_p_id=site-browser&p_p_action=1&p_p_state=maximized&_site_browser_struts_action=%2Fext%2Ffolders%2Forder_menu";

    public VelocityEditMode(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final IHTMLPage htmlPage,
            final Host host) {

        super(request, response, htmlPage, host);
        this.setMode(PageMode.EDIT_MODE);
        this.user = WebAPILocator.getUserWebAPI().getUser(request);
    }

    public void serve(final OutputStream out) throws DotDataException, IOException, DotSecurityException {

       // creates the context where to place the variables
        response.setContentType(CHARSET);

        Context context = VelocityUtil.getWebContext(request, response);
        context.put("directorURL", getReorderMenuUrl());

        new PageRenderUtil(htmlPage, user, PageMode.EDIT_MODE).addAll(context);

        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, mode, host, context));


        try(final Writer outStr = new BufferedWriter(new OutputStreamWriter(out))){
            this.getTemplate(htmlPage, mode).merge(context, outStr);
        } catch (PreviewEditParseErrorException e) {
            this.processException(user, htmlPage.getName(), e);
        }
    }

    @Override
    public void serve() throws DotDataException, IOException, DotSecurityException {
        serve(response.getOutputStream());
    }

    
    
    private String getReorderMenuUrl() throws DotDataException {
        if(APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("site-browser", user)){
            Layout lay = APILocator.getLayoutAPI().loadLayoutsForUser(user).get(0);
            return REORDER_MENU_URL.replace("{0}", lay.getId());
        }
        return null;
        
        
    }


}
