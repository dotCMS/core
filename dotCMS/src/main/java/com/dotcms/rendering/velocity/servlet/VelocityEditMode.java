package com.dotcms.rendering.velocity.servlet;

import com.dotcms.rendering.velocity.services.PageContextBuilder;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;

import java.io.File;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.liferay.portal.model.User;

public class VelocityEditMode implements VelocityModeHandler {


    private static HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private final PageMode mode = PageMode.EDIT_MODE;
    private String CHARSET = Config.getStringProperty("CHARSET", "UTF-8");
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


    public void serve(Writer out) throws Exception {

        // Getting the user to check the permissions
        User user = com.liferay.portal.util.PortalUtil.getUser(request);


        // Getting the identifier from the uri
        Identifier id = APILocator.getIdentifierAPI().find(host, uri);


        // creates the context where to place the variables
        response.setContentType(CHARSET);

        Context context = VelocityUtil.getWebContext(request, response);
        long langId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
        IHTMLPage htmlPage = VelocityUtil.getPage(id, langId, mode.showLive);
        PageContextBuilder builder = new PageContextBuilder(htmlPage, user, PageMode.EDIT_MODE);
        builder.addAll(context);



        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, mode, host, context));

        request.setAttribute("velocityContext", context);


        VelocityUtil.getEngine()
            .getTemplate(mode.name() + File.separator + htmlPage.getIdentifier() + "_" + htmlPage.getLanguageId() + "."
                    + VelocityType.HTMLPAGE.fileExtension)
            .merge(context, out);


    }

    @Override
    public void serve() throws Exception {
        serve(response.getWriter());


    }



}
