package com.dotcms.rendering.velocity.servlet;

import com.dotcms.rendering.velocity.services.PageContextBuilder;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.repackage.javax.portlet.WindowState;

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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.liferay.portal.model.User;

public class VelocityEditMode extends VelocityModeHandler {

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    private static final PageMode mode = PageMode.EDIT_MODE;
    protected final String uri;
    private final Host host;
    private final User user;
    private final static String REORDER_MENU_URL="/c/portal/layout?p_l_id={0}&p_p_id=site-browser&p_p_action=1&p_p_state=maximized&_site_browser_struts_action=%2Fext%2Fdirector%2Fdirect";
    
    public VelocityEditMode(HttpServletRequest request, HttpServletResponse response, String uri, Host host) {
        this.request = request;
        this.response = response;
        this.uri = uri;
        this.host = host;
        this.user = WebAPILocator.getUserWebAPI().getUser(request);
    }

    public VelocityEditMode(HttpServletRequest request, HttpServletResponse response) {
        this(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }


    public void serve(final OutputStream out) throws DotDataException, IOException, DotSecurityException {

        // Getting the user to check the permissions
       


        // Getting the identifier from the uri
        Identifier id = APILocator.getIdentifierAPI().find(host, uri);


        // creates the context where to place the variables
        response.setContentType(CHARSET);

        Context context = VelocityUtil.getWebContext(request, response);
        context.put("directorURL", getReorderMenuUrl());
        

        long langId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
        IHTMLPage htmlPage = VelocityUtil.getPage(id, langId, mode.showLive);
        new PageContextBuilder(htmlPage, user, PageMode.EDIT_MODE).addAll(context);

        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, mode, host, context));


        try(final Writer outStr = new BufferedWriter(new OutputStreamWriter(out))){
            this.getTemplate(htmlPage, mode).merge(context, outStr);
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
