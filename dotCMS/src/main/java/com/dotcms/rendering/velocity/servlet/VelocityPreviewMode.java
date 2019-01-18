package com.dotcms.rendering.velocity.servlet;

import com.dotcms.rendering.velocity.events.PreviewEditParseErrorException;
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
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class VelocityPreviewMode extends VelocityModeHandler {



    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private static final PageMode mode = PageMode.PREVIEW_MODE;
    private final String uri;
    private final Host host;

    public VelocityPreviewMode(HttpServletRequest request, HttpServletResponse response, String uri, Host host) {
        this.request = request;
        this.response = response;
        this.uri = uri;
        this.host = host;
    }

    public VelocityPreviewMode(HttpServletRequest request, HttpServletResponse response) {
        this(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }



    @Override
    public void serve() throws DotDataException, IOException, DotSecurityException {

        serve(response.getOutputStream());

    }

    @Override
    public void serve(final OutputStream out) throws DotDataException, IOException, DotSecurityException {


        // Getting the user to check the permissions
        User user = WebAPILocator.getUserWebAPI().getUser(request);

        // Getting the identifier from the uri
        Identifier id = APILocator.getIdentifierAPI().find(host, uri);


        // creates the context where to place the variables
        response.setContentType(CHARSET);
        Context context = VelocityUtil.getWebContext(request, response);

        long langId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
        IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(id, langId, mode.showLive,user, mode.respectAnonPerms);
        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, mode, host, context));


        new PageContextBuilder(htmlPage, user, PageMode.PREVIEW_MODE).addAll(context);
        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, mode, host, context));

        request.setAttribute("velocityContext", context);
        try(final Writer outStr = new BufferedWriter(new OutputStreamWriter(out))){
            this.getTemplate(htmlPage, mode).merge(context, outStr);
        } catch (PreviewEditParseErrorException e) {
            this.processException(user, htmlPage.getName(), e);
        }
    }

}
