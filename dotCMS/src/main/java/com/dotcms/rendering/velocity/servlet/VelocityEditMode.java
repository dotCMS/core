package com.dotcms.rendering.velocity.servlet;

import com.dotcms.rendering.velocity.services.PageContextBuilder;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.PageMode;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.liferay.portal.model.User;

public class VelocityEditMode extends VelocityModeHandler {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private static final PageMode mode = PageMode.EDIT_MODE;
    private final String uri;
    private final Host host;

    public VelocityEditMode(HttpServletRequest request, HttpServletResponse response, String uri, Host host) {
        this.request = request;
        this.response = response;
        this.uri = uri;
        this.host = host;
    }

    public VelocityEditMode(HttpServletRequest request, HttpServletResponse response) {
        this(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }


    public void serve(Writer out) throws DotDataException, IOException, DotSecurityException {

        // Getting the user to check the permissions
        User user = WebAPILocator.getUserWebAPI().getUser(request);


        // Getting the identifier from the uri
        Identifier id = APILocator.getIdentifierAPI().find(host, uri);


        // creates the context where to place the variables
        response.setContentType(CHARSET);

        Context context = VelocityUtil.getWebContext(request, response);
        long langId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
        IHTMLPage htmlPage = VelocityUtil.getPage(id, langId, mode.showLive);
        new PageContextBuilder(htmlPage, user, PageMode.PREVIEW_MODE).addAll(context);

        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, mode, host, context));



        this.getTemplate(htmlPage, mode).merge(context, out);


    }

    @Override
    public void serve() throws DotDataException, IOException, DotSecurityException {
        serve(response.getWriter());


    }



}
